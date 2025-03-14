package com.example.proyecto1;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements RecipeFragment.recipeListener,
RecyclerViewFragment.recipeListener {
    private RecyclerView list;
    private MyAdapter adapter;
    private ArrayList<String> recipeNames;
    private ArrayList<String> images;
    private MyDB dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dbHelper = new MyDB(this, null);

        list = findViewById(R.id.recyclerView);
        if (list == null) {
            list = findViewById(R.id.recyclerViewFragment);
            TextView ingredientsTextView = findViewById(R.id.textViewIngredients);
            TextView stepsTextView = findViewById(R.id.textViewSteps);
            ingredientsTextView.setVisibility(View.INVISIBLE);
            stepsTextView.setVisibility(View.INVISIBLE);
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

        loadRecipes();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
        }

        createNotificationChannel();
        scheduleDailyNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log Log = null;
        Log.d("BackStack", "Back stack size MainActivity: " +
                this.getSupportFragmentManager().getBackStackEntryCount());
        loadRecipes();
    }

    private void loadRecipes() {
        recipeNames.clear();
        images.clear();

        Cursor cursor = dbHelper.getAllRecipes();
        if (cursor.moveToFirst()) {
            do {
                recipeNames.add(cursor.getString(1));
                String image = cursor.getString(2);
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
        int orientation = getResources().getConfiguration().orientation;
        Cursor cursor = dbHelper.getAllRecipes();
        cursor.moveToPosition(recipePos);

        int code = cursor.getInt(0);
        String recipeName = cursor.getString(1);
        String image = cursor.getString(2);
        String ingredients = cursor.getString(3);
        String steps = cursor.getString(4);
        cursor.close();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            RecipeFragment recipeFragment = (RecipeFragment) getSupportFragmentManager().
                    findFragmentById(R.id.recipeFragment);
            if (recipeFragment != null) {
                TextView ingredientsTextView = findViewById(R.id.textViewIngredients);
                TextView stepsTextView = findViewById(R.id.textViewSteps);
                ingredientsTextView.setVisibility(View.VISIBLE);
                stepsTextView.setVisibility(View.VISIBLE);
                recipeFragment.updateRecipe(code, recipeName, image, ingredients, steps);
            }
        }
        else {
            Intent i = new Intent(this, ShowRecipeActivity.class);
            i.putExtra("code", code);
            i.putExtra("recipe_name", recipeName);
            i.putExtra("recipe_image", image);
            i.putExtra("recipe_ingredients", ingredients);
            i.putExtra("recipe_steps", steps);
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

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis() - 3 * 60 * 1000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.closeDatabase();
    }
}
