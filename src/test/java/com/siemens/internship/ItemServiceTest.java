package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemServiceTest {
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindAll() {
        List<Item> items=List.of(new Item(1L,"name","desc","NEW","test@gmail.com"));
        when(itemRepository.findAll()).thenReturn(items);
        List<Item> result=itemService.findAll();
        assertEquals(1,result.size());
        assertEquals(items.get(0),result.get(0));

        items=List.of();
        when(itemRepository.findAll()).thenReturn(items);
        result=itemService.findAll();
        assertEquals(0,result.size());

        //in case of exception
        when(itemRepository.findAll()).thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () -> itemService.findAll());
    }

    @Test
    public void testFindById() {
        Item item=new Item(1L,"name","desc","NEW","test@gmail.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        //item found
        Optional<Item> result=itemService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(item,result.get());

        //item not found
        result=itemService.findById(2L);
        assertFalse(result.isPresent());

        when(itemRepository.findById(anyLong())).thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () -> itemService.findById(1L));
    }

    @Test
    public void testSave() {
        Item item=new Item(1L,"name","desc","NEW","test@gmail.com");
        Item item1=new Item(null,"name","desc","NEW","test@gmail.com");
        Item item2=new Item(null,null,null,null,null);
        Item item3=new Item(2L,"name","desc","NEW","test@gmail.com");

        when(itemRepository.save(item1)).thenReturn(item);
        Item savedItem=itemService.save(item1);
        assertNotNull(savedItem.getId());

        when(itemRepository.save(item2)).thenReturn(item2);
        savedItem=itemService.save(item2);
        assertNull(savedItem.getId());
        assertNull(savedItem.getName());

        when(itemRepository.save(item3)).thenReturn(item3);
        savedItem=itemService.save(item3);
        assertEquals(item3,savedItem);

        when(itemRepository.save(item)).thenThrow(new RuntimeException("DB error"));
        assertThrows(RuntimeException.class, () -> itemService.save(item));
    }


    @Test
    public void testDelete() {
        //test when id exists
        itemService.deleteById(1L);
        verify(itemRepository).deleteById(1L);

        //test when id doesn't exist
        doNothing().when(itemRepository).deleteById(999L);
        assertDoesNotThrow(() -> itemService.deleteById(999L));

        doThrow(new RuntimeException("DB error")).when(itemRepository).deleteById(1L);
        assertThrows(RuntimeException.class, () -> itemService.deleteById(1L));
    }

    @Test
    public void testProcessItemsAsync(){
        List<Long> ids = List.of(1L, 2L);
        Item item1 = new Item(1L, "Item1", "desc", "NEW", "test1@example.com");
        Item item2 = new Item(2L, "Item2", "desc", "NEW", "test2@example.com");
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        for (Long id : ids) {
            Item item = new Item(id, "Item " + id, "desc", "NEW", "email@test.com");
            when(itemRepository.findById(id)).thenReturn(Optional.of(item));
            when(itemRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        }

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result=future.join();
        assertEquals(2, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
        assertEquals("PROCESSED", result.get(1).getStatus());
        verify(itemRepository, times(2)).save(any(Item.class));

    }

    @Test
    void testProcessItemsAsyncWithMissingItem() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        Item item1 = new Item(1L, "Item1", "desc", "NEW", "a@a.com");

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Item> result = itemService.processItemsAsync().get();

        assertEquals(1, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
    }

    @Test
    void testProcessItemsAsyncSaveFailsForOneItem() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        Item item1 = new Item(1L, "Item1", "desc", "NEW", "a@a.com");
        Item item2 = new Item(2L, "Item2", "desc", "NEW", "b@b.com");

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(item1)).thenThrow(new RuntimeException("DB error"));
        when(itemRepository.save(item2)).thenReturn(item2);

        List<Item> result = itemService.processItemsAsync().get();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void testProcessItemsAsyncEmptyList() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(Collections.emptyList());

        List<Item> result = itemService.processItemsAsync().get();

        assertTrue(result.isEmpty());
        verify(itemRepository, never()).findById(any());
        verify(itemRepository, never()).save(any());
    }

}
