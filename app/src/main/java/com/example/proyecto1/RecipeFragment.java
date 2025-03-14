package com.example.proyecto1;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class RecipeFragment extends Fragment {

    public interface recipeListener {
        void onRecipeSelected(int pos);
    }

    private ImageView recipeImageView;
    private TextView recipeTextView;
    private TextView ingredientsTextView;
    private TextView stepsTextView;
    private int recipeId;

    private recipeListener listener;

    public RecipeFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);
        recipeTextView = view.findViewById(R.id.recipe_name);
        recipeImageView = view.findViewById(R.id.recipe_image);
        ingredientsTextView = view.findViewById(R.id.recipe_ingredients);
        stepsTextView = view.findViewById(R.id.recipe_steps);

        Button backButton = view.findViewById(R.id.back2ListButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    getActivity().finish();
                }
            });
        }

        Button editButton = view.findViewById(R.id.editButton);
        if (editButton != null) {
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), EditRecipeActivity.class);
                    intent.putExtra("recipe_id", recipeId);
                    intent.putExtra("name", recipeTextView.getText());
                    intent.putExtra("ingredients", ingredientsTextView.getText());
                    intent.putExtra("steps", stepsTextView.getText());
                    intent.putExtra("image", (String) recipeImageView.getTag());
                    editRecipeLauncher.launch(intent);
                }
            });
        }

        Button deleteButton = view.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogDelete dialogoDelete = new DialogDelete(recipeId);
                    dialogoDelete.show(getParentFragmentManager(), "etiqueta5");
                }
            });
        }
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

    public void updateRecipe(int code, String recipeName, String recipeImage, String ingredients,
                             String steps) {
        this.recipeId = code;

        if (recipeTextView != null && ingredients != null && steps != null) {
            recipeTextView.setText(recipeName);
            if (recipeImageView != null) {
                if (recipeImage.matches("\\d+")) {
                    recipeImageView.setImageResource(Integer.parseInt(recipeImage));
                } else {
                    recipeImageView.setImageBitmap(BitmapFactory.decodeFile(recipeImage));
                }
                recipeImageView.setTag(recipeImage);
            }
            ingredientsTextView.setText(ingredients);
            stepsTextView.setText(steps);
        }
    }
}
