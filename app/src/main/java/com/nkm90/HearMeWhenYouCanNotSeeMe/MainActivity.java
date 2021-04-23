package com.nkm90.HearMeWhenYouCanNotSeeMe;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.activity_main);

        //Setting the ActionBar tittle
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getResources().getString(R.string.app_name));

        //Assign the elements with the id on the layout
        Button btnLaunch = findViewById(R.id.btnLaunch);
        Button btnLangChang = findViewById(R.id.btnLangChange);
        resultView = findViewById(R.id.result);


        // Intent to launch the menu
        btnLaunch.setOnClickListener(v -> {
            //openMP(v, MediaPipeActivity.class);
            Intent intent = new Intent(MainActivity.this, MenuActivity.class);
            startActivity(intent);
        });

        btnLangChang.setOnClickListener(v -> showChangeLanguageDialog());
    }

    //Method that displays an Alert to choose the language preference
    private void showChangeLanguageDialog() {
        //Array of languages available
        final String[] listLanguages = {"English", "Spanish"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("Choose Language");
        mBuilder.setSingleChoiceItems(listLanguages, -1, (dialog, i) -> {
            if (i==0){
                setLocale("en");
                recreate();
            }
            else if (i==1){
                setLocale("es");
                recreate();
            }

            //Close the dialog once the language has been selected
            dialog.dismiss();
        });

        //Displaying the dialog
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
        //Saving the data to the shared preferences
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
    }

    //Load the language saved on the shared preferences
    public void loadLocale(){
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "");
        setLocale(language);
    }
}