package com.example.lunar_inventory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.List;

public class ExportHistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ExportHistoryActivity";

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private ExportHistoryAdapter adapter;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_history);

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

        recyclerView = findViewById(R.id.export_history_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadExports();
    }

    private void loadExports() {
        List<ExportRecord> exports = dbManager.getAllExportRecords();
        if (adapter == null) {
            adapter = new ExportHistoryAdapter(this, exports, this::showExportOptionsDialog);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(exports);
        }
    }

    private void showExportOptionsDialog(ExportRecord record) {
        String[] options = {"Re-export PDF", "Re-export CSV", "Delete Export", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Export Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Re-export PDF
                            reExportAsPdf(record);
                            break;
                        case 1: // Re-export CSV
                            reExportAsCsv(record);
                            break;
                        case 2: // Delete Export
                            showDeleteConfirmation(record);
                            break;
                        case 3: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void reExportAsPdf(ExportRecord record) {
        File backupCsv = new File(getBackupPath(record.filepath));

        if (!backupCsv.exists()) {
            Toast.makeText(this, "Error: Backup file not found", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Backup CSV not found: " + backupCsv.getAbsolutePath());
            return;
        }

        try {
            PdfExporter pdfExporter = new PdfExporter(this, dbManager);
            File pdfFile = pdfExporter.exportFromBackupCsv(backupCsv, getReExportFilename(record.filename, "pdf"));

            if (pdfFile != null && pdfFile.exists()) {
                shareFile(pdfFile, "application/pdf");
                Toast.makeText(this, "PDF re-exported successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to re-export PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error re-exporting PDF", e);
            Toast.makeText(this, "Error re-exporting PDF", Toast.LENGTH_LONG).show();
        }
    }

    private void reExportAsCsv(ExportRecord record) {
        File backupCsv = new File(getBackupPath(record.filepath));

        if (!backupCsv.exists()) {
            Toast.makeText(this, "Error: Backup file not found", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Backup CSV not found: " + backupCsv.getAbsolutePath());
            return;
        }

        try {
            // Copy the backup CSV to a new file for sharing
            File exportsDir = new File(getFilesDir(), "exports");
            File csvFile = new File(exportsDir, getReExportFilename(record.filename, "csv"));

            java.io.FileInputStream fis = new java.io.FileInputStream(backupCsv);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(csvFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            shareFile(csvFile, "text/csv");
            Toast.makeText(this, "CSV re-exported successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error re-exporting CSV", e);
            Toast.makeText(this, "Error re-exporting CSV", Toast.LENGTH_LONG).show();
        }
    }

    private void showDeleteConfirmation(ExportRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Export")
                .setMessage("Are you sure you want to delete this export? This will delete all associated files and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteExport(record))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteExport(ExportRecord record) {
        try {
            // Delete original export file
            File originalFile = new File(record.filepath);
            if (originalFile.exists()) {
                originalFile.delete();
                Log.d(TAG, "Deleted original file: " + originalFile.getAbsolutePath());
            }

            // Delete backup CSV
            File backupCsv = new File(getBackupPath(record.filepath));
            if (backupCsv.exists()) {
                backupCsv.delete();
                Log.d(TAG, "Deleted backup CSV: " + backupCsv.getAbsolutePath());
            }

            // Delete any re-export files
            deleteReExportFiles(record.filename);

            // Remove from database
            dbManager.deleteExportRecord(record.id);

            Toast.makeText(this, "Export deleted successfully", Toast.LENGTH_SHORT).show();
            loadExports();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting export", e);
            Toast.makeText(this, "Error deleting export", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteReExportFiles(String originalFilename) {
        File exportsDir = new File(getFilesDir(), "exports");
        if (!exportsDir.exists()) return;

        String baseFilename = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        File[] files = exportsDir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(baseFilename + "_reexport")) {
                    file.delete();
                    Log.d(TAG, "Deleted re-export file: " + file.getName());
                }
            }
        }
    }

    private String getBackupPath(String originalPath) {
        File originalFile = new File(originalPath);
        File backupDir = new File(getFilesDir(), "exports/backup");
        return new File(backupDir, "backup_" + originalFile.getName().replaceFirst("\\.[^.]+$", ".csv")).getAbsolutePath();
    }

    private String getReExportFilename(String originalFilename, String extension) {
        String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        return nameWithoutExt + "_reexport." + extension;
    }

    private void shareFile(File file, String mimeType) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this,
                    "com.example.lunar_inventory.fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Intent chooser = Intent.createChooser(intent, "Share Export");
            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Toast.makeText(this, "No app available to share file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to share file", e);
            Toast.makeText(this, "Failed to share file", Toast.LENGTH_LONG).show();
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
            startActivity(new Intent(this, AddItemActivity.class));
        } else if (id == R.id.nav_add_category) {
            startActivity(new Intent(this, AddCategoryActivity.class));
        } else if (id == R.id.nav_sale_history) {
            startActivity(new Intent(this, SaleHistoryActivity.class));
        } else if (id == R.id.nav_export_sales) {
            startActivity(new Intent(this, ExportSalesActivity.class));
        } else if (id == R.id.nav_export_history) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
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