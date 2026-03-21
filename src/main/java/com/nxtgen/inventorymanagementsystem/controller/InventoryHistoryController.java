package com.nxtgen.inventorymanagementsystem.controller;

import com.nxtgen.inventorymanagementsystem.dto.InventoryHistoryDto;
import com.nxtgen.inventorymanagementsystem.entity.InventoryHistory;
import com.nxtgen.inventorymanagementsystem.repository.InventoryHistoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class InventoryHistoryController {

    private final InventoryHistoryRepository historyRepository;

    public InventoryHistoryController(InventoryHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    // 🔥 GET ALL HISTORY
    @GetMapping
    public List<InventoryHistoryDto> getAllHistory() {
        List<InventoryHistory> rows =
                historyRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        return rows.stream()
                .map(this::toDto)
                .toList();
    }

    // 🔥 GET HISTORY PER ITEM (VERY IMPORTANT FOR YOUR DROPDOWN)
    @GetMapping("/item/{itemId}")
    public List<InventoryHistoryDto> getItemHistory(@PathVariable Long itemId) {
        List<InventoryHistory> rows =
                historyRepository.findByItemIdOrderByCreatedAtDesc(itemId);

        return rows.stream()
                .map(this::toDto)
                .toList();
    }

    // 🔥 MAPPER (clean and reusable)
    private InventoryHistoryDto toDto(InventoryHistory h) {
        return new InventoryHistoryDto(
                h.getId(),
                h.getItemId(),
                h.getItemName(),
                h.getAction(),
                h.getQuantityChange(),
                h.getEditedBy(),          // NEW
                h.getNotes(),             // NEW
                h.getReceiptImageData(),  // NEW
                h.getCreatedAt()
        );
    }
}