package com.example.proyecto1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DialogDelete extends DialogFragment {
    private int recipeId;

    public DialogDelete(int recipeId) {
        this.recipeId = recipeId;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // confirmar que realmente se quiere eliminar la receta
        builder.setTitle(getContext().getString(R.string.delete_title));
        builder.setPositiveButton(getContext().getString(R.string.delete_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // eliminar la receta
                MyDB db = MyDB.getInstance(requireContext());
                int deletedRows = db.deleteRecipe(recipeId);

                // si se ha eliminado correctamente, volver a MainActivity
                if (deletedRows > 0) {
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton(getContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        return builder.create();
    }
}
