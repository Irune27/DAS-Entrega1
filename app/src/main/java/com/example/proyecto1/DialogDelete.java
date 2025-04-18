package com.example.proyecto1;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

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
                SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", MODE_PRIVATE);
                int userId = prefs.getInt("user_id", -1);
                // guardar la actividad para gestionar el resultado del worker
                Activity activity = getActivity();

                // modo online
                if (userId != -1) {
                    Data data = new Data.Builder()
                            .putString("action", "delete_recipe")
                            .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/delete_recipe.php")
                            .putInt("code", recipeId)
                            .build();

                    OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                            .setInputData(data)
                            .build();

                    WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(registerRequest.getId())
                            .observe(requireActivity(), workInfo -> {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    Data output = workInfo.getOutputData();
                                    boolean success = output.getBoolean("success", false);
                                    String serverMessage = output.getString("message");
                                    String message;

                                    if (success && activity != null) {
                                        Intent intent = new Intent(activity, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        activity.startActivity(intent);
                                        Toast.makeText(activity, activity.getString(R.string.recipe_delete_ok), Toast.LENGTH_SHORT).show();
                                        if (activity instanceof ShowRecipeActivity) {
                                            // en modo portrait, eliminar la actividad
                                            activity.finish();
                                        } else if (activity instanceof MainActivity) {
                                            // en modo landscape, eliminar el fragmento de la pila
                                            FragmentManager fm = ((FragmentActivity) activity).getSupportFragmentManager();
                                            fm.popBackStack();
                                        }
                                    } else {
                                        switch (serverMessage) {
                                            case "Connection error":
                                                message = activity.getString(R.string.connection_error);
                                                break;
                                            case "Missing data":
                                                message = activity.getString(R.string.fill_all_fields);
                                                break;
                                            default:
                                                message = activity.getString(R.string.recipe_delete_error);
                                                break;
                                        }
                                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                    WorkManager.getInstance(getContext()).enqueue(registerRequest);
                }
                // modo offline
                else {
                    // eliminar la receta de la base de datos local usando el ContentProvider
                    Uri uri = ContentUris.withAppendedId(RecipeProvider.CONTENT_URI, recipeId);
                    int deletedRows = requireContext().getContentResolver().delete(uri, null, null);

                    // si se ha eliminado correctamente, volver a MainActivity
                    if (deletedRows > 0) {
                        Intent intent = new Intent(requireActivity(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
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
