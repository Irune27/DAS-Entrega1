package com.example.proyecto1;

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
            fragment.updateRecipe(code, recipeName, recipeImage, recipeIngredients, recipeSteps);
        }
    }

    @Override
    public void onRecipeSelected(int pos) {

    }
}
