package com.odilias.odms.model;

import java.math.BigDecimal;

public class InvoiceItem {
    public enum ItemType {
        SERVICE, MEDICATION, LAB_TEST
    }

    private int id;
    private int invoiceId;
    private ItemType itemType;
    private int itemReferenceId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public InvoiceItem() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public int getItemReferenceId() {
        return itemReferenceId;
    }

    public void setItemReferenceId(int itemReferenceId) {
        this.itemReferenceId = itemReferenceId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
