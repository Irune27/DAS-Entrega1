package com.example.proyecto1;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class EditRecipeActivity extends BaseRecipeActivity {
    private EditText nameInput, ingredientsInput, stepsInput;
    private int recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setLocale(this);
        setContentView(R.layout.activity_add_recipe);

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

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
            // mostrar los datos actuales de la receta
            recipeId = intent.getIntExtra("recipe_id", -1);
            nameInput.setText(intent.getStringExtra("name"));
            ingredientsInput.setText(intent.getStringExtra("ingredients"));
            stepsInput.setText(intent.getStringExtra("steps"));

            if (savedInstanceState == null) {
                // para que si el usuario cambia la imagen y gira el dispositivo, aparezca la imagen nueva
                imagePath = intent.getStringExtra("image");
            }

            AppUtils.loadImage(this, imagePath, recipeImage);
        }

        selectImageButton.setOnClickListener(v -> openImageChooser());
        cameraButton.setOnClickListener(v -> openCamera());
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // modo online
                if (userId != -1) {
                    updateRecipeOnServer();
                }
                // modo offline
                else updateRecipe();
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
            AppUtils.loadImage(this, imagePath, recipeImage);
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

        // actualizar la receta en local utilizando el ContentProvider
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Image", imagePath);
        values.put("Ingredients", ingredients);
        values.put("Steps", steps);

        Uri uri = ContentUris.withAppendedId(RecipeProvider.CONTENT_URI, recipeId);
        int updated = getContentResolver().update(uri, values, null, null);

        if (updated > 0) {
            // destruir la actividad, volver a RecipeFragment y pasarle los nuevos datos para que actualice la vista
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

    private void updateRecipeOnServer() {
        String name = nameInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();

        // es necesario rellenar los campos del nombre, los ingredientes y los pasos
        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, this.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("Images", "imagePath: " + imagePath + " imagePathOnServer: " + imagePathOnServer);
        // si la imagen ha cambiado, el image path será la ruta al archivo local
        if (!imagePath.startsWith("recipe_images/")) {
            // subir la nueva imagen al servidor SFTP para conseguir la ruta que se va a guardar en la base de datos remota
            uploadImageToServer(imagePath, imagePathOnServer -> {
                updateRecipeInServer(name, imagePathOnServer, ingredients, steps);
            });
        } else {
            // la imagen no ha cambiado, por lo que no hay que almacenar la imagen otra vez y la ruta anterior sigue siendo correcta
            updateRecipeInServer(name, imagePath, ingredients, steps);
        }
    }

    private void updateRecipeInServer(String name, String serverImagePath, String ingredients, String steps) {
        Data data = new Data.Builder()
                .putString("action", "update_recipe")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/update_recipe.php")
                .putInt("code", recipeId)
                .putString("name", name)
                .putString("image", serverImagePath)
                .putString("ingredients", ingredients)
                .putString("steps", steps)
                .build();

        OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(data)
                .build();

        WorkManager.getInstance(EditRecipeActivity.this).getWorkInfoByIdLiveData(registerRequest.getId())
                .observe(EditRecipeActivity.this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Data output = workInfo.getOutputData();
                        boolean success = output.getBoolean("success", false);
                        String serverMessage = output.getString("message");
                        String message;

                        if (success) {
                            Intent intent = new Intent();
                            intent.putExtra("recipe_id", recipeId);
                            intent.putExtra("updated_name", name);
                            intent.putExtra("updated_image", serverImagePath);
                            intent.putExtra("updated_ingredients", ingredients);
                            intent.putExtra("updated_steps", steps);
                            setResult(RESULT_OK, intent);
                            Toast.makeText(this, this.getString(R.string.recipe_update_ok), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            switch (serverMessage) {
                                case "Connection error":
                                    message = getString(R.string.connection_error);
                                    break;
                                case "Missing data":
                                    message = getString(R.string.fill_all_fields);
                                    break;
                                default:
                                    message = getString(R.string.recipe_update_error);
                                    break;
                            }
                            Toast.makeText(EditRecipeActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(EditRecipeActivity.this).enqueue(registerRequest);
    }
}
