package com.example.brainstorm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Game extends AppCompatActivity {
    final Random random = new Random();
    final Context context = this;

    int image = R.drawable.krug;

    public String question;
    private ImageView[][] allImages;
    public Bitmap b;
    public TextView timerText;
    public int answer;
    public int correctAnswer;
    public int numberDiff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        String path;
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

        }else{
            b = null;
        }

        // генерация случайной матрицы и запуск обратного отсчёта
        int seconds = setup();
        // внутренний класс таймера, для обратного отсчёта и вызова метода проверки ответа
        class TimerThread extends Thread {

            private Context context;

            public int seconds;
            public  boolean toStop = false;
            public TextView timer;

            public void doTask(){
                Game.this.takeAnswer();
            }

            public TimerThread(Context context, int seconds, TextView timer) {
                this.context = context;
                this.seconds = seconds;
                this.timer = timer;
                timer.setText("" + seconds);
            }
            public void stopTimer() {
                toStop = true;
            }

            @Override
            public void run() {

                while (!toStop) {
                    try {
                        if(seconds > 0) {
                            seconds--;

                                Game.this.runOnUiThread(new Runnable()
                                {
                                    public void run()
                                    {
                                        timer.setText("" + (seconds + 1));
                                    }
                                });
                            Thread.sleep(1000);
                        }
                        else{
                            Game.this.runOnUiThread(new Runnable()
                            {
                                public void run()
                                {
                                    timer.setText("" + 0);
                                    doTask();
                                }
                            });

                            stopTimer();
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        TimerThread timer = new TimerThread(context, seconds, timerText);
        timer.start();
        writeAllStats();
    }

    public void secret(){
        for (int i = 0; i < numberDiff; i++) {
            for (int j = 0; j < numberDiff; j++) {
                allImages[i][j].setImageResource(R.drawable.vopros);
            }
        }
    }

    public int setup(){
        int difficulty = getIntent().getIntExtra("difficulty", 0);
        numberDiff = difficulty + 3;
        int[][] matrix;
        int sec = 0;
        switch (difficulty){
            case 0:
                setContentView(R.layout.activity_game);
                timerText = (TextView)findViewById(R.id.easytimer);
                matrix = getM(3);
                drawM3(matrix);
                sec = 10;
                break;
            case 1:
                setContentView(R.layout.activity_game_medium);
                timerText = (TextView)findViewById(R.id.timer2);
                matrix = getM(4);
                drawM4(matrix);
                sec = 20;
                break;
            case 2:
                setContentView(R.layout.activity_game_hard);
                timerText = (TextView)findViewById(R.id.timer3);
                matrix = getM(5);
                drawM5(matrix);
                sec = 30;
                break;
            default:
                break;
        }
        return sec;
    }

    public void goBackBtn(View v){
        Intent intent = new Intent(Game.this, LevelMenu.class);
        startActivity(intent);
    }

    public void check(){
        Toast toast, tokens;
        if (answer == correctAnswer){
            toast = Toast.makeText(getApplicationContext(),
                    R.string.correctAnswer, Toast.LENGTH_SHORT);
            int x = (random.nextInt(3) + 1) * numberDiff;
            setTokens(getTokens() + x);
            tokens = Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.getTokens) + "  " + x, Toast.LENGTH_SHORT);

            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            tokens.show();
            switch (numberDiff - 2){
                case 1:
                    pasteData("easy", getValue("easy") + 1);
                    break;
                case 2:
                    pasteData("medium", getValue("medium") + 1);
                    break;
                case 3:
                    pasteData("hard", getValue("hard") + 1);
                    break;
            }
            try {
                Uri notify = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notify);
                r.play();
            } catch (Exception e) {
               Log.e("SomeEROR ", e.toString());
            }
            writeAllStats();

        }else{
            toast = Toast.makeText(getApplicationContext(),
                    R.string.uncorrectAnswer, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
        pasteData("all", getValue("all") + 1);


    }

    public int getTokens(){ // не готово
        SharedPreferences mSettings = getSharedPreferences("tokens", Context.MODE_PRIVATE);
        return mSettings.getInt("tokens", 0);
    }

    public void setTokens(int value){
        SharedPreferences mSettings = getSharedPreferences("tokens", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("tokens", value);
        editor.apply();
    }

    public void writeDefaults(){
        SharedPreferences mSettings = getSharedPreferences("easy", Context.MODE_PRIVATE);
        if(!mSettings.contains("easy")){
            pasteData("easy", 0);
            pasteData("hard", 0);
            pasteData("medium", 0);
            pasteData("all", 0);
        }
    }

    public void writeAllStats(){
        TextView tv;
        writeDefaults();
        switch (numberDiff - 2){
            case 1:
                tv = findViewById(R.id.easyResult);
                tv.setText(""+getValue("easy"));
                break;
            case 2:
                tv = findViewById(R.id.easyResult2);
                tv.setText(""+getValue("medium"));
                break;
            case 3:
                tv = findViewById(R.id.easyResult3);
                tv.setText(""+getValue("hard"));
                break;

        }
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

    public void takeAnswer(){
        secret();
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt, null);

        //Создаем AlertDialog
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);

        //Настраиваем prompt.xml для нашего AlertDialog:
        mDialogBuilder.setView(promptsView);

        //Настраиваем отображение поля для ввода текста в открытом диалоге:
        final EditText userInput = (EditText) promptsView.findViewById(R.id.input_text);
        TextView tyty = (TextView) promptsView.findViewById(R.id.tv);
        tyty.setText(question);

        //Настраиваем сообщение в диалоговом окне:
        mDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                //Вводим текст и отображаем в строке ввода на основном экране:
                                String input =  userInput.getText().toString();
                                if(input.equals("")){
                                    answer = 0;
                                }
                                else{
                                    answer = Integer.parseInt(input);
                                }
                                check();
                            }
                        })
                .setNegativeButton("Не знаю ответ",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                answer = -1;
                                dialog.cancel();
                                check();

                            }
                        });

        //Создаем AlertDialog:
        AlertDialog alertDialog = mDialogBuilder.create();

        //и отображаем его:
        alertDialog.show();

    }

    public void drawM3(int[][] m){
        ImageView[][] imges = new ImageView[3][3];

        imges[0][0] = (ImageView)findViewById(R.id.easy1);
        imges[0][1] = (ImageView)findViewById(R.id.easy2);
        imges[0][2] = (ImageView)findViewById(R.id.easy3);

        imges[1][0] = (ImageView)findViewById(R.id.easy4);
        imges[1][1] = (ImageView)findViewById(R.id.easy5);
        imges[1][2] = (ImageView)findViewById(R.id.easy6);

        imges[2][0] = (ImageView)findViewById(R.id.easy7);
        imges[2][1] = (ImageView)findViewById(R.id.easy8);
        imges[2][2] = (ImageView)findViewById(R.id.easy9);

        allImages = imges;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (m[i][j] == 1 ){
                    if(b != null){
                        imges[i][j].setImageBitmap(b);
                    }
                    else{
                        imges[i][j].setImageResource(image);
                    }

                }
            }
        }
    }

    public void drawM4(int[][] m){
        ImageView[][] imges = new ImageView[4][4];

        imges[0][0] = (ImageView)findViewById(R.id.med1);
        imges[0][1] = (ImageView)findViewById(R.id.med2);
        imges[0][2] = (ImageView)findViewById(R.id.med3);
        imges[0][3] = (ImageView)findViewById(R.id.med4);
        imges[1][0] = (ImageView)findViewById(R.id.med5);
        imges[1][1] = (ImageView)findViewById(R.id.med6);
        imges[1][2] = (ImageView)findViewById(R.id.med7);
        imges[1][3] = (ImageView)findViewById(R.id.med8);
        imges[2][0] = (ImageView)findViewById(R.id.med9);
        imges[2][1] = (ImageView)findViewById(R.id.med10);
        imges[2][2] = (ImageView)findViewById(R.id.med11);
        imges[2][3] = (ImageView)findViewById(R.id.med12);
        imges[3][0] = (ImageView)findViewById(R.id.med13);
        imges[3][1] = (ImageView)findViewById(R.id.med14);
        imges[3][2] = (ImageView)findViewById(R.id.med15);
        imges[3][3] = (ImageView)findViewById(R.id.med16);


        //тут ничо не менять
        allImages = imges;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (m[i][j] == 1 ){
                    if(b != null){
                        imges[i][j].setImageBitmap(b);
                    }
                    else{
                        imges[i][j].setImageResource(image);
                    }
                }
            }
        }
    }

    public void drawM5(int[][] m){
        ImageView[][] imges = new ImageView[5][5];

        imges[0][0] = (ImageView)findViewById(R.id.hard1);
        imges[0][1] = (ImageView)findViewById(R.id.hard2);
        imges[0][2] = (ImageView)findViewById(R.id.hard3);
        imges[0][3] = (ImageView)findViewById(R.id.hard4);
        imges[0][4] = (ImageView)findViewById(R.id.hard5);
        imges[1][0] = (ImageView)findViewById(R.id.hard6);
        imges[1][1] = (ImageView)findViewById(R.id.hard7);
        imges[1][2] = (ImageView)findViewById(R.id.hard8);
        imges[1][3] = (ImageView)findViewById(R.id.hard9);
        imges[1][4] = (ImageView)findViewById(R.id.hard10);
        imges[2][0] = (ImageView)findViewById(R.id.hard11);
        imges[2][1] = (ImageView)findViewById(R.id.hard12);
        imges[2][2] = (ImageView)findViewById(R.id.hard13);
        imges[2][3] = (ImageView)findViewById(R.id.hard14);
        imges[2][4] = (ImageView)findViewById(R.id.hard15);
        imges[3][0] = (ImageView)findViewById(R.id.hard16);
        imges[3][1] = (ImageView)findViewById(R.id.hard17);
        imges[3][2] = (ImageView)findViewById(R.id.hard18);
        imges[3][3] = (ImageView)findViewById(R.id.hard19);
        imges[3][4] = (ImageView)findViewById(R.id.hard20);
        imges[4][0] = (ImageView)findViewById(R.id.hard21);
        imges[4][1] = (ImageView)findViewById(R.id.hard22);
        imges[4][2] = (ImageView)findViewById(R.id.hard23);
        imges[4][3] = (ImageView)findViewById(R.id.hard24);
        imges[4][4] = (ImageView)findViewById(R.id.hard25);


        //тут ничо не менять
        allImages = imges;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (m[i][j] == 1 ){
                    if(b != null){
                        imges[i][j].setImageBitmap(b);
                    }
                    else{
                        imges[i][j].setImageResource(image);
                    }
                }
            }
        }
    }

    public int[][] getM(int n){
        correctAnswer = 0; // считаем точки

        int type = random.nextInt(2);
        int randomI_J = random.nextInt(n);

        int[][] m = new int[n][n];
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                m[i][j] = random.nextInt(2);
            }
        }

        if(type == 0){
            for(int k = 0; k < n; k++){
                if(m[randomI_J][k] == 1){
                    correctAnswer++;
                }
            }
            question = getResources().getString(R.string.question) + " " +
                    (randomI_J + 1) + " " + getResources().getString(R.string.question1);

        }
        else if(type == 1){
            for(int p = 0; p < n; p++){
                if(m[p][randomI_J] == 1){
                    correctAnswer++;
                }
            }
            question = getResources().getString(R.string.question) + " " +
                    (randomI_J + 1) + " " + getResources().getString(R.string.question2);
        }

        return  m;
    }
    public void onClickNewGame(View v){
        toGame(numberDiff - 3);
    }

    public void toGame(int difficulty){
        Intent intent = new Intent(Game.this, Game.class);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
    }



}