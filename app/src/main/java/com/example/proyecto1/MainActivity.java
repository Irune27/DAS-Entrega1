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
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecipeFragment.recipeListener,
RecyclerViewFragment.recipeListener {
    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames;
    private ArrayList<String> images;
    private MyDB dbHelper;
    private int selectedRecipePosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ajustar el idioma y el tema según las preferencias
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        String language = prefs.getString("language", "es");
        setLocale(language);

        setContentView(R.layout.activity_main);

        // si la pantalla está en horizontal
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // comprobar si la actividad se ha lanzado con una receta
            Intent intent = getIntent();
            if (intent.hasExtra("code")) {
                int code = intent.getIntExtra("code", -1);
                String recipeName = intent.getStringExtra("recipe_name");
                String recipeImage = intent.getStringExtra("recipe_image");
                String recipeIngredients = intent.getStringExtra("recipe_ingredients");
                String recipeSteps = intent.getStringExtra("recipe_steps");
                selectedRecipePosition = intent.getIntExtra("selected_position", -1);

                // cargar los detalles de la receta
                RecipeFragment recipeFragment = (RecipeFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.recipeFragment);
                if (recipeFragment != null) {
                    recipeFragment.updateRecipe(code, recipeName, recipeImage, recipeIngredients, recipeSteps);
                }
            }
        }

        dbHelper = MyDB.getInstance(this);

        list = findViewById(R.id.recyclerView);
        if (list == null) {
            list = findViewById(R.id.recyclerViewFragment);
        }

        recipeNames = new ArrayList<>();
        images = new ArrayList<>();

        adapter = new MyAdapter(recipeNames, images, this::onRecipeSelected);
        list.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        list.setLayoutManager(layoutManager);

        Button addButton = findViewById(R.id.button);
        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                startActivity(intent);
            });
        }

        Button settingsButton = findViewById(R.id.settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                // lanzar la actividad de ajustes y destruir MainActivity, para recrearla luego
                // y que se actualice correctamente
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            });
        }

        // actualizar las recetas que se muestran con la lista de recetas de la base de datos
        loadRecipes();

        // pedir permiso para notificaciones, si no está concedido ya
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
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
        Log.d("BackStack", "Back stack size MainActivity: " +
                this.getSupportFragmentManager().getBackStackEntryCount());
        // actualizar las recetas que se muestran con la lista de recetas de la base de datos
        loadRecipes();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            TextView labelTextView = findViewById(R.id.textViewContent);
            Button ingredientsButton = findViewById(R.id.ingredientsButton);
            Button stepsButton = findViewById(R.id.stepsButton);
            Button deleteButton = findViewById(R.id.deleteButton);
            Button editButton = findViewById(R.id.editButton);
            labelTextView.setVisibility(View.VISIBLE);
            ingredientsButton.setVisibility(View.VISIBLE);
            stepsButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.VISIBLE);
            if (selectedRecipePosition == -1) {
                // si no hay una receta seleccionada, mostrar los detalles de la primera
                if (recipeNames.size() > 0) {
                    onRecipeSelected(0);
                    adapter.setSelectedPosition(0);
                }
                else {
                    labelTextView.setVisibility(View.INVISIBLE);
                    ingredientsButton.setVisibility(View.INVISIBLE);
                    stepsButton.setVisibility(View.INVISIBLE);
                    deleteButton.setVisibility(View.INVISIBLE);
                    editButton.setVisibility(View.INVISIBLE);
                }
            } else {
                // si no, mostrar los detalles de la receta seleccionada
                onRecipeSelected(selectedRecipePosition);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void loadRecipes() {
        recipeNames.clear();
        images.clear();

        // recuperar el nombre y la imagen de todas las recetas de la base de datos
        Cursor cursor = dbHelper.getRecipeNamesAndImages();
        if (cursor.moveToFirst()) {
            do {
                recipeNames.add(cursor.getString(0));
                String image = cursor.getString(1);
                if (image != null && !image.isEmpty()) {
                    images.add(image);
                } else {
                    images.add("");
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter.updateData(recipeNames, images, this::onRecipeSelected);
    }

    @Override
    public void onRecipeSelected(int recipePos) {
        selectedRecipePosition = recipePos;
        int orientation = getResources().getConfiguration().orientation;
        Cursor cursor = dbHelper.getAllRecipes();
        // identificar la receta que se ha seleccionado
        cursor.moveToPosition(recipePos);

        int code = cursor.getInt(0);
        String recipeName = cursor.getString(1);
        String image = cursor.getString(2);
        String ingredients = cursor.getString(3);
        String steps = cursor.getString(4);
        cursor.close();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // si el dispositivo está en horizontal
            RecipeFragment recipeFragment = (RecipeFragment) getSupportFragmentManager().
                    findFragmentById(R.id.recipeFragment);
            if (recipeFragment != null) {
                recipeFragment.updateRecipe(code, recipeName, image, ingredients, steps);
            }
            adapter.setSelectedPosition(recipePos);
            adapter.notifyDataSetChanged();
        }
        else {
            // si el dispositivo está en vertical
            Intent i = new Intent(this, ShowRecipeActivity.class);
            i.putExtra("code", code);
            i.putExtra("recipe_name", recipeName);
            i.putExtra("recipe_image", image);
            i.putExtra("recipe_ingredients", ingredients);
            i.putExtra("recipe_steps", steps);
            i.putExtra("selected_position", recipePos);
            startActivity(i);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.closeDatabase();
    }
}
