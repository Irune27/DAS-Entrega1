package com.example.proyecto1;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ShowRecipeActivity extends AppCompatActivity implements RecipeFragment.recipeListener {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_recipe);

        int code = getIntent().getIntExtra("code", -1);
        String recipeName = getIntent().getStringExtra("recipe_name");
        String recipeImage = getIntent().getStringExtra("recipe_image");
        String recipeIngredients = getIntent().getStringExtra("recipe_ingredients");
        String recipeSteps = getIntent().getStringExtra("recipe_steps");

        RecipeFragment fragment = (RecipeFragment) getSupportFragmentManager().
                findFragmentById(R.id.recipeFragment);
        if (fragment != null) {
            // actualizar la vista con los detalles de la receta
            fragment.updateRecipe(code, recipeName, recipeImage, recipeIngredients, recipeSteps);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // cerrar la actividad y abrir MainActivity para que salga la lista en landscape
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("code", getIntent().getIntExtra("code", -1));
            intent.putExtra("recipe_name", getIntent().getStringExtra("recipe_name"));
            intent.putExtra("recipe_image", getIntent().getStringExtra("recipe_image"));
            intent.putExtra("recipe_ingredients", getIntent().getStringExtra("recipe_ingredients"));
            intent.putExtra("recipe_steps", getIntent().getStringExtra("recipe_steps"));
            intent.putExtra("selected_position", getIntent().getIntExtra("selected_position", -1));
            intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }



    @Override
    public void onRecipeSelected(int pos) {

    }
}
