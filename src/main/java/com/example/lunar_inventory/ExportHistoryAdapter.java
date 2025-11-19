package com.example.lunar_inventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportHistoryAdapter extends RecyclerView.Adapter<ExportHistoryAdapter.ViewHolder> {
    private Context context;
    private List<ExportRecord> exports;
    private OnExportClickListener listener;

    public interface OnExportClickListener {
        void onExportClick(ExportRecord record);
    }

    public ExportHistoryAdapter(Context context, List<ExportRecord> exports, OnExportClickListener listener) {
        this.context = context;
        this.exports = exports;
        this.listener = listener;
    }

    public void updateData(List<ExportRecord> newExports) {
        this.exports = newExports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.export_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExportRecord record = exports.get(position);

        // Set export name
        String exportName = getExportName(record);
        holder.exportName.setText(exportName);

        // Set export date
        String formattedDate = formatDateTime(record.exportTime);
        holder.exportDate.setText(formattedDate);

        // Set export details
        String details = record.format + " • " + (record.isFullExport ? "Full Export" : "Batch Export");
        holder.exportDetails.setText(details);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExportClick(record);
            }
        });
    }

    private String getExportName(ExportRecord record) {
        // Extract name from filename
        String filename = record.filename;
        if (filename.startsWith("export_")) {
            // Remove "export_" prefix and file extension
            String nameWithExt = filename.substring(7);
            int lastDot = nameWithExt.lastIndexOf('.');
            if (lastDot > 0) {
                return nameWithExt.substring(0, lastDot);
            }
            return nameWithExt;
        }
        return filename;
    }

    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTime);
            return date != null ? outputFormat.format(date) : dateTime;
        } catch (ParseException e) {
            return dateTime;
        }
    }

    @Override
    public int getItemCount() {
        return exports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exportName, exportDate, exportDetails;

        ViewHolder(View itemView) {
            super(itemView);
            exportName = itemView.findViewById(R.id.export_name);
            exportDate = itemView.findViewById(R.id.export_date);
            exportDetails = itemView.findViewById(R.id.export_details);
        }
    }
}