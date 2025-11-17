package com.example.lunar_inventory;

class Item {
    public int id;
    public String name;
    public String picture;
    public double basePrice;
    public int currentStock;
    public int totalSold;
    public boolean usesCategoryPrice;
    public boolean shown;
    public Integer categoryId;

    public Item(int id, String name, String picture, double basePrice, int currentStock,
                int totalSold, boolean usesCategoryPrice, boolean shown, Integer categoryId) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.basePrice = basePrice;
        this.currentStock = currentStock;
        this.totalSold = totalSold;
        this.usesCategoryPrice = usesCategoryPrice;
        this.shown = shown;
        this.categoryId = categoryId;
    }
}