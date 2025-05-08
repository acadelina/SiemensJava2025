package com.siemens.internship;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ItemController.class)
public class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testCreateItem() throws Exception {
        Item item = new Item(1L, "Item", "desc", "NEW", "email@test.com");

        when(itemService.save(any(Item.class))).thenReturn(item);
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name",is("Item")));

        //invalid item
        item=new Item(null, "Item", "desc", "NEW", "emaicom");
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateItem() throws Exception {
        Item existing = new Item(1L, "Item", "desc", "NEW", "email@test.com");
        Item update = new Item(null, "new", "desc", "PROCESSED", "new@email.com");

        //for item that exists
        when(itemService.findById(1L)).thenReturn(Optional.of(existing));
        when(itemService.save(any())).thenReturn(update);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new"));

        //for item that doesn't exist
        when(itemService.findById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Item())))
                .andExpect(status().isNotFound());
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
