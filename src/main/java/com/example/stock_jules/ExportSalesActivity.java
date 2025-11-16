package com.example.stock_jules;

import android.content.Intent;
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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

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

        // TODO: Implement actual export functionality
        Toast.makeText(this,
                "Export functionality not yet implemented\n" +
                        "Scope: " + (isCurrentBatch ? "Current Batch" : "Full Export") + "\n" +
                        "Format: " + format + "\n" +
                        "End Batch: " + (endBatch ? "Yes" : "No"),
                Toast.LENGTH_LONG).show();

        // If end batch is checked and exporting current batch, create new batch
        if (isCurrentBatch && endBatch) {
            dbManager.createNewBatch();
            Toast.makeText(this, "New batch started", Toast.LENGTH_SHORT).show();
            updateExportInfo();
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