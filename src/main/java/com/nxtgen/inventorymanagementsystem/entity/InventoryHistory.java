package com.nxtgen.inventorymanagementsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "inventory_history")
public class InventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "action")
    private String action;

    @Column(name = "quantity_change")
    private Integer quantityChange;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "notes")
    private String notes;

    @Lob
    @Column(name = "receipt_image", columnDefinition = "LONGTEXT")
    private String receiptImageData;

    /* 🔥 FIXED DATE */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventoryHistory() {}

    public InventoryHistory(Long itemId, String itemName, String action, Integer quantityChange) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.action = action;
        this.quantityChange = quantityChange;
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getReceiptImageData() { return receiptImageData; }
    public void setReceiptImageData(String receiptImageData) { this.receiptImageData = receiptImageData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}