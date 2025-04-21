package com.example.proyecto1;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class RecipeProvider extends ContentProvider {

    // URI base para acceder a la tabla Recetas
    public static final String AUTHORITY = "com.example.proyecto1.provider";
    public static final String RECIPE_PATH = "recetas";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + RECIPE_PATH);
    private static final int RECIPES = 1;
    private static final int RECIPE_ID = 2;

    // para distinguir URIs: lista o receta específica
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, RECIPE_PATH, RECIPES);
        uriMatcher.addURI(AUTHORITY, RECIPE_PATH + "/#", RECIPE_ID);
    }

    private MyDB myDB;

    @Override
    public boolean onCreate() {
        // obtener la instancia de la base de datos local
        myDB = MyDB.getInstance(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // consultar datos
        SQLiteDatabase db = myDB.getReadableDatabase();

        switch (uriMatcher.match(uri)) {
            case RECIPES:
                // se han pedido todas las recetas
                return db.query("Recetas", projection, selection, selectionArgs, null, null, sortOrder);
            case RECIPE_ID:
                // se ha pedido una receta específica, así que se filtra por Code
                selection = "Code=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return db.query("Recetas", projection, selection, selectionArgs, null, null, sortOrder);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // devolver el tipo del recurso
        switch (uriMatcher.match(uri)) {
            case RECIPES:
                // una lista
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".recetas";
            case RECIPE_ID:
                // un elemento
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".recetas";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = myDB.getWritableDatabase();

        if (uriMatcher.match(uri) == RECIPES) {
            // insertar la fila en la tabla Recetas
            long id = db.insert("Recetas", null, values);
            if (id > 0) {
                // devolver una URI con el id de la receta añadida
                return ContentUris.withAppendedId(CONTENT_URI, id);
            } else {
                throw new SQLException("Failed to insert row into " + uri);
            }
        }
        throw new IllegalArgumentException("Invalid URI for insert: " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        SQLiteDatabase db = myDB.getWritableDatabase();

        if (uriMatcher.match(uri) == RECIPE_ID) {
            long recipeId = ContentUris.parseId(uri);

            // obtener la ruta a la imagen local antes de borrar la receta
            Cursor cursor = db.query("Recetas", new String[]{"Image"}, "Code=?",
                    new String[]{String.valueOf(recipeId)}, null, null, null);

            String imagePath = null;
            if (cursor != null && cursor.moveToFirst()) {
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow("Image"));
                cursor.close();
            }

            // borrar la receta
            selection = "Code=?";
            selectionArgs = new String[]{String.valueOf(recipeId)};
            int rowsDeleted = db.delete("Recetas", selection, selectionArgs);

            // borrar la imagen local si existe
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    Log.d("RecipeProvider", "Image deleted: " + deleted + " -> " + imagePath);
                }
            }

            return rowsDeleted;
        }
        throw new IllegalArgumentException("Invalid URI for delete: " + uri);
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase db = myDB.getWritableDatabase();
        // editar una receta de la tabla
        if (uriMatcher.match(uri) == RECIPE_ID) {
            selection = "Code=?";
            selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
            return db.update("Recetas", values, selection, selectionArgs);
        }
        throw new IllegalArgumentException("Invalid URI for update: " + uri);
    }
}
