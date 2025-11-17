package com.example.lunar_inventory;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CustomSaleActivity extends AppCompatActivity {
    private EditText customPriceInput;
    private Button giftButton, off50Button, off25Button, off75Button, confirmButton;

    private DatabaseManager dbManager;
    private int itemId;
    private double basePrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_sale);

        dbManager = new DatabaseManager(this);

        itemId = getIntent().getIntExtra("item_id", -1);
        basePrice = getIntent().getDoubleExtra("base_price", 0.0);

        customPriceInput = findViewById(R.id.custom_price_input);
        giftButton = findViewById(R.id.gift_button);
        off50Button = findViewById(R.id.off_50_button);
        off25Button = findViewById(R.id.off_25_button);
        off75Button = findViewById(R.id.off_75_button);
        confirmButton = findViewById(R.id.confirm_button);

        giftButton.setOnClickListener(v -> customPriceInput.setText("0"));
        off50Button.setOnClickListener(v -> customPriceInput.setText(String.valueOf(basePrice * 0.5)));
        off25Button.setOnClickListener(v -> customPriceInput.setText(String.valueOf(basePrice * 0.75)));
        off75Button.setOnClickListener(v -> customPriceInput.setText(String.valueOf(basePrice * 0.25)));

        confirmButton.setOnClickListener(v -> {
            String priceStr = customPriceInput.getText().toString().trim();
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                dbManager.addSale(itemId, price);
                Toast.makeText(this, "Sale recorded", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}