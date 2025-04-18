package com.example.proyecto1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class RegisterActivity extends AppCompatActivity {
    private EditText userEditText, passwordEditText, confirmEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setLocale(this);
        setContentView(R.layout.activity_register);

        userEditText = findViewById(R.id.userEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmEditText = findViewById(R.id.confirmEditText);
        Button registerButton = findViewById(R.id.buttonRegister);
        Button backButton = findViewById(R.id.buttonBack);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = userEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString();
                String confirmPassword = confirmEditText.getText().toString();

                // es necesario rellenar los campos de usuario y contraseña
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                    return;
                }

                // los dos campos de la nueva contraseña deben coincidir
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show();
                    return;
                }

                Data data = new Data.Builder()
                        .putString("action", "register")
                        .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/register.php")
                        .putString("username", username)
                        .putString("password", password)
                        .build();

                OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(RegisterActivity.this).getWorkInfoByIdLiveData(registerRequest.getId())
                        .observe(RegisterActivity.this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                Data output = workInfo.getOutputData();
                                boolean success = output.getBoolean("success", false);
                                String serverMessage = output.getString("message");
                                int userId = output.getInt("user_id", -1);
                                String message;

                                if (success) {
                                    message = getString(R.string.user_registered);
                                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                                    // guardar el id del usuario para utilizar en la aplicación
                                    SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putInt("user_id", userId);
                                    editor.apply();

                                    Intent intent = new Intent(RegisterActivity.this, MenuActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    switch (serverMessage) {
                                        case "Connection error":
                                            message = getString(R.string.connection_error);
                                            break;
                                        case "Missing data":
                                            message = getString(R.string.fill_all_fields);
                                            break;
                                        case "The user already exists":
                                            message = getString(R.string.user_already_exists);
                                            break;
                                        default:
                                            message = getString(R.string.error_registering_user);
                                            break;
                                    }
                                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                WorkManager.getInstance(RegisterActivity.this).enqueue(registerRequest);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
