package com.example.lunar_inventory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private MainAdapter adapter;
    private DatabaseManager dbManager;
    private Integer currentCategoryId = null;
    private View categoryHeader;
    private TextView categoryTitle;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = new DatabaseManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navFooter = drawerLayout.findViewById(R.id.github_logo);
        if (navFooter != null) {
            navFooter.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Azulia888/Lunar-Inventory"));
                startActivity(browserIntent);
            });
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        categoryHeader = findViewById(R.id.category_header);
        categoryTitle = findViewById(R.id.category_title);
        backButton = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new MainAdapter(this, new ArrayList<>(), dbManager);
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> goBackToParentCategory());

        loadData();
    }

    private void loadData() {
        List<Object> items = new ArrayList<>();
        items.addAll(dbManager.getCategories(currentCategoryId, false));
        items.addAll(dbManager.getItems(currentCategoryId, false));
        adapter.updateData(items);

        if (currentCategoryId == null) {
            categoryHeader.setVisibility(View.GONE);
        } else {
            categoryHeader.setVisibility(View.VISIBLE);
            Category cat = dbManager.getCategory(currentCategoryId);
            if (cat != null) {
                categoryTitle.setText(cat.name);
            }
        }
    }

    private void goBackToParentCategory() {
        if (currentCategoryId != null) {
            Category cat = dbManager.getCategory(currentCategoryId);
            if (cat != null) {
                currentCategoryId = cat.parentCategory;
                loadData();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            currentCategoryId = null;
            loadData();
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
            Intent intent = new Intent(this, ExportSalesActivity.class);
            startActivity(intent);
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
                        loadData();
                    } else {
                        Toast.makeText(this, "Failed to reset sales", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void editItem(int itemId) {
        Intent intent = new Intent(this, EditItemActivity.class);
        intent.putExtra("item_id", itemId);
        startActivity(intent);
    }

    public void editCategory(int categoryId) {
        Intent intent = new Intent(this, EditCategoryActivity.class);
        intent.putExtra("category_id", categoryId);
        startActivity(intent);
    }

    public void deleteItem(int itemId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbManager.deleteItem(itemId);
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void deleteCategory(int categoryId) {
        int itemCount = dbManager.countItemsInCategory(categoryId);

        if (itemCount > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("This category contains " + itemCount + " item(s). Are you sure you want to delete it?")
                    .setPositiveButton("Continue", (dialog, which) -> showFinalDeleteConfirmation(categoryId, itemCount))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Are you sure you want to delete this category?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbManager.deleteCategory(categoryId);
                        loadData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showFinalDeleteConfirmation(int categoryId, int itemCount) {
        new AlertDialog.Builder(this)
                .setTitle("Final Confirmation")
                .setMessage("WARNING: All " + itemCount + " item(s) in this category will also be deleted. This action cannot be undone. Continue?")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    dbManager.deleteCategory(categoryId);
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentCategoryId != null) {
            goBackToParentCategory();
        } else {
            super.onBackPressed();
        }
    }

    public void openCategory(int categoryId) {
        currentCategoryId = categoryId;
        loadData();
    }
}