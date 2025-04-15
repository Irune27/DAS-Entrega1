package com.example.proyecto1;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

public class MenuActivity extends AppCompatActivity {
    private int userId;
    private String username;
    private String profileImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ajustar el idioma y el tema según las preferencias
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        String language = prefs.getString("language", "es");
        setLocale(language);

        setContentView(R.layout.activity_menu);

        userId = getIntent().getIntExtra("user_id", -1);
        loadUserData(userId);

        Button buttonRecipes = findViewById(R.id.buttonRecipes);
        buttonRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Button buttonMap = findViewById(R.id.buttonMap);
        buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });

        Button settingsButton = findViewById(R.id.settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                // lanzar SettingsActivity
                Intent intent = new Intent(MenuActivity.this, SettingsActivity.class);
                startActivity(intent);
                // destruir MenuActivity, para recrearla luego y que se actualice correctamente
                finish();
            });
        }

        Button profileButton = findViewById(R.id.profileButton);
        if (profileButton != null) {
            profileButton.setOnClickListener(v -> {
                Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("username", username);
                intent.putExtra("profile_image", profileImage);
                startActivity(intent);
            });
        }

        // pedir permiso para notificaciones, si no está concedido ya
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }

        // establecer la notificación diaria
        createNotificationChannel();
        scheduleDailyNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // control de la pila de actividades
        Log Log = null;
        Log.d("BackStack", "Back stack size MenuActivity: " +
                this.getSupportFragmentManager().getBackStackEntryCount());
        loadUserData(userId);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_id", "channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Recordatorio.");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.enableVibration(true);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // mandar una notificación cada día a las 19:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // dejar un margen de 3 minutos para mandarla
        if (calendar.getTimeInMillis() < System.currentTimeMillis() - 3 * 60 * 1000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    private void setLocale(String languageCode) {
        // establecer idioma
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void loadUserData(int userId) {
        Data inputData = new Data.Builder()
                .putString("action", "get_user_info")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/get_user_info.php")
                .putInt("user_id", userId)
                .build();

        OneTimeWorkRequest infoRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoByIdLiveData(infoRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Data resultData = workInfo.getOutputData();
                        if (resultData.getBoolean("success", false)) {
                            username = resultData.getString("username");
                            profileImage = resultData.getString("profile_image");
                            Log.d("Profile image", "The image:" + profileImage);

                            ImageView userImageView = findViewById(R.id.userImage);
                            String imageUrl = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/" + profileImage;

                            new Thread(() -> {
                                try {
                                    URL url = new URL(imageUrl);
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setDoInput(true);
                                    conn.connect();

                                    int responseCode = conn.getResponseCode();
                                    if (responseCode == HttpURLConnection.HTTP_OK) {
                                        InputStream input = conn.getInputStream();
                                        Bitmap bitmap = BitmapFactory.decodeStream(input);

                                        runOnUiThread(() -> userImageView.setImageBitmap(bitmap));
                                    } else {
                                        Log.e("ImageError", "Error code: " + responseCode);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }).start();

                            TextView userText = findViewById(R.id.userTextView);
                            userText.setText(username);
                        } else {
                            Toast.makeText(this, getString(R.string.error_fetching_data), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        WorkManager.getInstance(getApplicationContext()).enqueue(infoRequest);
    }
}