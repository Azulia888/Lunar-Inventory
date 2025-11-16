package com.example.stock_jules;

class Sale {
    public int id;
    public double soldPrice;
    public String saleTime;
    public int exportId;
    public int itemId;
    public String itemName;

    public Sale(int id, double soldPrice, String saleTime, int exportId, int itemId, String itemName) {
        this.id = id;
        this.soldPrice = soldPrice;
        this.saleTime = saleTime;
        this.exportId = exportId;
        this.itemId = itemId;
        this.itemName = itemName;
    }
}