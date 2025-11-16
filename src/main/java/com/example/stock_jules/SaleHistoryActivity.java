package com.example.stock_jules;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class SaleHistoryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private SaleHistoryAdapter adapter;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_history);

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

        recyclerView = findViewById(R.id.sale_history_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadSales();
    }

    private void loadSales() {
        List<Sale> sales = dbManager.getAllSales();
        if (adapter == null) {
            adapter = new SaleHistoryAdapter(this, sales, dbManager, this::loadSales);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(sales);
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
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_export_sales) {
            Intent intent = new Intent(this, ExportSalesActivity.class);
            startActivity(intent);
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