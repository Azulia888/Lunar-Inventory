package com.example.stock_jules;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class ExportSalesActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private RadioGroup scopeGroup, formatGroup;
    private RadioButton currentBatchRadio, fullExportRadio;
    private RadioButton csvRadio, excelRadio, pdfRadio;
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

        scopeGroup = findViewById(R.id.export_scope_group);
        formatGroup = findViewById(R.id.export_format_group);
        currentBatchRadio = findViewById(R.id.radio_current_batch);
        fullExportRadio = findViewById(R.id.radio_full_export);
        csvRadio = findViewById(R.id.radio_csv);
        excelRadio = findViewById(R.id.radio_excel);
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
        String format = csvRadio.isChecked() ? "CSV" :
                excelRadio.isChecked() ? "XLSX" : "PDF";

        if (!pdfRadio.isChecked()) {
            Toast.makeText(this, "CSV and Excel export not yet implemented", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get batch information
        String batchName = isCurrentBatch ? dbManager.getCurrentBatchName() : "All Batches";
        String dateRange = dbManager.getExportDateRange(isCurrentBatch);
        int batchId = isCurrentBatch ? dbManager.getCurrentBatchId() : -1;

        // Generate PDF
        PdfExporter exporter = new PdfExporter(this, dbManager);
        File pdfFile = exporter.exportToPdf(isCurrentBatch, batchName, dateRange);

        if (pdfFile == null) {
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save export record
        dbManager.saveExportRecord(pdfFile.getName(), pdfFile.getAbsolutePath(),
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

        // Share the PDF
        shareFile(pdfFile);
    }

    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this,
                "com.example.stock_jules.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(intent, "Share Export"));
            Toast.makeText(this, "Export created successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share file", Toast.LENGTH_SHORT).show();
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
            // TODO: Implement
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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