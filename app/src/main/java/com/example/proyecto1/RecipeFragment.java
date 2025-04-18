package com.example.proyecto1;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class RecipeFragment extends Fragment {

    public interface recipeListener {
        void onRecipeSelected(int pos);
    }

    private ImageView recipeImageView;
    private TextView recipeTextView, ingredientsTextView, contentTextView, stepsTextView, labelTextView;
    private String recipeName, recipeIngredients, recipeSteps, recipeImagePath;
    private int recipeId;
    private recipeListener listener;

    public RecipeFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            recipeTextView = view.findViewById(R.id.recipe_name);
            recipeImageView = view.findViewById(R.id.recipe_image);
            ingredientsTextView = view.findViewById(R.id.recipe_ingredients);
            stepsTextView = view.findViewById(R.id.recipe_steps);
            Button back2ListButton = view.findViewById(R.id.buttonToList);

            recipeTextView.setMovementMethod(new ScrollingMovementMethod());
            if (back2ListButton != null) {
                back2ListButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // sacar el fragment de la pila y destruir ShowRecipeActivity
                        requireActivity().getSupportFragmentManager().popBackStack();
                        getActivity().finish();
                    }
                });
            }
        }
        else {
            contentTextView = view.findViewById(R.id.recipe_content);
            labelTextView = view.findViewById(R.id.textViewContent);
            Button ingredientsButton = view.findViewById(R.id.buttonIngredients);
            Button stepsButton = view.findViewById(R.id.buttonSteps);

            ingredientsButton.setOnClickListener(v -> {
                contentTextView.setText(recipeIngredients);
                labelTextView.setText(R.string.ingredients);
            });

            stepsButton.setOnClickListener(v -> {
                contentTextView.setText(recipeSteps);
                labelTextView.setText(R.string.steps);
            });
        }

        Button editButton = view.findViewById(R.id.buttonEdit);
        Button deleteButton = view.findViewById(R.id.buttonDelete);

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), EditRecipeActivity.class);
                intent.putExtra("recipe_id", recipeId);
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // recipeTextView.getText() es un SpannableString, y devolvería null --> toString()
                    intent.putExtra("name", recipeTextView.getText().toString());
                    intent.putExtra("ingredients", ingredientsTextView.getText());
                    intent.putExtra("steps", stepsTextView.getText());
                    intent.putExtra("image", (String) recipeTextView.getTag());
                }
                else {
                    intent.putExtra("name", recipeName);
                    intent.putExtra("ingredients", recipeIngredients);
                    intent.putExtra("steps", recipeSteps);
                    intent.putExtra("image", recipeImagePath);
                }
                editRecipeLauncher.launch(intent);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // diálogo para asegurar que se quiere eliminar la receta
                DialogDelete dialogoDelete = new DialogDelete(recipeId);
                dialogoDelete.show(getParentFragmentManager(), "etiqueta5");
            }
        });

        return view;
    }

    private final ActivityResultLauncher<Intent> editRecipeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int id = result.getData().getIntExtra("recipe_id", -1);
                    String updatedName = result.getData().getStringExtra("updated_name");
                    String updatedImage = result.getData().getStringExtra("updated_image");
                    String updatedIngredients = result.getData().getStringExtra("updated_ingredients");
                    String updatedSteps = result.getData().getStringExtra("updated_steps");

                    if (id != -1) {
                        recipeId = id;
                    }
                    // actualizar la vista con los nuevos datos
                    updateRecipe(recipeId, updatedName, updatedImage, updatedIngredients, updatedSteps);
                }
            });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (recipeListener) context;
        }
        catch (ClassCastException e) {
            throw new ClassCastException("The class " + context.toString()
            + " has to implement recipeListener");
        }
    }

    public void updateRecipe(int code, String recipeName, String recipeImage, String ingredients, String steps) {
        this.recipeId = code;
        this.recipeName = recipeName;
        this.recipeIngredients = ingredients;
        this.recipeSteps = steps;
        this.recipeImagePath = recipeImage;

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (recipeName != null && ingredients != null && steps != null) {
                recipeTextView.setText(recipeName);
                if (recipeImageView != null) {
                    AppUtils.loadImage(getContext(), recipeImage, recipeImageView);
                }
                // guardar la ruta en el tag del título para poder usarla
                recipeTextView.setTag(recipeImage);
                ingredientsTextView.setText(ingredients);
                stepsTextView.setText(steps);
            }
        }
        else {
            contentTextView.setText(recipeIngredients);
            labelTextView.setText(R.string.ingredients);
        }
    }
}
