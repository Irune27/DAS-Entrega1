package com.example.proyecto1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "RecetasDB";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase database;

    public MyDB(@Nullable Context context, @Nullable SQLiteDatabase.CursorFactory factory) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE Recetas (" +
                "Code INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Name TEXT NOT NULL, " +
                "Image TEXT, " +
                "Ingredients TEXT NOT NULL, " +
                "Steps TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Recetas");
        onCreate(db);
    }

    public Cursor getAllRecipes() {
        database = this.getReadableDatabase();
        return database.rawQuery("SELECT * FROM Recetas", null);
    }

    public long addRecipe(String name, String imagePath, String ingredients, String steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Image", imagePath);
        values.put("Ingredients", ingredients);
        values.put("Steps", steps);
        long result = db.insert("Recetas", null, values);
        db.close();
        return result;
    }

    public long updateRecipe(int recipeId, String name, String imagePath, String ingredients, String steps) {
        database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Image", imagePath);
        values.put("Ingredients", ingredients);
        values.put("Steps", steps);

        return database.update("Recetas", values, "Code=?", new String[]{String.valueOf(recipeId)});
    }

    public int deleteRecipe(int recipeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete("Recetas", "Code=?", new String[]{String.valueOf(recipeId)});
        db.close();
        return rowsDeleted;
    }

    public void closeDatabase() {
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}
