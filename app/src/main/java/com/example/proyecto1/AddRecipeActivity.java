package com.example.proyecto1;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class AddRecipeActivity extends BaseRecipeActivity {
    private EditText nameInput, ingredientsInput, stepsInput;

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

        selectImageButton.setOnClickListener(v -> openImageChooser());
        cameraButton.setOnClickListener(v -> openCamera());
        backButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userId != -1) {
                    // modo online
                    saveRecipeToServer(userId);
                }
                // modo offline
                else saveRecipe();
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

        // añadir la receta a la base de datos local usando el ContentProvider
        ContentValues values = new ContentValues();
        values.put("Name", name);
        values.put("Image", imagePath);
        values.put("Ingredients", ingredients);
        values.put("Steps", steps);

        Uri insertUri = getContentResolver().insert(RecipeProvider.CONTENT_URI, values);

        // si la receta se ha añadido correctamente, destruir la actividad (volver a MainActivity)
        if (insertUri != null) {
            Toast.makeText(this, this.getString(R.string.recipe_add_ok), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, this.getString(R.string.recipe_add_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveRecipeToServer(int userId) {
        String name = nameInput.getText().toString().trim();
        String ingredients = ingredientsInput.getText().toString().trim();
        String steps = stepsInput.getText().toString().trim();

        // es necesario rellenar los campos del nombre, los ingredientes y los pasos
        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, this.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // añadir la receta a la base de datos remota, después de subir la imagen al servidor SFTP
        uploadImageToServer(imagePath, imagePathOnServer -> {

            Data data = new Data.Builder()
                    .putString("action", "save_recipe")
                    .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/save_recipe.php")
                    .putString("name", name)
                    .putString("image", imagePathOnServer)
                    .putString("ingredients", ingredients)
                    .putString("steps", steps)
                    .putInt("user_id", userId)
                    .build();

            OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(AddRecipeActivity.this).getWorkInfoByIdLiveData(registerRequest.getId())
                    .observe(AddRecipeActivity.this, workInfo -> {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data output = workInfo.getOutputData();
                            boolean success = output.getBoolean("success", false);
                            String serverMessage = output.getString("message");
                            String message;

                            if (success) {
                                Toast.makeText(this, this.getString(R.string.recipe_add_ok), Toast.LENGTH_SHORT).show();
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
                                        message = getString(R.string.recipe_add_error);
                                        break;
                                }
                                Toast.makeText(AddRecipeActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            WorkManager.getInstance(AddRecipeActivity.this).enqueue(registerRequest);
        });
    }
}
