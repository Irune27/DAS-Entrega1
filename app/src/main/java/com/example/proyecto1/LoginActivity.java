package com.example.proyecto1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText, passwordEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ajustar el idioma y el tema según las preferencias
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        AppUtils.setLocale(this);

        // resetear el user_id de las preferencias
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("user_id", -1);
        editor.apply();
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.userEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button login = findViewById(R.id.buttonLogin);
        Button register = findViewById(R.id.buttonRegister);
        Button offline = findViewById(R.id.buttonOffline);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // es necesario rellenar los campos de usuario y contraseña
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                    return;
                }

                Data data = new Data.Builder()
                        .putString("action", "login")
                        .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/login.php")
                        .putString("username", username)
                        .putString("password", password)
                        .build();

                OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                        .setInputData(data)
                        .build();

                WorkManager.getInstance(LoginActivity.this).getWorkInfoByIdLiveData(loginRequest.getId())
                        .observe(LoginActivity.this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                Data output = workInfo.getOutputData();
                                boolean success = output.getBoolean("success", false);
                                String serverMessage = output.getString("message");
                                int userId = output.getInt("user_id", -1);
                                String message;

                                if (success) {
                                    message = getString(R.string.login_successful);
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();

                                    // guardar el id del usuario para utilizar en la aplicación
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putInt("user_id", userId);
                                    editor.apply();

                                    Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
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
                                        default:
                                            message = getString(R.string.invalid_credentials);
                                            break;
                                    }
                                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                WorkManager.getInstance(LoginActivity.this).enqueue(loginRequest);
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

        offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, MenuActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}
