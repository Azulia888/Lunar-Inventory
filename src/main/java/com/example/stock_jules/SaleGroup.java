package com.example.stock_jules;

class SaleGroup {
    public int itemId;
    public String itemName;
    public Integer categoryId;
    public double soldPrice;
    public int quantity;
    public double total;

    public SaleGroup(int itemId, String itemName, Integer categoryId, double soldPrice, int quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.soldPrice = soldPrice;
        this.quantity = quantity;
        this.total = soldPrice * quantity;
    }
}