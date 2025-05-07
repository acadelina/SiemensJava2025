package com.siemens.internship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ItemController.class)
public class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllItems() {
        List<Item> items=List.of(new Item(1L,"name","desc","NEW","test@gmail.com"));
        when(itemService.findAll()).thenReturn(items);

        var resp=itemController.getAllItems();
        assertEquals(1,resp.getBody().size());
        assertEquals(1L,resp.getBody().get(0).getId());
    }

    @Test
    void testGetItemById() {
        Item item = new Item(1L, "Item", "desc", "NEW", "email@test.com");

        //when id is found
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        var response = itemController.getItemById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(item, response.getBody());

        //when id is not found
        when(itemService.findById(2L)).thenReturn(Optional.empty());
        response = itemController.getItemById(2L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testCreateItem() {
        Item item = new Item(1L, "Item", "desc", "NEW", "email@test.com");
        when(itemService.save(item)).thenReturn(item);
        var response=itemController.createItem(item);
        assertEquals(HttpStatus.CREATED,response.getStatusCode());
        assertEquals(item,response.getBody());

    }

    @Test
    void testUpdateItem() {
        Item existing = new Item(1L, "Item", "desc", "NEW", "email@test.com");
        Item update = new Item(null, "new", "desc", "PROCESSED", "new@email.com");

        //for item that exists
        when(itemService.findById(1L)).thenReturn(Optional.of(existing));
        when(itemService.save(any())).thenReturn(update);

        ResponseEntity<Item> response = itemController.updateItem(1L, update);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new", response.getBody().getName());

        //for item that doesn't exist
        when(itemService.findById(5L)).thenReturn(Optional.empty());

        response = itemController.updateItem(5L, new Item());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteItem() {
        Item item = new Item(1L, "Item", "desc", "NEW", "email@test.com");

        //when item exists
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        var response = itemController.deleteItem(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        //when item doesn't exist
        when(itemService.findById(1L)).thenReturn(Optional.empty());
        response = itemController.deleteItem(1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testProcessItems() throws Exception {
        List<Item> items = List.of(
                new Item(1L, "Item1", "desc", "PROCESSED", "a@a.com"),
                new Item(2L, "Item2", "desc", "PROCESSED", "b@b.com")
        );
        when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(items));

        MvcResult result = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id",is(1)))
                .andExpect(jsonPath("$[1].id",is(2)));
    }
}
