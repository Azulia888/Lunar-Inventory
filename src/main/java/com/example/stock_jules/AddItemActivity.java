package com.example.stock_jules;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddItemActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;

    private EditText nameInput, priceInput, stockInput;
    private Spinner categorySpinner;
    private CheckBox useCategoryPriceCheckBox;
    private ImageView imagePreview;
    private Button selectImageButton, saveButton;

    private DatabaseManager dbManager;
    private String selectedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        dbManager = new DatabaseManager(this);

        nameInput = findViewById(R.id.name_input);
        priceInput = findViewById(R.id.price_input);
        stockInput = findViewById(R.id.stock_input);
        categorySpinner = findViewById(R.id.category_spinner);
        useCategoryPriceCheckBox = findViewById(R.id.use_category_price);
        imagePreview = findViewById(R.id.image_preview);
        selectImageButton = findViewById(R.id.select_image_button);
        saveButton = findViewById(R.id.save_button);

        // Load categories
        loadCategories();

        useCategoryPriceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            priceInput.setEnabled(!isChecked);
            if (isChecked) {
                priceInput.setText("");
            }
        });

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        saveButton.setOnClickListener(v -> saveItem());
    }

    private void loadCategories() {
        List<Category> categories = dbManager.getCategories(null, false);
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("None");

        for (Category cat : categories) {
            categoryNames.add(cat.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imagePreview.setImageBitmap(bitmap);
                selectedImagePath = saveImageToInternalStorage(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        File directory = getFilesDir();
        File file = new File(directory, "item_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void saveItem() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        Double price = null;
        boolean useCategoryPrice = useCategoryPriceCheckBox.isChecked();

        if (!useCategoryPrice) {
            String priceStr = priceInput.getText().toString().trim();
            if (!priceStr.isEmpty()) {
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer stock = null;
        String stockStr = stockInput.getText().toString().trim();
        if (!stockStr.isEmpty()) {
            try {
                stock = Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid stock", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer categoryId = null;
        if (categorySpinner.getSelectedItemPosition() > 0) {
            List<Category> categories = dbManager.getCategories(null, false);
            categoryId = categories.get(categorySpinner.getSelectedItemPosition() - 1).id;
        }

        long result = dbManager.addItem(name, selectedImagePath, price, stock, categoryId, useCategoryPrice);

        if (result != -1) {
            Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to add item", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}