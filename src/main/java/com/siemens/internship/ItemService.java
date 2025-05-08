package com.siemens.internship;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Validated
@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final Logger logger = LogManager.getLogger(ItemService.class);


    public List<Item> findAll() {
        try{
             return itemRepository.findAll();
        } catch(DataAccessException e){
            logger.error("Error finding all items: ",e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Item> findById(Long id) {
        try{
            return itemRepository.findById(id);
        } catch(DataAccessException e){
            logger.error("Error finding item by id: ",e);
            throw new RuntimeException(e);
        }
    }

    public Item save(Item item) {
        try{
            return itemRepository.save(item);
        } catch(DataAccessException e){
            logger.error("Error saving item: ",e);
            throw new RuntimeException(e);
        }
    }

    public void deleteById(Long id) {
        try{
            itemRepository.deleteById(id);
        } catch(DataAccessException e){
            logger.error("Error deleting item: ",e);
            throw new RuntimeException(e);
        }
    }


    /**
    The original version
     *used CompletableFuture.runAsync() without collecting the futures, it did not wait for any async task to finish
     *used shared mutable processedItems, processedCount across threads without synchronization
     *did not propagate or log errors correctly
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        //all item ids for the processing
        List<Long> itemIds = itemRepository.findAllIds();

        //create a list of ComputableFutures, one for each item's async processing
        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id->CompletableFuture.supplyAsync(()->{
                    try{
                        //simulate some delay
                        Thread.sleep(100);
                        //attempt to fetch item by id from db
                        return itemRepository.findById(id).orElse(null);
                    }catch(InterruptedException e){
                        //restore the interrupted status and propagate the error
                        Thread.currentThread().interrupt();
                        logger.error(e);
                        throw new CompletionException(e);
                    }

                },executor).thenApply(item->{
                    //process the item if it was found in the db
                    if(item!=null){
                        try {
                            //set item status to indicate the processing
                            item.setStatus("PROCESSED");
                            //save the update in db
                            return itemRepository.save(item);
                        }
                        catch (Exception e) {
                            //log any failure during save
                            logger.error("Error saving item: ",e);
                            //return null to skip the item
                           return null;
                        }
                    }
                    //skip null items
                    return null;
                })).toList();

        //combine all futures into one that completes when all tasks are done
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        //after all futures complete join result without null items
        return allDone.thenApply(f->futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

    }

}

