package com.nxtgen.inventorymanagementsystem.service;

import com.nxtgen.inventorymanagementsystem.entity.InventoryHistory;
import com.nxtgen.inventorymanagementsystem.entity.Item;
import com.nxtgen.inventorymanagementsystem.repository.InventoryHistoryRepository;
import com.nxtgen.inventorymanagementsystem.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final InventoryHistoryRepository historyRepository;

    public ItemService(ItemRepository itemRepository, InventoryHistoryRepository historyRepository) {
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item saveItem(Item item) {
        Item saved = itemRepository.save(item);

        historyRepository.save(
                new InventoryHistory(
                        saved.getId(),
                        saved.getName(),
                        "ADD",
                        saved.getQuantity()
                )
        );
        return saved;
    }

    public Item updateItem(Long id, Item updated) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        int oldQty = existing.getQuantity();
        int newQty = updated.getQuantity();
        int change = newQty - oldQty;

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setLocation(updated.getLocation());
        existing.setQuantity(newQty);

        Item saved = itemRepository.save(existing);

        historyRepository.save(
                new InventoryHistory(
                        saved.getId(),
                        saved.getName(),
                        "UPDATE",
                        change
                )
        );

        return saved;
    }

    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        historyRepository.save(
                new InventoryHistory(
                        item.getId(),
                        item.getName(),
                        "DELETE",
                        -item.getQuantity()
                )
        );

        itemRepository.deleteById(id);
    }
}