package com.example.stock_jules;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // Category methods
    public long addCategory(String name, String picture, Double defaultPrice, Integer parentCategory) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("picture", picture);
        if (defaultPrice != null) values.put("category_default_price", defaultPrice);
        if (parentCategory != null) values.put("parent_category", parentCategory);
        return db.insert("category", null, values);
    }

    public List<Category> getCategories(Integer parentCategory, boolean includeHidden) {
        List<Category> categories = new ArrayList<>();
        String selection = includeHidden ? "" : "shown = 1";
        if (parentCategory == null) {
            selection += (selection.isEmpty() ? "" : " AND ") + "(parent_category IS NULL OR parent_category = 0)";
        } else {
            selection += (selection.isEmpty() ? "" : " AND ") + "parent_category = " + parentCategory;
        }

        Cursor cursor = db.query("category", null, selection.isEmpty() ? null : selection,
                null, null, null, "name ASC");

        while (cursor.moveToNext()) {
            categories.add(new Category(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id_category")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("picture")),
                    cursor.isNull(cursor.getColumnIndexOrThrow("category_default_price")) ? null :
                            cursor.getDouble(cursor.getColumnIndexOrThrow("category_default_price")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("shown")) == 1,
                    cursor.isNull(cursor.getColumnIndexOrThrow("parent_category")) ? null :
                            cursor.getInt(cursor.getColumnIndexOrThrow("parent_category"))
            ));
        }
        cursor.close();
        return categories;
    }

    // Item methods
    public long addItem(String name, String picture, Double basePrice, Integer stock,
                        Integer categoryId, boolean usesCategoryPrice) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("picture", picture);

        Double finalPrice = basePrice;
        if (usesCategoryPrice && categoryId != null) {
            finalPrice = getCategoryPrice(categoryId);
        }
        values.put("base_price", finalPrice);
        values.put("current_stock", stock != null ? stock : -1);
        values.put("id_category", categoryId);
        values.put("uses_category_price", usesCategoryPrice ? 1 : 0);

        return db.insert("item", null, values);
    }

    public List<Item> getItems(Integer categoryId, boolean includeHidden) {
        List<Item> items = new ArrayList<>();
        String selection = includeHidden ? "" : "shown = 1";
        if (categoryId == null) {
            selection += (selection.isEmpty() ? "" : " AND ") + "(id_category IS NULL OR id_category = 0)";
        } else {
            selection += (selection.isEmpty() ? "" : " AND ") + "id_category = " + categoryId;
        }

        Cursor cursor = db.query("item", null, selection.isEmpty() ? null : selection,
                null, null, null, "name ASC");

        while (cursor.moveToNext()) {
            items.add(new Item(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id_item")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("picture")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("base_price")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("current_stock")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("total_sold")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("uses_category_price")) == 1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("shown")) == 1,
                    cursor.isNull(cursor.getColumnIndexOrThrow("id_category")) ? null :
                            cursor.getInt(cursor.getColumnIndexOrThrow("id_category"))
            ));
        }
        cursor.close();
        return items;
    }

    public Double getCategoryPrice(int categoryId) {
        Cursor cursor = db.rawQuery(
                "WITH RECURSIVE cat_path AS (" +
                        "  SELECT id_category, category_default_price, parent_category FROM category WHERE id_category = ?" +
                        "  UNION ALL" +
                        "  SELECT c.id_category, c.category_default_price, c.parent_category " +
                        "  FROM category c INNER JOIN cat_path cp ON c.id_category = cp.parent_category" +
                        ") SELECT category_default_price FROM cat_path WHERE category_default_price IS NOT NULL LIMIT 1",
                new String[]{String.valueOf(categoryId)});

        Double price = null;
        if (cursor.moveToFirst()) {
            price = cursor.getDouble(0);
        }
        cursor.close();
        return price != null ? price : 0.0;
    }

    // Sale methods
    public long addSale(int itemId, double soldPrice) {
        // Get current batch
        Cursor cursor = db.rawQuery("SELECT id_export FROM sale_batch ORDER BY id_export DESC LIMIT 1", null);
        int batchId = 1;
        if (cursor.moveToFirst()) {
            batchId = cursor.getInt(0);
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("sold_price", soldPrice);
        values.put("id_export", batchId);
        values.put("id_item", itemId);

        long result = db.insert("sale", null, values);

        if (result != -1) {
            // Update item stats
            db.execSQL("UPDATE item SET total_sold = total_sold + 1, " +
                    "current_stock = CASE WHEN current_stock > 0 THEN current_stock - 1 ELSE current_stock END " +
                    "WHERE id_item = ?", new Object[]{itemId});
        }

        return result;
    }

    public Item getItem(int itemId) {
        Cursor cursor = db.query("item", null, "id_item = ?",
                new String[]{String.valueOf(itemId)}, null, null, null);

        Item item = null;
        if (cursor.moveToFirst()) {
            item = new Item(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id_item")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("picture")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("base_price")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("current_stock")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("total_sold")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("uses_category_price")) == 1,
                    cursor.getInt(cursor.getColumnIndexOrThrow("shown")) == 1,
                    cursor.isNull(cursor.getColumnIndexOrThrow("id_category")) ? null :
                            cursor.getInt(cursor.getColumnIndexOrThrow("id_category"))
            );
        }
        cursor.close();
        return item;
    }
}