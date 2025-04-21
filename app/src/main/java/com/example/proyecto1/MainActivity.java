package com.example.proyecto1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecipeFragment.recipeListener,
RecyclerViewFragment.recipeListener {
    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames;
    private ArrayList<String> images;
    private int selectedRecipePosition = -1;
    private int code;
    private String recipeName, image, ingredients, steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setLocale(this);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        list = findViewById(R.id.recyclerView);
        Button addButton = findViewById(R.id.buttonAdd);
        Button backButton = findViewById(R.id.buttonBack);
        Button syncButton = findViewById(R.id.buttonSync);

        recipeNames = new ArrayList<>();
        images = new ArrayList<>();
        adapter = new MyAdapter(this, recipeNames, images, this::onRecipeSelected);
        list.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                startActivity(intent);
            }
        });
        backButton.setOnClickListener(v -> finish());
        if (userId != -1) {
            syncButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent myService = new Intent(MainActivity.this, MyService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(myService);
                    } else {
                        startService(myService);
                    }
                }
            });
        }
        else {
            syncButton.setEnabled(false);
            syncButton.setAlpha(0.5f);
        }

        // actualizar las recetas que se muestran con la lista de recetas del servidor
        if (userId != -1) fetchRecipesFromServer(userId);
        // actualizar las recetas que se muestran con la lista de recetas de la base de datos local
        else loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        // actualizar las recetas que se muestran con la lista de recetas del servidor
        if (userId != -1) fetchRecipesFromServer(userId);
        // actualizar las recetas que se muestran con la lista de recetas de la base de datos local
        else loadRecipes();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TextView labelTextView = findViewById(R.id.textViewContent);
            Button ingredientsButton = findViewById(R.id.buttonIngredients);
            Button stepsButton = findViewById(R.id.buttonSteps);
            Button deleteButton = findViewById(R.id.buttonDelete);
            Button editButton = findViewById(R.id.buttonEdit);
            labelTextView.setVisibility(View.VISIBLE);
            ingredientsButton.setVisibility(View.VISIBLE);
            stepsButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // actualizar el intent de la actividad
        setIntent(intent);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // en modo landscape
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
    }

    private void loadRecipes() {
        AppUtils.loadRecipesFromLocal(this, (names, imgs) -> {
            recipeNames.clear();
            // se devuelve la lista entera de nombres de recetas
            recipeNames.addAll(names);

            images.clear();
            // se devuelve la lista entera de rutas a las imágenes de las recetas
            images.addAll(imgs);

            adapter.updateData(recipeNames, images, this::onRecipeSelected);
        });

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            showRecipeLandscape();
        }
    }

    private void fetchRecipesFromServer(int userId) {
        AppUtils.fetchRecipesFromServer(this, this, userId, (names, imgs) -> {
            recipeNames.clear();
            // se devuelve la lista entera de nombres de recetas
            recipeNames.addAll(names);

            images.clear();
            // se devuelve la lista entera de rutas a las imágenes de las recetas
            images.addAll(imgs);

            adapter.updateData(recipeNames, images, this::onRecipeSelected);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                showRecipeLandscape();
            }
        });
    }

    @Override
    public void onRecipeSelected(int recipePos) {
        selectedRecipePosition = recipePos;

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        if (userId == -1) {
            // modo offline
            Cursor cursor = getContentResolver().query(RecipeProvider.CONTENT_URI, null,
                    null, null, null);

            // identificar la receta que se ha seleccionado
            cursor.moveToPosition(recipePos);

            code = cursor.getInt(0);
            recipeName = cursor.getString(1);
            image = cursor.getString(2);
            ingredients = cursor.getString(3);
            steps = cursor.getString(4);
            cursor.close();

            showRecipe(recipePos);
        } else {
            // modo online
            Data inputData = new Data.Builder()
                    .putString("action", "get_recipes")
                    .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/get_recipes.php")
                    .putInt("user_id", userId)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data output = workInfo.getOutputData();
                            String json = output.getString("recipes_json");

                            if (json != null && !json.isEmpty()) {
                                try {
                                    JSONArray array = new JSONArray(json);
                                    JSONObject obj = array.getJSONObject(recipePos);

                                    code = obj.getInt("Code");
                                    recipeName = obj.optString("Name", getString(R.string.unnamed));
                                    image = obj.optString("Image", "");
                                    ingredients = obj.optString("Ingredients", "");
                                    steps = obj.optString("Steps", "");

                                    showRecipe(recipePos);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            WorkManager.getInstance(this).enqueue(request);
        }
    }

    private void showRecipe(int recipePos) {
        int orientation = getResources().getConfiguration().orientation;
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

    private void showRecipeLandscape() {
        TextView labelTextView = findViewById(R.id.textViewContent);
        Button ingredientsButton = findViewById(R.id.buttonIngredients);
        Button stepsButton = findViewById(R.id.buttonSteps);
        Button deleteButton = findViewById(R.id.buttonDelete);
        Button editButton = findViewById(R.id.buttonEdit);
        if (selectedRecipePosition == -1) {
            // si no hay una receta seleccionada, mostrar los detalles de la primera
            if (!recipeNames.isEmpty()) {
                onRecipeSelected(0);
                adapter.setSelectedPosition(0);
            } else {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
