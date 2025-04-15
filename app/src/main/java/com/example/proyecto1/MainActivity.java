package com.example.proyecto1;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecipeFragment.recipeListener,
RecyclerViewFragment.recipeListener {
    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames;
    private ArrayList<String> images;
    private int selectedRecipePosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // si la pantalla está en horizontal
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // comprobar si la actividad se ha lanzado con una receta
            Intent intent = getIntent();
            if (intent.hasExtra("code")) {
                int code = intent.getIntExtra("code", -1);
                String recipeName = intent.getStringExtra("recipe_name");
                String recipeImage = intent.getStringExtra("recipe_image");
                String recipeIngredients = intent.getStringExtra("recipe_ingredients");
                String recipeSteps = intent.getStringExtra("recipe_steps");
                selectedRecipePosition = intent.getIntExtra("selected_position", -1);

                // cargar los detalles de la receta
                RecipeFragment recipeFragment = (RecipeFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.recipeFragment);
                if (recipeFragment != null) {
                    recipeFragment.updateRecipe(code, recipeName, recipeImage, recipeIngredients, recipeSteps);
                }
            }
        }

        list = findViewById(R.id.recyclerView);
        if (list == null) {
            list = findViewById(R.id.recyclerViewFragment);
        }

        recipeNames = new ArrayList<>();
        images = new ArrayList<>();

        adapter = new MyAdapter(recipeNames, images, this::onRecipeSelected);
        list.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);

        Button addButton = findViewById(R.id.button);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                startActivity(intent);
            });
        }

        Button backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
            });
        }

        // actualizar las recetas que se muestran con la lista de recetas de la base de datos
        loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // control de la pila de actividades
        Log Log = null;
        Log.d("BackStack", "Back stack size MainActivity: " +
                this.getSupportFragmentManager().getBackStackEntryCount());
        // actualizar las recetas que se muestran con la lista de recetas de la base de datos
        loadRecipes();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TextView labelTextView = findViewById(R.id.textViewContent);
            Button ingredientsButton = findViewById(R.id.ingredientsButton);
            Button stepsButton = findViewById(R.id.stepsButton);
            Button deleteButton = findViewById(R.id.deleteButton);
            Button editButton = findViewById(R.id.editButton);
            labelTextView.setVisibility(View.VISIBLE);
            ingredientsButton.setVisibility(View.VISIBLE);
            stepsButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            if (selectedRecipePosition == -1) {
                // si no hay una receta seleccionada, mostrar los detalles de la primera
                if (recipeNames.size() > 0) {
                    onRecipeSelected(0);
                    adapter.setSelectedPosition(0);
                }
                else {
                    labelTextView.setVisibility(View.INVISIBLE);
                    ingredientsButton.setVisibility(View.INVISIBLE);
                    stepsButton.setVisibility(View.INVISIBLE);
                    deleteButton.setVisibility(View.INVISIBLE);
                    editButton.setVisibility(View.INVISIBLE);
                }
            } else {
                // si no, mostrar los detalles de la receta seleccionada
                onRecipeSelected(selectedRecipePosition);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void loadRecipes() {
        recipeNames.clear();
        images.clear();

        // recuperar el nombre y la imagen de todas las recetas de la base de datos
        String[] projection = {"Name", "Image"};
        Cursor cursor = getContentResolver().query(RecipeProvider.CONTENT_URI, projection,
                null, null, null);
        if (cursor.moveToFirst()) {
            do {
                recipeNames.add(cursor.getString(0));
                String image = cursor.getString(1);
                if (image != null && !image.isEmpty()) {
                    images.add(image);
                } else {
                    images.add("");
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter.updateData(recipeNames, images, this::onRecipeSelected);
    }

    @Override
    public void onRecipeSelected(int recipePos) {
        selectedRecipePosition = recipePos;
        int orientation = getResources().getConfiguration().orientation;
        Cursor cursor = getContentResolver().query(RecipeProvider.CONTENT_URI, null,
                null, null, null);
        // identificar la receta que se ha seleccionado
        cursor.moveToPosition(recipePos);

        int code = cursor.getInt(0);
        String recipeName = cursor.getString(1);
        String image = cursor.getString(2);
        String ingredients = cursor.getString(3);
        String steps = cursor.getString(4);
        cursor.close();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // si el dispositivo está en horizontal
            RecipeFragment recipeFragment = (RecipeFragment) getSupportFragmentManager().
                    findFragmentById(R.id.recipeFragment);
            if (recipeFragment != null) {
                recipeFragment.updateRecipe(code, recipeName, image, ingredients, steps);
            }
            adapter.setSelectedPosition(recipePos);
            adapter.notifyDataSetChanged();
        }
        else {
            // si el dispositivo está en vertical
            Intent i = new Intent(this, ShowRecipeActivity.class);
            i.putExtra("code", code);
            i.putExtra("recipe_name", recipeName);
            i.putExtra("recipe_image", image);
            i.putExtra("recipe_ingredients", ingredients);
            i.putExtra("recipe_steps", steps);
            i.putExtra("selected_position", recipePos);
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
