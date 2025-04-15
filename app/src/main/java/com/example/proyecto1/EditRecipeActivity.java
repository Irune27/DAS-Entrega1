package com.example.proyecto1;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditRecipeActivity extends BaseRecipeActivity {
    private EditText nameInput, ingredientsInput, stepsInput;
    private int recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

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

            // ajustar la manera de mostrar la imagen teniendo en cuenta si es la imagen
            // predeterminada o una imagen añadida por el usuario y guardada en el
            // almacenamiento externo
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

    private void updateRecipe() {
        String name = nameInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();

        // es necesario rellenar los campos del nombre, los ingredientes y los pasos
        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, this.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // si la receta se ha actualizado correctamente, destruir la actividad, volver a RecipeFragment
        // y pasarle los nuevos datos para que actualice la vista
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Image", imagePath);
        values.put("Ingredients", ingredients);
        values.put("Steps", steps);

        Uri uri = ContentUris.withAppendedId(RecipeProvider.CONTENT_URI, recipeId);
        int updated = getContentResolver().update(uri, values, null, null);

        if (updated > 0) {
            Intent intent = new Intent();
            intent.putExtra("recipe_id", recipeId);
            intent.putExtra("updated_name", name);
            intent.putExtra("updated_image", imagePath);
            intent.putExtra("updated_ingredients", ingredients);
            intent.putExtra("updated_steps", steps);
            setResult(RESULT_OK, intent);
            Toast.makeText(this, this.getString(R.string.recipe_update_ok), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, this.getString(R.string.recipe_update_error), Toast.LENGTH_SHORT).show();
        }
    }
}
