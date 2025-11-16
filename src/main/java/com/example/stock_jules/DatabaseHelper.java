package com.example.stock_jules;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "stock_jules.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE category(" +
                "id_category INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "picture TEXT," +
                "category_default_price REAL," +
                "shown INTEGER DEFAULT 1," +
                "parent_category INTEGER," +
                "FOREIGN KEY(parent_category) REFERENCES category(id_category))");

        db.execSQL("CREATE TABLE sale_batch(" +
                "id_export INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "export_time TEXT)");

        db.execSQL("CREATE TABLE item(" +
                "id_item INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "picture TEXT," +
                "base_price REAL," +
                "current_stock INTEGER DEFAULT -1," +
                "total_sold INTEGER DEFAULT 0," +
                "uses_category_price INTEGER DEFAULT 0," +
                "shown INTEGER DEFAULT 1," +
                "id_category INTEGER," +
                "FOREIGN KEY(id_category) REFERENCES category(id_category))");

        db.execSQL("CREATE TABLE sale(" +
                "id_sale INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sold_price REAL," +
                "sale_time TEXT DEFAULT CURRENT_TIMESTAMP," +
                "id_export INTEGER NOT NULL," +
                "id_item INTEGER NOT NULL," +
                "FOREIGN KEY(id_export) REFERENCES sale_batch(id_export)," +
                "FOREIGN KEY(id_item) REFERENCES item(id_item))");

        // Create initial sale batch
        db.execSQL("INSERT INTO sale_batch (name) VALUES ('Batch 1')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS sale");
        db.execSQL("DROP TABLE IF EXISTS item");
        db.execSQL("DROP TABLE IF EXISTS sale_batch");
        db.execSQL("DROP TABLE IF EXISTS category");
        onCreate(db);
    }
}