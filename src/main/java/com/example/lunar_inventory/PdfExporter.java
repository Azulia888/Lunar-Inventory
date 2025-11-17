package com.example.lunar_inventory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfExporter {
    private static final String TAG = "PdfExporter";
    private static final int PAGE_WIDTH = 595; // A4 width in points
    private static final int PAGE_HEIGHT = 842; // A4 height in points
    private static final int MARGIN = 40;
    private static final int LINE_HEIGHT = 20;

    private Context context;
    private DatabaseManager dbManager;
    private PdfDocument document;

    public PdfExporter(Context context, DatabaseManager dbManager) {
        this.context = context;
        this.dbManager = dbManager;
    }

    public File exportToPdf(boolean isCurrentBatch, String batchName, String dateRange) {
        document = new PdfDocument();
        PdfDocument.Page currentPage = null;
        Canvas canvas = null;
        Paint paint = new Paint();
        int pageNumber = 1;
        int yPos = MARGIN;

        try {
            // Get sales data
            List<SaleGroup> sales = isCurrentBatch ?
                    dbManager.getSalesGroupedForExport(true) :
                    dbManager.getSalesGroupedForExport(false);

            // Organize by category hierarchy
            Map<Integer, List<SaleGroup>> itemsByCategory = organizeSalesByCategory(sales);

            // Start first page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            currentPage = document.startPage(pageInfo);
            canvas = currentPage.getCanvas();

            // Draw header
            yPos = drawHeader(canvas, paint, batchName, dateRange, yPos);
            yPos += LINE_HEIGHT * 2;

            // Draw category hierarchy
            PageContext ctx = new PageContext(canvas, yPos, pageNumber, currentPage);
            ctx = drawCategoryHierarchy(paint, null, itemsByCategory, 0, ctx);
            currentPage = ctx.page;
            canvas = ctx.canvas;
            yPos = ctx.yPos;

            // Draw items without category
            if (itemsByCategory.containsKey(null)) {
                ctx = drawItemsInCategory(paint, itemsByCategory.get(null), 0, ctx);
                currentPage = ctx.page;
            }

            // Finish the last page
            if (currentPage != null) {
                document.finishPage(currentPage);
            }

            // Create exports directory in internal storage
            File exportsDir = new File(context.getFilesDir(), "exports");
            if (!exportsDir.exists()) {
                boolean created = exportsDir.mkdirs();
                Log.d(TAG, "Exports directory created: " + created);
            }

            // Save to file in exports directory
            File file = new File(exportsDir, generateFilename());
            Log.d(TAG, "Attempting to save PDF to: " + file.getAbsolutePath());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                document.writeTo(fos);
                fos.flush();
                Log.d(TAG, "PDF saved successfully");
                return file;
            } catch (IOException e) {
                Log.e(TAG, "Error saving PDF", e);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            e.printStackTrace();
            return null;
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    private static class PageContext {
        Canvas canvas;
        int yPos;
        int pageNumber;
        PdfDocument.Page page;

        PageContext(Canvas canvas, int yPos, int pageNumber, PdfDocument.Page page) {
            this.canvas = canvas;
            this.yPos = yPos;
            this.pageNumber = pageNumber;
            this.page = page;
        }
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

    private int drawHeader(Canvas canvas, Paint paint, String batchName, String dateRange, int yPos) {
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        float textWidth = paint.measureText(batchName);
        canvas.drawText(batchName, (PAGE_WIDTH - textWidth) / 2, yPos, paint);
        yPos += LINE_HEIGHT;

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        textWidth = paint.measureText(dateRange);
        canvas.drawText(dateRange, (PAGE_WIDTH - textWidth) / 2, yPos, paint);

        return yPos;
    }

    private PageContext drawCategoryHierarchy(Paint paint, Integer parentCategoryId,
                                              Map<Integer, List<SaleGroup>> itemsByCategory,
                                              int indent, PageContext ctx) {
        List<Category> categories = dbManager.getCategories(parentCategoryId, true);

        for (Category category : categories) {
            // Check if we need a new page
            if (ctx.yPos > PAGE_HEIGHT - MARGIN - LINE_HEIGHT * 3) {
                document.finishPage(ctx.page);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++ctx.pageNumber).create();
                ctx.page = document.startPage(pageInfo);
                ctx.canvas = ctx.page.getCanvas();
                ctx.yPos = MARGIN;
            }

            // Draw category name
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            ctx.canvas.drawText(category.name, MARGIN + indent * 20, ctx.yPos, paint);
            ctx.yPos += LINE_HEIGHT;
            paint.setFakeBoldText(false);

            // Draw subcategories recursively
            ctx = drawCategoryHierarchy(paint, category.id, itemsByCategory, indent + 1, ctx);

            // Draw items in this category
            if (itemsByCategory.containsKey(category.id)) {
                ctx = drawItemsInCategory(paint, itemsByCategory.get(category.id), indent + 1, ctx);
            }

            // Draw category total
            double categoryTotal = calculateCategoryTotal(category.id, itemsByCategory);
            int categoryCount = calculateCategoryCount(category.id, itemsByCategory);

            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            String totalText = String.format("Total %s", category.name);
            ctx.canvas.drawText(totalText, MARGIN + indent * 20, ctx.yPos, paint);
            ctx.canvas.drawText(String.valueOf(categoryCount), PAGE_WIDTH - MARGIN - 150, ctx.yPos, paint);
            ctx.canvas.drawText(String.format("€%.2f", categoryTotal), PAGE_WIDTH - MARGIN - 80, ctx.yPos, paint);
            ctx.yPos += LINE_HEIGHT * 1.5f;
            paint.setFakeBoldText(false);
        }

        return ctx;
    }

    private PageContext drawItemsInCategory(Paint paint, List<SaleGroup> sales,
                                            int indent, PageContext ctx) {
        paint.setTextSize(11);

        for (SaleGroup sale : sales) {
            // Check if we need a new page
            if (ctx.yPos > PAGE_HEIGHT - MARGIN - LINE_HEIGHT * 2) {
                document.finishPage(ctx.page);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++ctx.pageNumber).create();
                ctx.page = document.startPage(pageInfo);
                ctx.canvas = ctx.page.getCanvas();
                ctx.yPos = MARGIN;
            }

            // Item name with price indicator
            String priceIndicator = getPriceIndicator(sale.soldPrice, sale.itemId);
            String itemText = sale.itemName + (priceIndicator.isEmpty() ? "" : " " + priceIndicator);
            ctx.canvas.drawText(itemText, MARGIN + indent * 20, ctx.yPos, paint);

            // Price, quantity, total
            ctx.canvas.drawText(String.format("€%.2f", sale.soldPrice), PAGE_WIDTH - MARGIN - 250, ctx.yPos, paint);
            ctx.canvas.drawText(String.valueOf(sale.quantity), PAGE_WIDTH - MARGIN - 150, ctx.yPos, paint);
            ctx.canvas.drawText(String.format("€%.2f", sale.total), PAGE_WIDTH - MARGIN - 80, ctx.yPos, paint);

            ctx.yPos += LINE_HEIGHT;
        }

        return ctx;
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

        return String.format("%.2f%% off", discount);
    }

    private double calculateCategoryTotal(int categoryId, Map<Integer, List<SaleGroup>> itemsByCategory) {
        double total = 0;

        // Add items in this category
        if (itemsByCategory.containsKey(categoryId)) {
            for (SaleGroup sale : itemsByCategory.get(categoryId)) {
                total += sale.total;
            }
        }

        // Add subcategories
        List<Category> subCategories = dbManager.getCategories(categoryId, true);
        for (Category subCat : subCategories) {
            total += calculateCategoryTotal(subCat.id, itemsByCategory);
        }

        return total;
    }

    private int calculateCategoryCount(int categoryId, Map<Integer, List<SaleGroup>> itemsByCategory) {
        int count = 0;

        // Add items in this category
        if (itemsByCategory.containsKey(categoryId)) {
            for (SaleGroup sale : itemsByCategory.get(categoryId)) {
                count += sale.quantity;
            }
        }

        // Add subcategories
        List<Category> subCategories = dbManager.getCategories(categoryId, true);
        for (Category subCat : subCategories) {
            count += calculateCategoryCount(subCat.id, itemsByCategory);
        }

        return count;
    }

    private String generateFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "export_" + sdf.format(new Date()) + ".pdf";
    }
}