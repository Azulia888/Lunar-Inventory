package com.example.lunar_inventory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class ExportSalesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ExportSalesActivity";

    private DrawerLayout drawerLayout;
    private EditText exportNameInput;
    private RadioGroup scopeGroup, formatGroup;
    private RadioButton currentBatchRadio, fullExportRadio;
    private RadioButton csvRadio, pdfRadio;
    private CheckBox endBatchCheckbox;
    private TextView exportInfo;
    private Button exportButton;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_sales);

        dbManager = new DatabaseManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        exportNameInput = findViewById(R.id.export_name_input);
        scopeGroup = findViewById(R.id.export_scope_group);
        formatGroup = findViewById(R.id.export_format_group);
        currentBatchRadio = findViewById(R.id.radio_current_batch);
        fullExportRadio = findViewById(R.id.radio_full_export);
        csvRadio = findViewById(R.id.radio_csv);
        pdfRadio = findViewById(R.id.radio_pdf);
        endBatchCheckbox = findViewById(R.id.end_batch_checkbox);
        exportInfo = findViewById(R.id.export_info);
        exportButton = findViewById(R.id.export_button);

        updateExportInfo();

        scopeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_full_export) {
                endBatchCheckbox.setEnabled(false);
                endBatchCheckbox.setChecked(false);
            } else {
                endBatchCheckbox.setEnabled(true);
            }
            updateExportInfo();
        });

        endBatchCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> updateExportInfo());

        exportButton.setOnClickListener(v -> performExport());
    }

    private void updateExportInfo() {
        int currentBatchCount = dbManager.getCurrentBatchSaleCount();
        int totalCount = dbManager.getTotalSaleCount();

        StringBuilder info = new StringBuilder();
        info.append("Current Batch Sales: ").append(currentBatchCount).append("\n");
        info.append("Total Sales (All Batches): ").append(totalCount).append("\n\n");

        if (currentBatchRadio.isChecked()) {
            info.append("Will export: ").append(currentBatchCount).append(" sales from current batch");
            if (endBatchCheckbox.isChecked()) {
                info.append("\n\nA new batch will be started after export");
            }
        } else {
            info.append("Will export: ").append(totalCount).append(" sales from all batches");
        }

        exportInfo.setText(info.toString());
    }

    private void performExport() {
        boolean isCurrentBatch = currentBatchRadio.isChecked();
        boolean endBatch = endBatchCheckbox.isChecked();
        String format = csvRadio.isChecked() ? "CSV" : "PDF";

        // Get export name
        String customName = exportNameInput.getText().toString().trim();
        String exportName;
        if (!customName.isEmpty()) {
            exportName = customName;
        } else {
            exportName = isCurrentBatch ? dbManager.getCurrentBatchName() : "All Batches";
        }

        String dateRange = dbManager.getExportDateRange(isCurrentBatch);
        int batchId = isCurrentBatch ? dbManager.getCurrentBatchId() : -1;

        Log.d(TAG, "Starting export - Format: " + format + ", Name: " + exportName + ", Date Range: " + dateRange);

        // Generate export file
        File exportFile;
        if (csvRadio.isChecked()) {
            CsvExporter csvExporter = new CsvExporter(this, dbManager);
            exportFile = csvExporter.exportToCsv(isCurrentBatch, exportName, dateRange);
        } else {
            PdfExporter pdfExporter = new PdfExporter(this, dbManager);
            exportFile = pdfExporter.exportToPdf(isCurrentBatch, exportName, dateRange);
        }

        if (exportFile == null || !exportFile.exists()) {
            Log.e(TAG, "Failed to generate export or file doesn't exist");
            Toast.makeText(this, "Failed to generate export", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Export generated successfully at: " + exportFile.getAbsolutePath());
        Log.d(TAG, "File exists: " + exportFile.exists() + ", Size: " + exportFile.length() + " bytes");

        // Save export record
        dbManager.saveExportRecord(exportFile.getName(), exportFile.getAbsolutePath(),
                isCurrentBatch ? batchId : null, format, !isCurrentBatch);

        // Update batch export time
        if (isCurrentBatch) {
            dbManager.updateBatchExportTime(batchId);
        }

        // Create new batch if requested
        if (isCurrentBatch && endBatch) {
            dbManager.createNewBatch();
            Toast.makeText(this, "New batch started", Toast.LENGTH_SHORT).show();
            updateExportInfo();
        }

        // Share the file
        shareFile(exportFile);
    }

    private void shareFile(File file) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    "com.example.lunar_inventory.fileprovider", file);

            Log.d(TAG, "File URI: " + fileUri.toString());

            String mimeType;
            if (file.getName().endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else {
                mimeType = "text/csv";
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent chooser = Intent.createChooser(intent, "Share Export");
            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
                Toast.makeText(this, "Export created successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "No app available to handle share intent");
                Toast.makeText(this, "No app available to share file", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "File path not supported by FileProvider", e);
            Toast.makeText(this, "Error: File path not accessible", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to share file", e);
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (id == R.id.nav_add_item) {
            Intent intent = new Intent(this, AddItemActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_add_category) {
            Intent intent = new Intent(this, AddCategoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_sale_history) {
            Intent intent = new Intent(this, SaleHistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_export_sales) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_export_history) {
            Intent intent = new Intent(this, ExportHistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_reset_sales) {
            showResetSalesDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showResetSalesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset All Sales")
                .setMessage("Are you sure you want to delete all sales? This will reset all sales records and item counters. This action cannot be undone.")
                .setPositiveButton("Yes, Reset", (dialog, which) -> {
                    boolean success = dbManager.resetAllSales();
                    if (success) {
                        Toast.makeText(this, "All sales have been reset", Toast.LENGTH_SHORT).show();
                        updateExportInfo();
                    } else {
                        Toast.makeText(this, "Failed to reset sales", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}