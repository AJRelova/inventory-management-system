package com.nxtgen.inventorymanagementsystem.service;

import com.nxtgen.inventorymanagementsystem.entity.InventoryHistory;
import com.nxtgen.inventorymanagementsystem.entity.Item;
import com.nxtgen.inventorymanagementsystem.repository.InventoryHistoryRepository;
import com.nxtgen.inventorymanagementsystem.repository.ItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        item.setLastEditedBy(currentUsername());
        Item saved = itemRepository.save(item);

        recordHistory(
                saved,
                "ADD",
                saved.getQuantity(),
                "Item created",
                saved.getImageData()
        );

        return saved;
    }

    public Item updateItem(Long id, Item updated) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        int oldQty = existing.getQuantity();
        int newQty = updated.getQuantity();
        int change = newQty - oldQty;

        boolean receiptChanged = updated.getImageData() != null
                && !updated.getImageData().isBlank()
                && !updated.getImageData().equals(existing.getImageData());

        existing.setSerialNumber(updated.getSerialNumber());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setLocation(updated.getLocation());
        existing.setQuantity(newQty);
        existing.setDeliveryReceipt(updated.getDeliveryReceipt());
        existing.setHardwareRevision(updated.getHardwareRevision());
        existing.setVendor(updated.getVendor());
        existing.setImageData(updated.getImageData());
        existing.setLastEditedBy(currentUsername());

        Item saved = itemRepository.save(existing);

        String notes;
        if (change > 0) {
            notes = "Quantity increased from " + oldQty + " to " + newQty;
        } else if (change < 0) {
            notes = "Quantity decreased from " + oldQty + " to " + newQty;
        } else {
            notes = "Item details updated";
        }

        recordHistory(
                saved,
                "UPDATE",
                change,
                notes,
                receiptChanged ? saved.getImageData() : null
        );

        if (receiptChanged) {
            recordHistory(
                    saved,
                    "RECEIPT_ADDED",
                    0,
                    "Receipt image uploaded/updated",
                    saved.getImageData()
            );
        }

        return saved;
    }

    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));

        recordHistory(
                item,
                "DELETE",
                -item.getQuantity(),
                "Item deleted",
                item.getImageData()
        );

        itemRepository.deleteById(id);
    }

    private void recordHistory(Item item, String action, Integer quantityChange, String notes, String receiptImageData) {
        InventoryHistory history = new InventoryHistory();

        history.setItemId(item.getId());
        history.setItemName(
                item.getDescription() != null && !item.getDescription().isBlank()
                        ? item.getDescription()
                        : item.getSerialNumber()
        );
        history.setAction(action);
        history.setQuantityChange(quantityChange);
        history.setEditedBy(currentUsername());
        history.setNotes(notes);
        history.setReceiptImageData(receiptImageData);

        historyRepository.save(history);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}