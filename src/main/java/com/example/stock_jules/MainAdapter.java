package com.example.stock_jules;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {
    private Context context;
    private List<Object> items;
    private DatabaseManager dbManager;

    public MainAdapter(Context context, List<Object> items, DatabaseManager dbManager) {
        this.context = context;
        this.items = items;
        this.dbManager = dbManager;
    }

    public void updateData(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);

        if (item instanceof Category) {
            Category category = (Category) item;
            holder.name.setText(category.name);
            holder.stock.setVisibility(View.GONE);
            holder.saleButton.setVisibility(View.GONE);
            holder.customSaleButton.setVisibility(View.GONE);

            if (category.picture != null && !category.picture.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(category.picture);
                holder.image.setImageBitmap(bitmap);
            } else {
                holder.image.setImageResource(android.R.color.transparent);
            }

            holder.itemView.setOnClickListener(v -> {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).openCategory(category.id);
                }
            });

            holder.menuButton.setOnClickListener(v -> showCategoryMenu(v, category));

        } else if (item instanceof Item) {
            Item itemObj = (Item) item;
            holder.name.setText(itemObj.name);

            holder.price.setVisibility(View.VISIBLE);
            holder.price.setText(String.format("€%.2f", itemObj.basePrice));

            if (itemObj.currentStock != -1) {
                holder.stock.setVisibility(View.VISIBLE);
                holder.stock.setText("Stock: " + itemObj.currentStock);
            } else {
                holder.stock.setVisibility(View.GONE);
            }

            holder.saleButton.setVisibility(View.VISIBLE);
            holder.customSaleButton.setVisibility(View.VISIBLE);

            if (itemObj.picture != null && !itemObj.picture.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(itemObj.picture);
                holder.image.setImageBitmap(bitmap);
            } else {
                holder.image.setImageResource(android.R.color.transparent);
            }

            holder.saleButton.setOnClickListener(v -> {
                dbManager.addSale(itemObj.id, itemObj.basePrice);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).onResume();
                }
            });

            holder.customSaleButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, CustomSaleActivity.class);
                intent.putExtra("item_id", itemObj.id);
                intent.putExtra("base_price", itemObj.basePrice);
                context.startActivity(intent);
            });

            holder.menuButton.setOnClickListener(v -> showItemMenu(v, itemObj));
        }
    }

    private void showCategoryMenu(View v, Category category) {
        PopupMenu popup = new PopupMenu(context, v);
        popup.getMenuInflater().inflate(R.menu.item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_edit) {
                // TODO: Edit category
                return true;
            } else if (id == R.id.menu_delete) {
                // TODO: Delete category
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showItemMenu(View v, Item item) {
        PopupMenu popup = new PopupMenu(context, v);
        popup.getMenuInflater().inflate(R.menu.item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_edit) {
                // TODO: Edit item
                return true;
            } else if (id == R.id.menu_delete) {
                // TODO: Delete item
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, stock;
        Button saleButton, customSaleButton, menuButton;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.item_image);
            name = itemView.findViewById(R.id.item_name);
            price = itemView.findViewById(R.id.item_price);
            stock = itemView.findViewById(R.id.item_stock);
            saleButton = itemView.findViewById(R.id.sale_button);
            customSaleButton = itemView.findViewById(R.id.custom_sale_button);
            menuButton = itemView.findViewById(R.id.menu_button);
        }
    }
}