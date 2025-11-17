package com.example.lunar_inventory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

public class EditItemActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;

    private EditText nameInput, priceInput, stockInput;
    private Spinner categorySpinner;
    private CheckBox useCategoryPriceCheckBox;
    private ImageView imagePreview;
    private Button selectImageButton, saveButton;

    private DatabaseManager dbManager;
    private String selectedImagePath = null;
    private int itemId;
    private Item currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);

        dbManager = new DatabaseManager(this);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentItem = dbManager.getItem(itemId);
        if (currentItem == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameInput = findViewById(R.id.edit_name_input);
        priceInput = findViewById(R.id.edit_price_input);
        stockInput = findViewById(R.id.edit_stock_input);
        categorySpinner = findViewById(R.id.edit_category_spinner);
        useCategoryPriceCheckBox = findViewById(R.id.edit_use_category_price);
        imagePreview = findViewById(R.id.edit_image_preview);
        selectImageButton = findViewById(R.id.edit_select_image_button);
        saveButton = findViewById(R.id.edit_save_button);

        loadItemData();
        loadCategories();

        useCategoryPriceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            priceInput.setEnabled(!isChecked);
            if (isChecked && categorySpinner.getSelectedItemPosition() > 0) {
                List<Category> categories = getAllCategoriesRecursive();
                int categoryId = categories.get(categorySpinner.getSelectedItemPosition() - 1).id;
                Double catPrice = dbManager.getCategoryPrice(categoryId);
                priceInput.setText(String.valueOf(catPrice));
            }
        });

        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (useCategoryPriceCheckBox.isChecked() && position > 0) {
                    List<Category> categories = getAllCategoriesRecursive();
                    int categoryId = categories.get(position - 1).id;
                    Double catPrice = dbManager.getCategoryPrice(categoryId);
                    priceInput.setText(String.valueOf(catPrice));
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        saveButton.setOnClickListener(v -> saveItem());
    }

    private void loadItemData() {
        nameInput.setText(currentItem.name);
        priceInput.setText(String.valueOf(currentItem.basePrice));

        if (currentItem.currentStock != -1) {
            stockInput.setText(String.valueOf(currentItem.currentStock));
        }

        useCategoryPriceCheckBox.setChecked(currentItem.usesCategoryPrice);
        priceInput.setEnabled(!currentItem.usesCategoryPrice);

        if (currentItem.picture != null && !currentItem.picture.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentItem.picture);
            imagePreview.setImageBitmap(bitmap);
            selectedImagePath = currentItem.picture;
        }
    }

    private void loadCategories() {
        List<Category> categories = getAllCategoriesRecursive();
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("None");

        int selectedPosition = 0;
        for (int i = 0; i < categories.size(); i++) {
            categoryNames.add(categories.get(i).name);
            if (currentItem.categoryId != null && categories.get(i).id == currentItem.categoryId) {
                selectedPosition = i + 1;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        categorySpinner.setSelection(selectedPosition);
    }

    private List<Category> getAllCategoriesRecursive() {
        List<Category> allCategories = new ArrayList<>();
        addCategoriesRecursive(null, allCategories, 0);
        return allCategories;
    }

    private void addCategoriesRecursive(Integer parentId, List<Category> result, int depth) {
        List<Category> cats = dbManager.getCategories(parentId, false);
        for (Category cat : cats) {
            result.add(cat);
            addCategoriesRecursive(cat.id, result, depth + 1);
        }
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
        } else {
            stock = -1;
        }

        Integer categoryId = null;
        if (categorySpinner.getSelectedItemPosition() > 0) {
            List<Category> categories = getAllCategoriesRecursive();
            categoryId = categories.get(categorySpinner.getSelectedItemPosition() - 1).id;
        }

        boolean result = dbManager.updateItem(itemId, name, selectedImagePath, price, stock, categoryId, useCategoryPrice);

        if (result) {
            Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}