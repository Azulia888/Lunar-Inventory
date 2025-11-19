package com.example.lunar_inventory;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvExporter {
    private static final String TAG = "CsvExporter";
    private Context context;
    private DatabaseManager dbManager;

    public CsvExporter(Context context, DatabaseManager dbManager) {
        this.context = context;
        this.dbManager = dbManager;
    }

    public File exportToCsv(boolean isCurrentBatch, String batchName, String dateRange) {
        try {
            List<SaleGroup> sales = dbManager.getSalesGroupedForExport(isCurrentBatch);
            Map<Integer, List<SaleGroup>> itemsByCategory = organizeSalesByCategory(sales);

            File exportsDir = new File(context.getFilesDir(), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }

            File file = new File(exportsDir, generateFilename());
            writeCsvFile(file, itemsByCategory);

            Log.d(TAG, "CSV exported successfully to: " + file.getAbsolutePath());

            // Create backup
            createBackup(file);

            return file;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV", e);
            return null;
        }
    }

    public File createBackupCsv(boolean isCurrentBatch) {
        try {
            List<SaleGroup> sales = dbManager.getSalesGroupedForExport(isCurrentBatch);
            Map<Integer, List<SaleGroup>> itemsByCategory = organizeSalesByCategory(sales);

            File backupDir = new File(context.getFilesDir(), "exports/backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            File backupFile = new File(backupDir, "backup_" + generateFilename());
            writeCsvFile(backupFile, itemsByCategory);

            Log.d(TAG, "Backup CSV created: " + backupFile.getAbsolutePath());
            return backupFile;
        } catch (IOException e) {
            Log.e(TAG, "Error creating backup CSV", e);
            return null;
        }
    }

    private void createBackup(File originalFile) {
        try {
            File backupDir = new File(context.getFilesDir(), "exports/backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            File backupFile = new File(backupDir, "backup_" + originalFile.getName());

            java.io.FileInputStream fis = new java.io.FileInputStream(originalFile);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(backupFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            Log.d(TAG, "Backup created: " + backupFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error creating backup", e);
        }
    }

    private void writeCsvFile(File file, Map<Integer, List<SaleGroup>> itemsByCategory) throws IOException {
        FileWriter writer = new FileWriter(file);

        // Write header
        writer.append("Parent,Name,Individual Price,Number of Sales,Total\n");

        // Write category hierarchy
        writeCategoryHierarchy(writer, null, itemsByCategory);

        // Write items without category
        if (itemsByCategory.containsKey(null)) {
            writeItemsInCategory(writer, "None", itemsByCategory.get(null));
        }

        writer.flush();
        writer.close();
    }

    private Map<Integer, List<SaleGroup>> organizeSalesByCategory(List<SaleGroup> sales) {
        Map<Integer, List<SaleGroup>> map = new HashMap<>();
        for (SaleGroup sale : sales) {
            Integer catId = sale.categoryId;
            if (!map.containsKey(catId)) {
                map.put(catId, new ArrayList<>());
            }
            map.get(catId).add(sale);
        }
        return map;
    }

    private void writeCategoryHierarchy(FileWriter writer, Integer parentCategoryId,
                                        Map<Integer, List<SaleGroup>> itemsByCategory) throws IOException {
        List<Category> categories = dbManager.getCategories(parentCategoryId, true);

        for (Category category : categories) {
            double categoryTotal = calculateCategoryTotal(category.id, itemsByCategory);
            int categoryCount = calculateCategoryCount(category.id, itemsByCategory);

            String parentName = parentCategoryId == null ? "None" : dbManager.getCategory(parentCategoryId).name;

            // Write category row
            writer.append(escapeCsv(parentName)).append(",");
            writer.append(escapeCsv(category.name)).append(",");
            writer.append("None").append(",");
            writer.append(String.valueOf(categoryCount)).append(",");
            writer.append(String.format(Locale.US, "%.2f", categoryTotal)).append("\n");

            // Write subcategories recursively
            writeCategoryHierarchy(writer, category.id, itemsByCategory);

            // Write items in this category
            if (itemsByCategory.containsKey(category.id)) {
                writeItemsInCategory(writer, category.name, itemsByCategory.get(category.id));
            }
        }
    }

    private void writeItemsInCategory(FileWriter writer, String parentName,
                                      List<SaleGroup> sales) throws IOException {
        for (SaleGroup sale : sales) {
            String priceIndicator = getPriceIndicator(sale.soldPrice, sale.itemId);
            String itemName = sale.itemName + (priceIndicator.isEmpty() ? "" : " " + priceIndicator);

            writer.append(escapeCsv(parentName)).append(",");
            writer.append(escapeCsv(itemName)).append(",");
            writer.append(String.format(Locale.US, "%.2f", sale.soldPrice)).append(",");
            writer.append(String.valueOf(sale.quantity)).append(",");
            writer.append(String.format(Locale.US, "%.2f", sale.total)).append("\n");
        }
    }

    private String getPriceIndicator(double soldPrice, int itemId) {
        Item item = dbManager.getItem(itemId);
        if (item == null) return "";

        double basePrice = item.basePrice;
        if (soldPrice == 0) return "Free";
        if (soldPrice == basePrice) return "";

        double discount = ((basePrice - soldPrice) / basePrice) * 100;
        if (Math.abs(discount - 50) < 0.01) return "50% off";
        if (Math.abs(discount - 25) < 0.01) return "25% off";
        if (Math.abs(discount - 75) < 0.01) return "75% off";

        return String.format(Locale.US, "%.2f%% off", discount);
    }

    private double calculateCategoryTotal(int categoryId, Map<Integer, List<SaleGroup>> itemsByCategory) {
        double total = 0;

        if (itemsByCategory.containsKey(categoryId)) {
            for (SaleGroup sale : itemsByCategory.get(categoryId)) {
                total += sale.total;
            }
        }

        List<Category> subCategories = dbManager.getCategories(categoryId, true);
        for (Category subCat : subCategories) {
            total += calculateCategoryTotal(subCat.id, itemsByCategory);
        }

        return total;
    }

    private int calculateCategoryCount(int categoryId, Map<Integer, List<SaleGroup>> itemsByCategory) {
        int count = 0;

        if (itemsByCategory.containsKey(categoryId)) {
            for (SaleGroup sale : itemsByCategory.get(categoryId)) {
                count += sale.quantity;
            }
        }

        List<Category> subCategories = dbManager.getCategories(categoryId, true);
        for (Category subCat : subCategories) {
            count += calculateCategoryCount(subCat.id, itemsByCategory);
        }

        return count;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String generateFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "export_" + sdf.format(new Date()) + ".csv";
    }
}