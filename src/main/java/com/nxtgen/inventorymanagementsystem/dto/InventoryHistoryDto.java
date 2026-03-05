package com.nxtgen.inventorymanagementsystem.dto;

import java.time.LocalDateTime;

public class InventoryHistoryDto {
    private Long id;
    private Long itemId;
    private String itemName;
    private String action;
    private int quantityChange;
    private LocalDateTime createdAt;

    public InventoryHistoryDto() {}

    public InventoryHistoryDto(Long id, Long itemId, String itemName, String action, int quantityChange, LocalDateTime createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.action = action;
        this.quantityChange = quantityChange;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getAction() { return action; }
    public int getQuantityChange() { return quantityChange; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setAction(String action) { this.action = action; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}