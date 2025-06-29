package com.example.brainstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;



public class Raiting extends AppCompatActivity {

    BD mDBConnector;
    long id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDBConnector=new BD(this);
        setContentView(R.layout.activity_raiting);
        writeDefaults();
        UpdateViews();
    }

    public void UpdateViews(){
        TextView tokens = findViewById(R.id.thisTokens);
        TextView all = findViewById(R.id.thisGames);
        TextView easy = findViewById(R.id.thisEasy);
        TextView med = findViewById(R.id.thisMedium);
        TextView hard = findViewById(R.id.thisHard);
        tokens.setText(""+getValue("tokens"));
        all.setText(""+getValue("all"));
        easy.setText(""+getValue("easy"));
        med.setText(""+getValue("medium"));
        hard.setText(""+getValue("hard"));
        // Обновляем данные в бд.

        Stats s = mDBConnector.select(id);

        s.all = getValue("all");
        s.normal = getValue("medium");
        s.easy = getValue("easy");
        s.hard = getValue("hard");
        mDBConnector.update(s);


    }
    public void pasteData(String key, int value){
        SharedPreferences mSettings = getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(key, value);
        editor.apply();

    }

    public int getValue(String key){
        SharedPreferences mSettings = getSharedPreferences(key, Context.MODE_PRIVATE);
        return mSettings.getInt(key, 0);
    }
    public void writeDefaults(){
        SharedPreferences mSettings = getSharedPreferences("easy", Context.MODE_PRIVATE);
        if(!mSettings.contains("easy")){
            pasteData("easy", 0);
            pasteData("hard", 0);
            pasteData("medium", 0);
            pasteData("all", 0);
            pasteData("tokens", 0);
            id = mDBConnector.insert(getValue("all"),
                    getValue("easy"), getValue("medium"), getValue("hard"));
            pasteData("ID", (int) id);
        }
        id = getValue("ID");
    }
    public void backToMenu(View v){
        Intent intent = new Intent(Raiting.this, MainActivity.class);
        startActivity(intent);
    }



}