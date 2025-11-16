package com.example.stock_jules;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SaleHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SaleHistoryAdapter adapter;
    private DatabaseManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_history);

        dbManager = new DatabaseManager(this);

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
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}
