package com.example.stock_jules;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ExportSalesActivity extends AppCompatActivity {
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
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}
