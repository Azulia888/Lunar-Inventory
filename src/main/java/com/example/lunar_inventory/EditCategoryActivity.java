package com.example.lunar_inventory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class EditCategoryActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;

    private EditText nameInput, priceInput;
    private Spinner parentCategorySpinner;
    private ImageView imagePreview;
    private Button selectImageButton, saveButton;

    private DatabaseManager dbManager;
    private String selectedImagePath = null;
    private int categoryId;
    private Category currentCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_category);

        dbManager = new DatabaseManager(this);

        categoryId = getIntent().getIntExtra("category_id", -1);
        if (categoryId == -1) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentCategory = dbManager.getCategory(categoryId);
        if (currentCategory == null) {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameInput = findViewById(R.id.edit_category_name_input);
        priceInput = findViewById(R.id.edit_category_price_input);
        parentCategorySpinner = findViewById(R.id.edit_parent_category_spinner);
        imagePreview = findViewById(R.id.edit_category_image_preview);
        selectImageButton = findViewById(R.id.edit_category_select_image_button);
        saveButton = findViewById(R.id.edit_category_save_button);

        loadCategoryData();
        loadParentCategories();

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        saveButton.setOnClickListener(v -> saveCategory());
    }

    private void loadCategoryData() {
        nameInput.setText(currentCategory.name);

        if (currentCategory.defaultPrice != null) {
            priceInput.setText(String.valueOf(currentCategory.defaultPrice));
        }

        if (currentCategory.picture != null && !currentCategory.picture.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentCategory.picture);
            imagePreview.setImageBitmap(bitmap);
            selectedImagePath = currentCategory.picture;
        }
    }

    private void loadParentCategories() {
        List<Category> categories = getAllCategoriesRecursive();
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("None");

        int selectedPosition = 0;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            if (cat.id != categoryId && !isDescendant(cat.id, categoryId)) {
                categoryNames.add(cat.name);
                if (currentCategory.parentCategory != null && cat.id == currentCategory.parentCategory) {
                    selectedPosition = categoryNames.size() - 1;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        parentCategorySpinner.setAdapter(adapter);
        parentCategorySpinner.setSelection(selectedPosition);
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

    private boolean isDescendant(int potentialDescendant, int ancestorId) {
        Category cat = dbManager.getCategory(potentialDescendant);
        while (cat != null && cat.parentCategory != null) {
            if (cat.parentCategory == ancestorId) {
                return true;
            }
            cat = dbManager.getCategory(cat.parentCategory);
        }
        return false;
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
        File file = new File(directory, "category_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void saveCategory() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        Double price = null;
        String priceStr = priceInput.getText().toString().trim();
        if (!priceStr.isEmpty()) {
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Integer parentId = null;
        if (parentCategorySpinner.getSelectedItemPosition() > 0) {
            List<Category> categories = getAllCategoriesRecursive();
            List<Category> filtered = new ArrayList<>();
            for (Category cat : categories) {
                if (cat.id != categoryId && !isDescendant(cat.id, categoryId)) {
                    filtered.add(cat);
                }
            }
            parentId = filtered.get(parentCategorySpinner.getSelectedItemPosition() - 1).id;
        }

        boolean priceChanged = (currentCategory.defaultPrice == null && price != null) ||
                (currentCategory.defaultPrice != null && !currentCategory.defaultPrice.equals(price));

        boolean result = dbManager.updateCategory(categoryId, name, selectedImagePath, price, parentId);

        if (result) {
            if (priceChanged && price != null) {
                dbManager.updateItemsWithCategoryPrice(categoryId);
            }
            Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update category", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}