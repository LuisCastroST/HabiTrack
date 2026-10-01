package com.example.habitrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "HabitosDB.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_HABITOS = "habitos_diarios";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HABITOS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_habito TEXT, " +
                "completado INTEGER DEFAULT 0)";
        db.execSQL(createTable);

        db.execSQL("INSERT INTO " + TABLE_HABITOS + " (nombre_habito, completado) VALUES ('Agua', 0)");
        db.execSQL("INSERT INTO " + TABLE_HABITOS + " (nombre_habito, completado) VALUES ('Leer', 0)");
        db.execSQL("INSERT INTO " + TABLE_HABITOS + " (nombre_habito, completado) VALUES ('Caminar', 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITOS);
        onCreate(db);
    }

    public void actualizarEstadoHabito(String nombreHabito, boolean estaCompletado) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completado", estaCompletado ? 1 : 0);

        db.update(TABLE_HABITOS, values, "nombre_habito = ?", new String[]{nombreHabito});
        db.close();
    }

    public boolean obtenerEstadoHabito(String nombreHabito) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT completado FROM " + TABLE_HABITOS + " WHERE nombre_habito = ?", new String[]{nombreHabito});

        boolean estado = false;
        if (cursor.moveToFirst()) {
            estado = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return estado;
    }

    public void reiniciarTodosHabitos() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completado", 0);

        db.update(TABLE_HABITOS, values, null, null);
        db.close();
    }
}