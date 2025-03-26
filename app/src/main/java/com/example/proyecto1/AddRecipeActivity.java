package com.example.proyecto1;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddRecipeActivity extends BaseRecipeActivity {
    private EditText nameInput, ingredientsInput, stepsInput;
    private MyDB databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        databaseHelper = MyDB.getInstance(this);

        nameInput = findViewById(R.id.editTextName);
        ingredientsInput = findViewById(R.id.editTextIngredients);
        stepsInput = findViewById(R.id.editTextSteps);
        recipeImage = findViewById(R.id.imageViewRecipe);
        Button selectImageButton = findViewById(R.id.buttonSelectImage);
        Button saveButton = findViewById(R.id.buttonSaveRecipe);
        Button cameraButton = findViewById(R.id.buttonCamera);
        Button backButton = findViewById(R.id.buttonBack);

        selectImageButton.setOnClickListener(v -> openImageChooser());

        saveButton.setOnClickListener(v -> saveRecipe());

        cameraButton.setOnClickListener(v -> openCamera());

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("imagePath", imagePath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imagePath = savedInstanceState.getString("imagePath");
        if (imagePath != null) {
            if (imagePath.matches("\\d+")) {
                recipeImage.setImageResource(Integer.parseInt(imagePath));
            } else {
                recipeImage.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            }
        }
    }

    private void saveRecipe() {
        String name = nameInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();

        // es necesario rellenar los campos del nombre, los ingredientes y los pasos
        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, this.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        long result = databaseHelper.addRecipe(name, imagePath, ingredients, steps);

        // si la receta se ha añadido correctamente, destruir la actividad (volver a MainActivity)
        if (result != -1) {
            Toast.makeText(this, this.getString(R.string.recipe_add_ok), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, this.getString(R.string.recipe_add_error), Toast.LENGTH_SHORT).show();
        }
    }
}
