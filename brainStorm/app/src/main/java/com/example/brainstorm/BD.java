package com.example.brainstorm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class BD {

    private static final String DATABASE_NAME = "score.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "score";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ALL = "AllS";
    private static final String COLUMN_EASY = "Easy";
    private static final String COLUMN_NORMAL = "Normal";
    private static final String COLUMN_HARD = "Hard";

    private static final int NUM_COLUMN_ID = 0;
    private static final int NUM_COLUMN_ALL = 1;
    private static final int NUM_COLUMN_EASY = 2;
    private static final int NUM_COLUMN_NORMAL = 3;
    private static final int NUM_COLUMN_HARD= 4;

    private SQLiteDatabase mDataBase;

    public BD(Context context) {
        OpenHelper mOpenHelper = new OpenHelper(context);
        mDataBase = mOpenHelper.getWritableDatabase();
    }

    public long insert(int a,int b,int c,int d) {
        ContentValues cv=new ContentValues();
        cv.put(COLUMN_ALL, a);
        cv.put(COLUMN_EASY, b);
        cv.put(COLUMN_NORMAL, c);
        cv.put(COLUMN_HARD,d);
        return mDataBase.insert(TABLE_NAME, null, cv);
    }

    public int update(Stats s) {
        ContentValues cv=new ContentValues();
        cv.put(COLUMN_ALL, s.all);
        cv.put(COLUMN_EASY, s.easy);
        cv.put(COLUMN_NORMAL, s.normal);
        cv.put(COLUMN_HARD, s.hard);
        return mDataBase.update(TABLE_NAME, cv, COLUMN_ID + " = ?",new String[] { String.valueOf(s.id)});
    }

    public void deleteAll() {
        mDataBase.delete(TABLE_NAME, null, null);
    }

    public void delete(long id) {
        mDataBase.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[] { String.valueOf(id) });
    }

    public Stats select(long id) {
        Cursor mCursor = mDataBase.query(TABLE_NAME, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);

        mCursor.moveToFirst();
        int all = mCursor.getInt(NUM_COLUMN_ALL);
        int easy = mCursor.getInt(NUM_COLUMN_EASY);
        int normal = mCursor.getInt(NUM_COLUMN_NORMAL);
        int hard = mCursor.getInt(NUM_COLUMN_HARD);
        return new Stats(id, all, easy, normal, hard);
    }

    public ArrayList<Stats> selectAll() {
        Cursor mCursor = mDataBase.query(TABLE_NAME, null, null, null, null, null, null);

        ArrayList<Stats> arr = new ArrayList<Stats>();
        mCursor.moveToFirst();
        if (!mCursor.isAfterLast()) {
            do {
                long id = mCursor.getLong(NUM_COLUMN_ID);
                int all = mCursor.getInt(NUM_COLUMN_ALL);
                int easy = mCursor.getInt(NUM_COLUMN_EASY);
                int normal = mCursor.getInt(NUM_COLUMN_NORMAL);
                int hard = mCursor.getInt(NUM_COLUMN_HARD);
                arr.add(new Stats(id, all, easy, normal,hard));
            } while (mCursor.moveToNext());
        }
        return arr;
    }

    private class OpenHelper extends SQLiteOpenHelper {

        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String query = "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ALL+ " INT, " +
                    COLUMN_EASY + " INT, " +
                    COLUMN_NORMAL + " INT,"+
                    COLUMN_HARD+" INT);";
            db.execSQL(query);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

}