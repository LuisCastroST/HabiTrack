package com.example.habitrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habitrack.db";
    private static final int DATABASE_VERSION = 10;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CATEGORIAS_TABLE = "CREATE TABLE " + HabitContract.CategoriaEntry.TABLE_NAME + " ("
                + HabitContract.CategoriaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.CategoriaEntry.COLUMN_NOMBRE + " TEXT NOT NULL);";

        String CREATE_HABITS_TABLE = "CREATE TABLE " + HabitContract.HabitEntry.TABLE_NAME + " ("
                + HabitContract.HabitEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.HabitEntry.COLUMN_NOMBRE + " TEXT NOT NULL, "
                + HabitContract.HabitEntry.COLUMN_CATEGORIA_ID + " INTEGER, "
                + HabitContract.HabitEntry.COLUMN_DIAS + " INTEGER DEFAULT 30, "
                + HabitContract.HabitEntry.COLUMN_COLOR + " TEXT DEFAULT '#4CAF50', "
                + HabitContract.HabitEntry.COLUMN_EN_DASHBOARD + " INTEGER DEFAULT 1, "
                + HabitContract.HabitEntry.COLUMN_LATITUD + " REAL DEFAULT 0.0, "
                + HabitContract.HabitEntry.COLUMN_LONGITUD + " REAL DEFAULT 0.0, "
                + "fecha_creacion TEXT, "
                + "FOREIGN KEY(" + HabitContract.HabitEntry.COLUMN_CATEGORIA_ID + ") REFERENCES "
                + HabitContract.CategoriaEntry.TABLE_NAME + "(" + HabitContract.CategoriaEntry._ID + ") ON DELETE SET NULL);";

        String CREATE_REGISTROS_TABLE = "CREATE TABLE " + HabitContract.RegistroEntry.TABLE_NAME + " ("
                + HabitContract.RegistroEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.RegistroEntry.COLUMN_HABITO_ID + " INTEGER, "
                + HabitContract.RegistroEntry.COLUMN_FECHA + " TEXT NOT NULL, "
                + HabitContract.RegistroEntry.COLUMN_COMPLETADO + " INTEGER DEFAULT 0, "
                + "distancia REAL DEFAULT 0.0, "
                + "tiempo_segundos INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + HabitContract.RegistroEntry.COLUMN_HABITO_ID + ") REFERENCES "
                + HabitContract.HabitEntry.TABLE_NAME + "(" + HabitContract.HabitEntry._ID + ") ON DELETE CASCADE);";

        String CREATE_RECORDATORIOS_TABLE = "CREATE TABLE " + HabitContract.RecordatorioEntry.TABLE_NAME + " ("
                + HabitContract.RecordatorioEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HabitContract.RecordatorioEntry.COLUMN_HABITO_ID + " INTEGER, "
                + HabitContract.RecordatorioEntry.COLUMN_HORA + " INTEGER NOT NULL, "
                + HabitContract.RecordatorioEntry.COLUMN_MINUTO + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + HabitContract.RecordatorioEntry.COLUMN_HABITO_ID + ") REFERENCES "
                + HabitContract.HabitEntry.TABLE_NAME + "(" + HabitContract.HabitEntry._ID + ") ON DELETE CASCADE);";

        db.execSQL(CREATE_CATEGORIAS_TABLE);
        db.execSQL(CREATE_HABITS_TABLE);
        db.execSQL(CREATE_REGISTROS_TABLE);
        db.execSQL(CREATE_RECORDATORIOS_TABLE);

        db.execSQL("INSERT INTO " + HabitContract.CategoriaEntry.TABLE_NAME + " (" + HabitContract.CategoriaEntry.COLUMN_NOMBRE + ") VALUES ('Salud');");
        db.execSQL("INSERT INTO " + HabitContract.CategoriaEntry.TABLE_NAME + " (" + HabitContract.CategoriaEntry.COLUMN_NOMBRE + ") VALUES ('Estudio');");
        db.execSQL("INSERT INTO " + HabitContract.CategoriaEntry.TABLE_NAME + " (" + HabitContract.CategoriaEntry.COLUMN_NOMBRE + ") VALUES ('Actividad Física');");
        db.execSQL("INSERT INTO " + HabitContract.CategoriaEntry.TABLE_NAME + " (" + HabitContract.CategoriaEntry.COLUMN_NOMBRE + ") VALUES ('Dinero');");
        db.execSQL("INSERT INTO " + HabitContract.CategoriaEntry.TABLE_NAME + " (" + HabitContract.CategoriaEntry.COLUMN_NOMBRE + ") VALUES ('General');");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.RegistroEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.RecordatorioEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.HabitEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + HabitContract.CategoriaEntry.TABLE_NAME);
        onCreate(db);
    }

    public long agregarHabito(String nombre) {
        return insertarHabito(nombre, 5, 30, "#4CAF50");
    }

    public long insertarRecordatorio(int habitoId, int hora, int minuto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.RecordatorioEntry.COLUMN_HABITO_ID, habitoId);
        values.put(HabitContract.RecordatorioEntry.COLUMN_HORA, hora);
        values.put(HabitContract.RecordatorioEntry.COLUMN_MINUTO, minuto);
        return db.insert(HabitContract.RecordatorioEntry.TABLE_NAME, null, values);
    }

    public long insertarHabito(String nombre, int categoriaId, int dias, String color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.HabitEntry.COLUMN_NOMBRE, nombre);
        values.put(HabitContract.HabitEntry.COLUMN_CATEGORIA_ID, categoriaId);
        values.put(HabitContract.HabitEntry.COLUMN_DIAS, dias);
        values.put(HabitContract.HabitEntry.COLUMN_COLOR, color);
        values.put(HabitContract.HabitEntry.COLUMN_EN_DASHBOARD, 1);

        String fechaActual = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        values.put("fecha_creacion", fechaActual);

        return db.insert(HabitContract.HabitEntry.TABLE_NAME, null, values);
    }

    public Cursor obtenerTodosLosHabitos() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h.*, c." + HabitContract.CategoriaEntry.COLUMN_NOMBRE + " AS categoria_nombre"
                + " FROM " + HabitContract.HabitEntry.TABLE_NAME + " h"
                + " LEFT JOIN " + HabitContract.CategoriaEntry.TABLE_NAME + " c"
                + " ON h." + HabitContract.HabitEntry.COLUMN_CATEGORIA_ID + " = c." + HabitContract.CategoriaEntry._ID;
        return db.rawQuery(query, null);
    }

    public Cursor obtenerRecordatoriosPorHabito(int habitoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.RecordatorioEntry.TABLE_NAME, null,
                HabitContract.RecordatorioEntry.COLUMN_HABITO_ID + " = ?",
                new String[]{String.valueOf(habitoId)}, null, null, null);
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

    public boolean actualizarUbicacionHabito(int idHabito, double latitud, double longitud) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.HabitEntry.COLUMN_LATITUD, latitud);
        values.put(HabitContract.HabitEntry.COLUMN_LONGITUD, longitud);

        int filasAfectadas = db.update(
                HabitContract.HabitEntry.TABLE_NAME,
                values,
                HabitContract.HabitEntry._ID + " = ?",
                new String[]{String.valueOf(idHabito)}
        );
        db.close();
        return filasAfectadas > 0;
    }

    public void guardarEstadoHabitoFecha(int habitoId, String fecha, boolean completado) {
        guardarEstadoHabitoFechaConDetalle(habitoId, fecha, completado, 0.0f, 0L);
    }

    public void guardarEstadoHabitoFechaConDetalle(int habitoId, String fecha, boolean completado, float distancia, long tiempoSegundos) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.RegistroEntry.TABLE_NAME,
                HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ? AND " + HabitContract.RegistroEntry.COLUMN_FECHA + " = ?",
                new String[]{String.valueOf(habitoId), fecha});
        if (completado) {
            ContentValues values = new ContentValues();
            values.put(HabitContract.RegistroEntry.COLUMN_HABITO_ID, habitoId);
            values.put(HabitContract.RegistroEntry.COLUMN_FECHA, fecha);
            values.put(HabitContract.RegistroEntry.COLUMN_COMPLETADO, 1);
            values.put("distancia", distancia);
            values.put("tiempo_segundos", tiempoSegundos);
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

    public String obtenerNombreHabito(int habitoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("habitos", new String[]{"nombre"}, "_id = ?", new String[]{String.valueOf(habitoId)}, null, null, null);
        String nombre = "Actividad Física";
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex("nombre");
            if (idx != -1) nombre = cursor.getString(idx);
            cursor.close();
        }
        return nombre;
    }

    public List<EstadoDiaHabito> obtenerHistorialDinamicoHabito(int habitoId) {
        List<EstadoDiaHabito> listaDias = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String queryHabito = "SELECT fecha_creacion, dias FROM habitos WHERE _id = ?";
        Cursor cursorHabito = db.rawQuery(queryHabito, new String[]{String.valueOf(habitoId)});

        String fechaCreacionStr = "2026-01-01";
        int totalDiasMeta = 7;

        if (cursorHabito != null && cursorHabito.moveToFirst()) {
            int idxFecha = cursorHabito.getColumnIndex("fecha_creacion");
            int idxDias = cursorHabito.getColumnIndex("dias");
            if (idxFecha != -1 && cursorHabito.getString(idxFecha) != null) {
                fechaCreacionStr = cursorHabito.getString(idxFecha);
            }
            if (idxDias != -1) {
                totalDiasMeta = cursorHabito.getInt(idxDias);
            }
            cursorHabito.close();
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            Date fechaInicio = sdf.parse(fechaCreacionStr);
            if (fechaInicio != null) {
                calendar.setTime(fechaInicio);

                for (int i = 0; i < totalDiasMeta; i++) {
                    String fechaActualStr = sdf.format(calendar.getTime());

                    boolean completado = false;
                    float distancia = 0;
                    long tiempoSegundos = 0;

                    String queryRegistro = "SELECT completado, distancia, tiempo_segundos FROM registros WHERE habito_id = ? AND fecha = ?";
                    Cursor cursorReg = db.rawQuery(queryRegistro, new String[]{String.valueOf(habitoId), fechaActualStr});
                    if (cursorReg != null && cursorReg.moveToFirst()) {
                        int idxComp = cursorReg.getColumnIndex("completado");
                        int idxDist = cursorReg.getColumnIndex("distancia");
                        int idxTiempo = cursorReg.getColumnIndex("tiempo_segundos");

                        if (idxComp != -1) {
                            completado = cursorReg.getInt(idxComp) == 1;
                        }
                        if (idxDist != -1) {
                            distancia = cursorReg.getFloat(idxDist);
                        }
                        if (idxTiempo != -1) {
                            tiempoSegundos = cursorReg.getLong(idxTiempo);
                        }
                        cursorReg.close();
                    }

                    listaDias.add(new EstadoDiaHabito(fechaActualStr, completado, distancia, tiempoSegundos));
                    calendar.add(Calendar.DATE, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return listaDias;
    }

    public Cursor obtenerCategorias() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.CategoriaEntry.TABLE_NAME, null, null, null, null, null, null);
    }

    public Cursor obtenerHabitosCompletadosPorFecha(String fecha) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h." + HabitContract.HabitEntry.COLUMN_NOMBRE + ", " +
                "h." + HabitContract.HabitEntry.COLUMN_CATEGORIA_ID + ", " +
                "h." + HabitContract.HabitEntry.COLUMN_COLOR + ", " +
                "r.distancia, r.tiempo_segundos " +
                "FROM " + HabitContract.HabitEntry.TABLE_NAME + " h " +
                "INNER JOIN " + HabitContract.RegistroEntry.TABLE_NAME + " r " +
                "ON h._id = r." + HabitContract.RegistroEntry.COLUMN_HABITO_ID + " " +
                "WHERE r." + HabitContract.RegistroEntry.COLUMN_FECHA + " = ? " +
                "AND r." + HabitContract.RegistroEntry.COLUMN_COMPLETADO + " = 1";
        return db.rawQuery(query, new String[]{fecha});
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

    public int calcularRachaHabito(int habitoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int racha = 0;

        String query = "SELECT " + HabitContract.RegistroEntry.COLUMN_FECHA +
                " FROM " + HabitContract.RegistroEntry.TABLE_NAME +
                " WHERE " + HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ? AND " +
                HabitContract.RegistroEntry.COLUMN_COMPLETADO + " = 1 " +
                " ORDER BY " + HabitContract.RegistroEntry.COLUMN_FECHA + " DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(habitoId)});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String fechaStr = cursor.getString(0);
                    Date fechaAnterior = sdf.parse(fechaStr);

                    racha = 1;

                    while (cursor.moveToNext()) {
                        String fechaActualStr = cursor.getString(0);
                        Date fechaActual = sdf.parse(fechaActualStr);

                        if (fechaAnterior != null && fechaActual != null) {
                            long diferenciaMillis = fechaAnterior.getTime() - fechaActual.getTime();
                            long diasDiferencia = diferenciaMillis / (1000 * 60 * 60 * 24);

                            if (diasDiferencia == 1) {
                                racha++;
                                fechaAnterior = fechaActual;
                            } else if (diasDiferencia == 0) {
                                continue;
                            } else {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        db.close();
        return racha;
    }
}
