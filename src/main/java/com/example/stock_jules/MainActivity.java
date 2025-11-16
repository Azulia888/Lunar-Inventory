package com.example.stock_jules;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
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
    private TextView categoryTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = new DatabaseManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup RecyclerView
        categoryTitle = findViewById(R.id.category_title);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new MainAdapter(this, new ArrayList<>(), dbManager);
        recyclerView.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        List<Object> items = new ArrayList<>();
        items.addAll(dbManager.getCategories(currentCategoryId, false));
        items.addAll(dbManager.getItems(currentCategoryId, false));
        adapter.updateData(items);

        if (currentCategoryId == null) {
            categoryTitle.setVisibility(View.GONE);
        } else {
            categoryTitle.setVisibility(View.VISIBLE);
            // Get category name
            List<Category> allCats = dbManager.getCategories(null, true);
            for (Category cat : allCats) {
                if (cat.id == currentCategoryId) {
                    categoryTitle.setText(cat.name);
                    break;
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_add_item) {
            Intent intent = new Intent(this, AddItemActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_add_category) {
            // TODO: Implement
        } else if (id == R.id.nav_sale_history) {
            // TODO: Implement
        } else if (id == R.id.nav_export_sales) {
            // TODO: Implement
        } else if (id == R.id.nav_export_history) {
            // TODO: Implement
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
            // Navigate to parent category
            List<Category> allCats = dbManager.getCategories(null, true);
            for (Category cat : allCats) {
                if (cat.id == currentCategoryId) {
                    currentCategoryId = cat.parentCategory;
                    loadData();
                    return;
                }
            }
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    public void openCategory(int categoryId) {
        currentCategoryId = categoryId;
        loadData();
    }
}