package com.example.lunar_inventory;

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

    public Category getCategory(int categoryId) {
        Cursor cursor = db.query("category", null, "id_category = ?",
                new String[]{String.valueOf(categoryId)}, null, null, null);

        Category category = null;
        if (cursor.moveToFirst()) {
            category = new Category(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id_category")),
                    cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("picture")),
                    cursor.isNull(cursor.getColumnIndexOrThrow("category_default_price")) ? null :
                            cursor.getDouble(cursor.getColumnIndexOrThrow("category_default_price")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("shown")) == 1,
                    cursor.isNull(cursor.getColumnIndexOrThrow("parent_category")) ? null :
                            cursor.getInt(cursor.getColumnIndexOrThrow("parent_category"))
            );
        }
        cursor.close();
        return category;
    }

    public boolean updateCategory(int categoryId, String name, String picture, Double defaultPrice, Integer parentCategory) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("picture", picture);
        if (defaultPrice != null) {
            values.put("category_default_price", defaultPrice);
        } else {
            values.putNull("category_default_price");
        }
        if (parentCategory != null) {
            values.put("parent_category", parentCategory);
        } else {
            values.putNull("parent_category");
        }

        int rows = db.update("category", values, "id_category = ?", new String[]{String.valueOf(categoryId)});
        return rows > 0;
    }

    public boolean deleteCategory(int categoryId) {
        ContentValues values = new ContentValues();
        values.put("shown", 0);

        hideItemsInCategory(categoryId);
        hideSubCategories(categoryId);

        int rows = db.update("category", values, "id_category = ?", new String[]{String.valueOf(categoryId)});
        return rows > 0;
    }

    private void hideItemsInCategory(int categoryId) {
        ContentValues values = new ContentValues();
        values.put("shown", 0);
        db.update("item", values, "id_category = ?", new String[]{String.valueOf(categoryId)});
    }

    private void hideSubCategories(int categoryId) {
        List<Category> subCategories = getCategories(categoryId, true);
        for (Category cat : subCategories) {
            ContentValues values = new ContentValues();
            values.put("shown", 0);
            db.update("category", values, "id_category = ?", new String[]{String.valueOf(cat.id)});
            hideItemsInCategory(cat.id);
            hideSubCategories(cat.id);
        }
    }

    public void updateItemsWithCategoryPrice(int categoryId) {
        Double categoryPrice = getCategoryPrice(categoryId);
        if (categoryPrice == null) return;

        ContentValues values = new ContentValues();
        values.put("base_price", categoryPrice);
        db.update("item", values, "id_category = ? AND uses_category_price = 1",
                new String[]{String.valueOf(categoryId)});

        List<Category> subCategories = getCategories(categoryId, false);
        for (Category cat : subCategories) {
            if (cat.defaultPrice == null) {
                updateItemsWithCategoryPrice(cat.id);
            }
        }
    }

    public int countItemsInCategory(int categoryId) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM item WHERE id_category = ? AND shown = 1",
                new String[]{String.valueOf(categoryId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        List<Category> subCats = getCategories(categoryId, false);
        for (Category cat : subCats) {
            count += countItemsInCategory(cat.id);
        }
        return count;
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

    public boolean updateItem(int itemId, String name, String picture, Double basePrice, Integer stock,
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

        if (categoryId != null) {
            values.put("id_category", categoryId);
        } else {
            values.putNull("id_category");
        }
        values.put("uses_category_price", usesCategoryPrice ? 1 : 0);

        int rows = db.update("item", values, "id_item = ?", new String[]{String.valueOf(itemId)});
        return rows > 0;
    }

    public boolean deleteItem(int itemId) {
        ContentValues values = new ContentValues();
        values.put("shown", 0);
        int rows = db.update("item", values, "id_item = ?", new String[]{String.valueOf(itemId)});
        return rows > 0;
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
            db.execSQL("UPDATE item SET total_sold = total_sold + 1, " +
                    "current_stock = CASE WHEN current_stock > 0 THEN current_stock - 1 ELSE current_stock END " +
                    "WHERE id_item = ?", new Object[]{itemId});
        }

        return result;
    }

    public List<Sale> getAllSales() {
        List<Sale> sales = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT s.id_sale, s.sold_price, s.sale_time, s.id_export, s.id_item, i.name " +
                        "FROM sale s " +
                        "INNER JOIN item i ON s.id_item = i.id_item " +
                        "ORDER BY s.sale_time DESC",
                null);

        while (cursor.moveToNext()) {
            sales.add(new Sale(
                    cursor.getInt(0),
                    cursor.getDouble(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getInt(4),
                    cursor.getString(5)
            ));
        }
        cursor.close();
        return sales;
    }

    public boolean updateSalePrice(int saleId, double newPrice) {
        ContentValues values = new ContentValues();
        values.put("sold_price", newPrice);
        int rows = db.update("sale", values, "id_sale = ?", new String[]{String.valueOf(saleId)});
        return rows > 0;
    }

    public boolean deleteSale(int saleId, int itemId) {
        int rows = db.delete("sale", "id_sale = ?", new String[]{String.valueOf(saleId)});

        if (rows > 0) {
            db.execSQL("UPDATE item SET total_sold = total_sold - 1, " +
                    "current_stock = CASE WHEN current_stock >= 0 THEN current_stock + 1 ELSE current_stock END " +
                    "WHERE id_item = ?", new Object[]{itemId});
        }

        return rows > 0;
    }

    public boolean resetAllSales() {
        try {
            // Delete all sales
            int salesDeleted = db.delete("sale", null, null);

            // Reset all item counters
            db.execSQL("UPDATE item SET total_sold = 0, current_stock = CASE WHEN current_stock = -1 THEN -1 ELSE current_stock + total_sold END");

            this.cleanHidden();

            return salesDeleted > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean cleanHidden(){
        try {
            //Remove hidden items with no sales left and which are hidden
            db.execSQL("DELETE from item where total_sold = 0 and shown = 0");

            //Remove hidden categories
            db.execSQL("DELETE from category WHERE shown = 0");

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteExportRecord(int recordId) {
        int rows = db.delete("export_record", "id_record = ?", new String[]{String.valueOf(recordId)});
        return rows > 0;
    }

    public int getCurrentBatchSaleCount() {
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM sale WHERE id_export = " +
                        "(SELECT id_export FROM sale_batch ORDER BY id_export DESC LIMIT 1)",
                null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getTotalSaleCount() {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM sale", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void createNewBatch() {
        Cursor cursor = db.rawQuery("SELECT MAX(id_export) FROM sale_batch", null);
        int nextBatchId = 1;
        if (cursor.moveToFirst()) {
            nextBatchId = cursor.getInt(0) + 1;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("name", "Batch " + nextBatchId);
        db.insert("sale_batch", null, values);
    }

    public List<SaleGroup> getSalesGroupedForExport(boolean currentBatchOnly) {
        List<SaleGroup> groups = new ArrayList<>();
        String query;

        if (currentBatchOnly) {
            query = "SELECT s.id_item, i.name, i.id_category, s.sold_price, COUNT(*) as quantity " +
                    "FROM sale s " +
                    "INNER JOIN item i ON s.id_item = i.id_item " +
                    "WHERE s.id_export = (SELECT id_export FROM sale_batch ORDER BY id_export DESC LIMIT 1) " +
                    "GROUP BY s.id_item, s.sold_price " +
                    "ORDER BY i.name, s.sold_price";
        } else {
            query = "SELECT s.id_item, i.name, i.id_category, s.sold_price, COUNT(*) as quantity " +
                    "FROM sale s " +
                    "INNER JOIN item i ON s.id_item = i.id_item " +
                    "GROUP BY s.id_item, s.sold_price " +
                    "ORDER BY i.name, s.sold_price";
        }

        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            groups.add(new SaleGroup(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.isNull(2) ? null : cursor.getInt(2),
                    cursor.getDouble(3),
                    cursor.getInt(4)
            ));
        }
        cursor.close();
        return groups;
    }

    public String getCurrentBatchName() {
        Cursor cursor = db.rawQuery("SELECT name FROM sale_batch ORDER BY id_export DESC LIMIT 1", null);
        String name = "Batch 1";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        return name;
    }

    public int getCurrentBatchId() {
        Cursor cursor = db.rawQuery("SELECT id_export FROM sale_batch ORDER BY id_export DESC LIMIT 1", null);
        int id = 1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    public String getExportDateRange(boolean currentBatchOnly) {
        String query;
        if (currentBatchOnly) {
            query = "SELECT MIN(sale_time), MAX(sale_time) FROM sale " +
                    "WHERE id_export = (SELECT id_export FROM sale_batch ORDER BY id_export DESC LIMIT 1)";
        } else {
            query = "SELECT MIN(sale_time), MAX(sale_time) FROM sale";
        }

        Cursor cursor = db.rawQuery(query, null);
        String dateRange = "";
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            String startDate = cursor.getString(0);
            String endDate = cursor.getString(1);
            dateRange = formatDateRange(startDate, endDate);
        }
        cursor.close();
        return dateRange;
    }

    private String formatDateRange(String start, String end) {
        try {
            // Assuming format: YYYY-MM-DD HH:MM:SS
            String startDate = start.substring(0, 10);
            String endDate = end.substring(0, 10);

            if (startDate.equals(endDate)) {
                return startDate;
            } else {
                return startDate + " to " + endDate;
            }
        } catch (Exception e) {
            return start + " to " + end;
        }
    }

    public void updateBatchExportTime(int batchId) {
        db.execSQL("UPDATE sale_batch SET export_time = datetime('now') WHERE id_export = ?",
                new Object[]{batchId});
    }

    public long saveExportRecord(String filename, String filepath, Integer batchId, String format, boolean isFullExport, String exportName) {
        ContentValues values = new ContentValues();
        values.put("filename", filename);
        values.put("filepath", filepath);
        if (batchId != null) values.put("id_batch", batchId);
        values.put("format", format);
        values.put("is_full_export", isFullExport ? 1 : 0);
        values.put("export_name", exportName);
        return db.insert("export_record", null, values);
    }

    public List<ExportRecord> getAllExportRecords() {
        List<ExportRecord> records = new ArrayList<>();
        Cursor cursor = db.query("export_record", null, null, null, null, null, "export_time DESC");

        while (cursor.moveToNext()) {
            records.add(new ExportRecord(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id_record")),
                    cursor.getString(cursor.getColumnIndexOrThrow("filename")),
                    cursor.getString(cursor.getColumnIndexOrThrow("filepath")),
                    cursor.getString(cursor.getColumnIndexOrThrow("export_time")),
                    cursor.isNull(cursor.getColumnIndexOrThrow("id_batch")) ? -1 :
                            cursor.getInt(cursor.getColumnIndexOrThrow("id_batch")),
                    cursor.getString(cursor.getColumnIndexOrThrow("format")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("is_full_export")) == 1,
                    cursor.isNull(cursor.getColumnIndexOrThrow("export_name")) ? null :
                            cursor.getString(cursor.getColumnIndexOrThrow("export_name"))
            ));
        }
        cursor.close();
        return records;
    }



}
