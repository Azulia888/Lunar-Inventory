package com.example.stock_jules;

// Category.java
class Category {
    public int id;
    public String name;
    public String picture;
    public Double defaultPrice;
    public boolean shown;
    public Integer parentCategory;

    public Category(int id, String name, String picture, Double defaultPrice,
                    boolean shown, Integer parentCategory) {
        this.id = id;
        this.name = name;
        this.picture = picture;
        this.defaultPrice = defaultPrice;
        this.shown = shown;
        this.parentCategory = parentCategory;
    }
}