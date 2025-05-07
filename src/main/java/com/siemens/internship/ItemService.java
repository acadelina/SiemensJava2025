package com.siemens.internship;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final Logger logger = LogManager.getLogger(ItemService.class);



    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id->CompletableFuture.supplyAsync(()->{
                    try{
                        Thread.sleep(100);
                        return itemRepository.findById(id).orElse(null);
                    }catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                        logger.error(e);
                        throw new CompletionException(e);
                    }

                },executor).thenApply(item->{
                    if(item!=null){
                        try {
                            item.setStatus("PROCESSED");
                            return itemRepository.save(item);
                        }
                        catch (Exception e) {
                            logger.error("Error saving item: ",e);
                           return null;
                        }
                    }
                    return null;
                })).toList();

        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allDone.thenApply(f->futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

    }

}

