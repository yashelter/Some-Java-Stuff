package com.example.brainstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class LevelMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_menu);
        getSupportActionBar().hide();
    }
    public void backToMenu(View v){
        Intent intent = new Intent(LevelMenu.this, MainActivity.class);
        startActivity(intent);
    }

    public void hardLevel(View v){
        toGame(2);
    }

    public void mediumLevel(View v){
        toGame(1);
    }

     public void playOnline(View v){
        Intent intent = new Intent(LevelMenu.this, Customize.class);
        startActivity(intent);
    }
    public void easyLevel(View v){
        toGame(0);
    }

    public void toGame(int difficulty){
        Intent intent = new Intent(LevelMenu.this, Game.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }
    public void rules(View v){
        Intent intent = new Intent(LevelMenu.this, Rules.class);
        startActivity(intent);
    }
}