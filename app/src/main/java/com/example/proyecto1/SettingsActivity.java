package com.example.proyecto1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ajustar el idioma y el tema según las preferencias
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        String language = prefs.getString("language", "es");
        AppUtils.setLocale(this);
        setContentView(R.layout.activity_settings);

        RadioGroup radioGroupTheme = findViewById(R.id.radioGroupTheme);
        RadioButton radioSystem = findViewById(R.id.radioSystem);
        RadioButton radioLight = findViewById(R.id.radioLight);
        RadioButton radioDark = findViewById(R.id.radioDark);
        Button confirmThemeButton = findViewById(R.id.buttonConfirmTheme);

        // marcar el radio button correspondiente a la configuración actual
        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                radioLight.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                radioDark.setChecked(true);
                break;
            default:
                radioSystem.setChecked(true);
                break;
        }

        confirmThemeButton.setOnClickListener(v -> {
            int selectedId = radioGroupTheme.getCheckedRadioButtonId();
            int newThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            if (selectedId == R.id.radioLight) {
                newThemeMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (selectedId == R.id.radioDark) {
                newThemeMode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            // cuando se pulse el botón de guardar, actualizar las preferencias del tema
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("theme_mode", newThemeMode);
            editor.apply();
            AppCompatDelegate.setDefaultNightMode(newThemeMode);
        });

        RadioGroup radioGroup = findViewById(R.id.radioGroupLanguage);
        RadioButton radioSpanish = findViewById(R.id.radioSpanish);
        RadioButton radioEnglish = findViewById(R.id.radioEnglish);
        RadioButton radioBasque = findViewById(R.id.radioBasque);
        Button confirmButton = findViewById(R.id.buttonConfirmLanguage);

        // marcar el radio button correspondiente a la configuración actual
        switch (language) {
            case "en":
                radioEnglish.setChecked(true);
                break;
            case "eu":
                radioBasque.setChecked(true);
                break;
            default:
                radioSpanish.setChecked(true);
                break;
        }

        confirmButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String newLanguage = "es";
            if (selectedId == R.id.radioEnglish) {
                newLanguage = "en";
            } else if (selectedId == R.id.radioBasque) {
                newLanguage = "eu";
            }

            // cuando se pulse el botón de guardar, actualizar las preferencias del idioma
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("language", newLanguage);
            editor.apply();
            AppUtils.setLocale(this, newLanguage);
            recreate();
        });

        Button backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // destruir la actividad y lanzar MainActivity
                Intent intent = new Intent(SettingsActivity.this, MenuActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
