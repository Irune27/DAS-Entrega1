package com.example.proyecto1;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditRecipeActivity extends BaseRecipeActivity {
    private EditText nameInput, ingredientsInput, stepsInput;
    private MyDB databaseHelper;
    private int recipeId;

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

        Intent intent = getIntent();
        if (intent != null) {
            recipeId = intent.getIntExtra("recipe_id", -1);
            nameInput.setText(intent.getStringExtra("name"));
            ingredientsInput.setText(intent.getStringExtra("ingredients"));
            stepsInput.setText(intent.getStringExtra("steps"));
            imagePath = intent.getStringExtra("image");

            if (imagePath.matches("\\d+")) {
                recipeImage.setImageResource(Integer.parseInt(imagePath));
            }
            else if (!imagePath.isEmpty()) {
                recipeImage.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            } else {
                recipeImage.setImageResource(R.drawable.default_image);
            }
        }

        selectImageButton.setOnClickListener(v -> openImageChooser());
        cameraButton.setOnClickListener(v -> openCamera());
        saveButton.setOnClickListener(v -> updateRecipe());
        backButton.setOnClickListener(v -> finish());
    }

    private void updateRecipe() {
        String name = nameInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();

        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, "Es necesario rellenar todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        long updated = databaseHelper.updateRecipe(recipeId, name, imagePath, ingredients, steps);
        if (updated > 0) {
            Intent intent = new Intent();
            intent.putExtra("recipe_id", recipeId);
            intent.putExtra("updated_name", name);
            intent.putExtra("updated_image", imagePath);
            intent.putExtra("updated_ingredients", ingredients);
            intent.putExtra("updated_steps", steps);
            setResult(RESULT_OK, intent);
            Toast.makeText(this, "¡Receta actualizada correctamente!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar la receta", Toast.LENGTH_SHORT).show();
        }
    }
}
