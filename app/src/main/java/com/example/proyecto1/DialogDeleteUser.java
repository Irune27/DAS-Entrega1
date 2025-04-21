package com.example.proyecto1;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DialogDeleteUser extends DialogFragment {
    private int userId;

    public DialogDeleteUser(int userId) {
        this.userId = userId;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        // confirmar que realmente se quiere eliminar la cuenta
        builder.setTitle(getContext().getString(R.string.delete_account_title));
        builder.setMessage(getContext().getString(R.string.delete_account_info));
        builder.setPositiveButton(getContext().getString(R.string.delete_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // guardar la actividad para gestionar el resultado del worker
                Activity activity = getActivity();

                Data data = new Data.Builder()
                        .putString("action", "delete_user")
                        .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/delete_user.php")
                        .putInt("user_id", userId)
                        .build();

                OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(registerRequest.getId())
                        .observe(requireActivity(), workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                Data output = workInfo.getOutputData();
                                boolean success = output.getBoolean("success", false);

                                if (success && activity != null) {
                                    Toast.makeText(activity, activity.getString(R.string.account_delete_ok), Toast.LENGTH_SHORT).show();

                                    // arrancar una nueva pila de actividades y borrar la anterior
                                    Intent intent = new Intent(activity, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    activity.startActivity(intent);
                                    activity.finish();
                                } else {
                                    Toast.makeText(activity, activity.getString(R.string.account_delete_error), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                WorkManager.getInstance(getContext()).enqueue(registerRequest);
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
