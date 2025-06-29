package com.example.brainstorm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Type;

public class Customize extends AppCompatActivity {

    private static int RESULT_LOAD_IMAGE = 1;

    public final String NAME = "photo";
    public ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);
        getSupportActionBar().hide();
        SharedPreferences mSettings = getSharedPreferences("square", Context.MODE_PRIVATE);
        if(!mSettings.contains("square")){
            pasteData("square", false);
            pasteData("romb", false);
            pasteData("triangle", false);
            setTokens(0);
        }
        imageView = findViewById(R.id.customPhoto);
        setupIco(imageView);
        editViews();
    }
    public void setupIco(ImageView v){
        String path;
        Bitmap b = null;
        int image =  R.drawable.krug;
        SharedPreferences mSettings = getSharedPreferences("photo", Context.MODE_PRIVATE);
        if(mSettings.contains("photo")) {
            path = (mSettings.getString("photo", ""));
        }else{
            path = "";
        }
        if(!path.equals("")){
            switch (path){
                case "krug":
                    b = null;
                    image = R.drawable.krug;
                    break;
                case "kvadrat":
                    b = null;
                    image = R.drawable.kvadrat;
                    break;
                case "romb":
                    b = null;
                    image = R.drawable.romb;
                    break;
                case "treug" :
                    b = null;
                    image = R.drawable.treug;
                    break;
                default:
                    b = BitmapFactory.decodeFile(path);
                    break;
            }

        }
        if(b != null){
            v.setImageBitmap(b);
        }else{
            v.setImageResource(image);
        }
    }

    public void setTokens(int value){
        SharedPreferences mSettings = getSharedPreferences("tokens", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("tokens", value);
        editor.apply();
    }

    public void pasteData(String key, Boolean value){
        SharedPreferences mSettings = getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getValue(String key){
        SharedPreferences mSettings = getSharedPreferences(key, Context.MODE_PRIVATE);
        return mSettings.getBoolean(key, false);
    }

    public void editViews(){
        TextView tv1 = findViewById(R.id.shop1);
        TextView tv2 = findViewById(R.id.shop2);
        TextView tv3 = findViewById(R.id.shop3);

        Button b1 = findViewById(R.id.buyR);
        Button b2 = findViewById(R.id.buyK);
        Button b3 = findViewById(R.id.buyT);

        if(getValue("square")){
            tv2.setText(R.string.bought);
            b2.setText(R.string.equip);
        }
        else{
            tv2.setText(R.string.cost);
            b2.setText(R.string.buy);
        }
        if(getValue("romb")){
            tv1.setText(R.string.bought);
            b1.setText(R.string.equip);
        }
        else{
            tv1.setText(R.string.cost2);
            b1.setText(R.string.buy);
        }
        if(getValue("treug")){
            tv3.setText(R.string.bought);
            b3.setText(R.string.equip);
        }
        else{
            tv3.setText(R.string.cost3);
            b3.setText(R.string.buy);
        }
        TextView tv = findViewById(R.id.myTokens);
        tv.setText("  "+getTokens());
    }

    public int getTokens(){ // не готово
        SharedPreferences mSettings = getSharedPreferences("tokens", Context.MODE_PRIVATE);
        return mSettings.getInt("tokens", 0);
    }

    public void onClick(View arg0) {
        if(getTokens() >= 300){
            if(isStoragePermissionGranted()){
                setTokens(getTokens() - 300);
                editViews();
                Intent loadIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(loadIntent, RESULT_LOAD_IMAGE);
            }else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Нет необходимых разрешений!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }else{
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.notokens, Toast.LENGTH_SHORT);
            toast.show();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            SharedPreferences mSettings = getSharedPreferences(NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(NAME, picturePath);
            editor.apply();

            Toast toast = Toast.makeText(getApplicationContext(),
                    "Изображение успешно получено!", Toast.LENGTH_SHORT);
            toast.show();
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }

    }

    public void goBackBtn(View v){
        Intent intent = new Intent(Customize.this, LevelMenu.class);
        startActivity(intent);
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    public void setCircle(View v){

        SharedPreferences mSettings = getSharedPreferences(NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(NAME, "krug");
        editor.apply();
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.pasted, Toast.LENGTH_SHORT);
        toast.show();
        imageView.setImageResource(R.drawable.krug);

    }

    public void setSquare(View v){
        if(getValue("square")){
            SharedPreferences mSettings = getSharedPreferences(NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(NAME, "kvadrat");
            editor.apply();
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.pasted, Toast.LENGTH_SHORT);
            toast.show();
            imageView.setImageResource(R.drawable.kvadrat);
        }else{
            if(getTokens() >= 150){
                setTokens(getTokens() - 150);
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.bought, Toast.LENGTH_SHORT);
                toast.show();
                pasteData("square", true);
                setSquare(v);
            }
            else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.notokens, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        editViews();
    }

    public void setTriangle(View v){
        if(getValue("treug")){
            SharedPreferences mSettings = getSharedPreferences(NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(NAME, "treug");
            editor.apply();
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.pasted, Toast.LENGTH_SHORT);
            toast.show();
            imageView.setImageResource(R.drawable.treug);
        }else{
            if(getTokens() >= 200){
                setTokens(getTokens() - 200);
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.bought, Toast.LENGTH_SHORT);
                toast.show();
                pasteData("treug", true);
                setSquare(v);
            }
            else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.notokens, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        editViews();
    }

    public void setRomb(View v){
        if(getValue("romb")){
            SharedPreferences mSettings = getSharedPreferences(NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.putString(NAME, "romb");
            editor.apply();
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.pasted, Toast.LENGTH_SHORT);
            toast.show();
            imageView.setImageResource(R.drawable.romb);
        }else{
            if(getTokens() >= 100){
                setTokens(getTokens() - 100);
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.bought, Toast.LENGTH_SHORT);
                toast.show();
                pasteData("romb", true);
                setSquare(v);
            }
            else{
                Toast toast = Toast.makeText(getApplicationContext(),
                        R.string.notokens, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        editViews();
    }

}