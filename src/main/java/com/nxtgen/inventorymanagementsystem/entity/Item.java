package com.nxtgen.inventorymanagementsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "description", length = 500)
    private String description;

    private String category;
    private String location;
    private int quantity;

    @Column(name = "delivery_receipt")
    private String deliveryReceipt;

    @Column(name = "hardware_revision")
    private String hardwareRevision;

    private String vendor;

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;

    @Column(name = "last_edited_by")
    private String lastEditedBy;

    public Item() {}

    public Item(String serialNumber, String description, String category, String location, int quantity) {
        this.serialNumber = serialNumber;
        this.description = description;
        this.category = category;
        this.location = location;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDeliveryReceipt() {
        return deliveryReceipt;
    }

    public void setDeliveryReceipt(String deliveryReceipt) {
        this.deliveryReceipt = deliveryReceipt;
    }

    public String getHardwareRevision() {
        return hardwareRevision;
    }

    public void setHardwareRevision(String hardwareRevision) {
        this.hardwareRevision = hardwareRevision;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }
}
