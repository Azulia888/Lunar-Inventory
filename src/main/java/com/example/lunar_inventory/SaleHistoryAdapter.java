package com.example.lunar_inventory;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SaleHistoryAdapter extends RecyclerView.Adapter<SaleHistoryAdapter.ViewHolder> {
    private Context context;
    private List<Sale> sales;
    private DatabaseManager dbManager;
    private OnDataChangedListener listener;

    public interface OnDataChangedListener {
        void onDataChanged();
    }

    public SaleHistoryAdapter(Context context, List<Sale> sales, DatabaseManager dbManager, OnDataChangedListener listener) {
        this.context = context;
        this.sales = sales;
        this.dbManager = dbManager;
        this.listener = listener;
    }

    public void updateData(List<Sale> newSales) {
        this.sales = newSales;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sale_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sale sale = sales.get(position);

        holder.itemName.setText(sale.itemName);
        holder.saleTime.setText(sale.saleTime);
        holder.salePrice.setText(String.format("€%.2f", sale.soldPrice));

        holder.editButton.setOnClickListener(v -> showEditDialog(sale));
        holder.deleteButton.setOnClickListener(v -> showDeleteDialog(sale));
    }

    private void showEditDialog(Sale sale) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Sale Price");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(sale.soldPrice));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double newPrice = Double.parseDouble(input.getText().toString());
                dbManager.updateSalePrice(sale.id, newPrice);
                if (listener != null) listener.onDataChanged();
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showDeleteDialog(Sale sale) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Sale")
                .setMessage("Are you sure you want to delete this sale?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbManager.deleteSale(sale.id, sale.itemId);
                    if (listener != null) listener.onDataChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return sales.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, saleTime, salePrice;
        Button editButton, deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.sale_item_name);
            saleTime = itemView.findViewById(R.id.sale_time);
            salePrice = itemView.findViewById(R.id.sale_price);
            editButton = itemView.findViewById(R.id.edit_price_button);
            deleteButton = itemView.findViewById(R.id.delete_sale_button);
        }
    }
}