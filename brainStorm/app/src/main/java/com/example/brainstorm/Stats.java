package com.example.brainstorm;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Serializable;

class Stats  implements Serializable {
    public long id;
    public int all;
    public int easy;
    public int normal;
    public int hard;
    // токены не хранятся в бд для защиты от взлома

    public Stats(long id, int a, int b, int c, int d){
        all = a;
        easy = b;
        normal = c;
        hard = d;
        this.id = id;
    }
}