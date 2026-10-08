package com.example.habitrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habitrack.db";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_HABITS_TABLE = "CREATE TABLE " + HabitContract.HabitEntry.TABLE_NAME + " ("
                + HabitContract.HabitEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.HabitEntry.COLUMN_NOMBRE + " TEXT NOT NULL, "
                + HabitContract.HabitEntry.COLUMN_CATEGORIA + " TEXT DEFAULT 'General', "
                + HabitContract.HabitEntry.COLUMN_DIAS + " INTEGER DEFAULT 30, "
                + HabitContract.HabitEntry.COLUMN_COLOR + " TEXT DEFAULT '#4CAF50', "
                + HabitContract.HabitEntry.COLUMN_EN_DASHBOARD + " INTEGER DEFAULT 1);";

        String CREATE_REGISTROS_TABLE = "CREATE TABLE " + HabitContract.RegistroEntry.TABLE_NAME + " ("
                + HabitContract.RegistroEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.RegistroEntry.COLUMN_HABITO_ID + " INTEGER, "
                + HabitContract.RegistroEntry.COLUMN_FECHA + " TEXT NOT NULL, "
                + HabitContract.RegistroEntry.COLUMN_COMPLETADO + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + HabitContract.RegistroEntry.COLUMN_HABITO_ID + ") REFERENCES "
                + HabitContract.HabitEntry.TABLE_NAME + "(" + HabitContract.HabitEntry._ID + ") ON DELETE CASCADE);";

        db.execSQL(CREATE_HABITS_TABLE);
        db.execSQL(CREATE_REGISTROS_TABLE);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.RegistroEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.HabitEntry.TABLE_NAME);
        onCreate(db);
    }

    public long insertarHabito(String nombre, String categoria, int dias, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.HabitEntry.COLUMN_NOMBRE, nombre);
        values.put(HabitContract.HabitEntry.COLUMN_CATEGORIA, categoria);
        values.put(HabitContract.HabitEntry.COLUMN_DIAS, dias);
        values.put(HabitContract.HabitEntry.COLUMN_COLOR, color);
        values.put(HabitContract.HabitEntry.COLUMN_EN_DASHBOARD, 1);
        return db.insert(HabitContract.HabitEntry.TABLE_NAME, null, values);
    }

    public long agregarHabito(String nombre) {
        return insertarHabito(nombre, "General", 30, "#4CAF50");
    }

    public Cursor obtenerTodosLosHabitos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.HabitEntry.TABLE_NAME, null, null, null, null, null, null);
    }

    public Cursor obtenerHabitosDashboard() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = HabitContract.HabitEntry.COLUMN_EN_DASHBOARD + " = ?";
        String[] selectionArgs = new String[]{"1"};
        return db.query(HabitContract.HabitEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
    }

    public void actualizarVisibilidadDashboard(int id, boolean enDashboard) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.HabitEntry.COLUMN_EN_DASHBOARD, enDashboard ? 1 : 0);
        db.update(HabitContract.HabitEntry.TABLE_NAME, values, HabitContract.HabitEntry._ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void guardarEstadoHabitoFecha(int habitoId, String fecha, boolean completado) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.RegistroEntry.TABLE_NAME,
                HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ? AND " + HabitContract.RegistroEntry.COLUMN_FECHA + " = ?",
                new String[]{String.valueOf(habitoId), fecha});
        if (completado) {
            ContentValues values = new ContentValues();
            values.put(HabitContract.RegistroEntry.COLUMN_HABITO_ID, habitoId);
            values.put(HabitContract.RegistroEntry.COLUMN_FECHA, fecha);
            values.put(HabitContract.RegistroEntry.COLUMN_COMPLETADO, 1);
            db.insert(HabitContract.RegistroEntry.TABLE_NAME, null, values);
        }
    }

    public boolean estaCompletadoEnFecha(int habitoId, String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(HabitContract.RegistroEntry.TABLE_NAME,
                new String[]{HabitContract.RegistroEntry.COLUMN_COMPLETADO},
                HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ? AND " + HabitContract.RegistroEntry.COLUMN_FECHA + " = ? AND " + HabitContract.RegistroEntry.COLUMN_COMPLETADO + " = 1",
                new String[]{String.valueOf(habitoId), fecha},
                null, null, null);

        boolean completado = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return completado;
    }

    public Cursor obtenerHabitosCompletadosPorFecha(String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h." + HabitContract.HabitEntry.COLUMN_NOMBRE + ", h." + HabitContract.HabitEntry.COLUMN_CATEGORIA + ", h." + HabitContract.HabitEntry.COLUMN_COLOR
                + " FROM " + HabitContract.HabitEntry.TABLE_NAME + " h"
                + " INNER JOIN " + HabitContract.RegistroEntry.TABLE_NAME + " r"
                + " ON h." + HabitContract.HabitEntry._ID + " = r." + HabitContract.RegistroEntry.COLUMN_HABITO_ID
                + " WHERE r." + HabitContract.RegistroEntry.COLUMN_FECHA + " = ? AND r." + HabitContract.RegistroEntry.COLUMN_COMPLETADO + " = 1";
        return db.rawQuery(query, new String[]{fecha});
    }

    public Cursor obtenerTodosLosRegistros() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.RegistroEntry.TABLE_NAME, null, null, null, null, null, null);
    }

    public void eliminarHabito(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.HabitEntry.TABLE_NAME, HabitContract.HabitEntry._ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(HabitContract.RegistroEntry.TABLE_NAME, HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void reiniciarTodo() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.RegistroEntry.TABLE_NAME, null, null);
        db.delete(HabitContract.HabitEntry.TABLE_NAME, null, null);
        db.close();
    }


}