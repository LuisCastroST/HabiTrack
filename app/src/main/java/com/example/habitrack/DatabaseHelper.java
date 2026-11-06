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

/**
 * Este databasehelper es el administrador principal de la base de datos en SQ lite local para la
 * aplicacion. Ayuda con el gestionamiento del diclo de vida de la bd, crea tablas relacionals,
 * ayuda con consultas CRUD y es el encargado de prosesar historiales y rachas de habitos.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "habitrack.db";
    private static final int DATABASE_VERSION = 10;

    /**
     * Constructor del helper de la base de datos.
     *
     * @param context, El contexto de la aplicación desde donde se invoca.
     */

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Ejecuta la creacion inciald de las tablas en la base de datos e inserta todos los registros por defecto.
     * @param db Es la Instancia de la base de datos SQLlite en modo escritura.
     */

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

    /**
     * Inserta un habito basico con valores predeterminados.
     *
     * @param nombre Nombre descriptivo del habito.
     * @return long ID de la fila insertada o -1 en caso de error.
     */


    public long agregarHabito(String nombre) {
        return insertarHabito(nombre, 5, 30, "#4CAF50");
    }

    /**
     * Inserta un nuevo recordatorio asociado a un habito en especifico.
     *
     * @param habitoId ID del hábito al que le pertenece la alarma.
     * @param hora     Hora programada.
     * @param minuto   Minuto programado de la alarma.
     * @return long ID del recordatorio creado o -1 si falla.
     */

    public long insertarRecordatorio(int habitoId, int hora, int minuto) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.RecordatorioEntry.COLUMN_HABITO_ID, habitoId);
        values.put(HabitContract.RecordatorioEntry.COLUMN_HORA, hora);
        values.put(HabitContract.RecordatorioEntry.COLUMN_MINUTO, minuto);
        return db.insert(HabitContract.RecordatorioEntry.TABLE_NAME, null, values);
    }

    /**
     * Inserta un nuevo habito compleco con categoria, duracion y color personalizado.
     *
     * @param nombre      Nombre o titulo del habito.
     * @param categoriaId ID de la categoria asociada.
     * @param dias        Cantidad total de dias de la meta.
     * @param color       Codigo hexadecimal del color en la interfaz.
     * @return long ID del nuevo registro creado o -1 si ocurre un error.
     * @throws SQLException Si ocurre un fallo en la ejecución de la sentencia SQL de insercion.
     */

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

    /**
     * Obtiene un cursor con todos los habitos registrados y sus respectivas categorias mediante un join.
     *
     * @return Cursor con los resultados de la consulta de habitos.
     */

    public Cursor obtenerTodosLosHabitos() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT h.*, c." + HabitContract.CategoriaEntry.COLUMN_NOMBRE + " AS categoria_nombre"
                + " FROM " + HabitContract.HabitEntry.TABLE_NAME + " h"
                + " LEFT JOIN " + HabitContract.CategoriaEntry.TABLE_NAME + " c"
                + " ON h." + HabitContract.HabitEntry.COLUMN_CATEGORIA_ID + " = c." + HabitContract.CategoriaEntry._ID;
        return db.rawQuery(query, null);
    }

    /**
     * Consulta los recordatorios asociados a un habite en particular.
     *
     * @param habitoId ID del habito a consultar.
     * @return Cursor con los recordatorios encontrados.
     */

    public Cursor obtenerRecordatoriosPorHabito(int habitoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.RecordatorioEntry.TABLE_NAME, null,
                HabitContract.RecordatorioEntry.COLUMN_HABITO_ID + " = ?",
                new String[]{String.valueOf(habitoId)}, null, null, null);
    }

    /**
     * Obtiene los habitos marcados para ser mostrados en el panel principal.
     *
     * @return Cursor con los habitos del dashboard.
     */

    public Cursor obtenerHabitosDashboard() {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = HabitContract.HabitEntry.COLUMN_EN_DASHBOARD + " = ?";
        String[] selectionArgs = new String[]{"1"};
        return db.query(HabitContract.HabitEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
    }

    /**
     * Actualiza la visibilidad de un habito en el panel principal.
     *
     * @param id          ID del habito.
     * @param enDashboard Estado booleano de visibilidad.
     */

    public void actualizarVisibilidadDashboard(int id, boolean enDashboard) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HabitContract.HabitEntry.COLUMN_EN_DASHBOARD, enDashboard ? 1 : 0);
        db.update(HabitContract.HabitEntry.TABLE_NAME, values, HabitContract.HabitEntry._ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Actualiza las coordenadas geograficas del apartado de habitos fisicos.
     *
     * @param idHabito ID del habito fisico.
     * @param latitud  Latitud GPS obtenida.
     * @param longitud Longitud GPS obtenida.
     * @return boolean True si se actualizo al menos una fila, false en el caso contrario.
     */

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
        return filasAfectadas > 0;
    }

    /**
     * Guarda el estado de cumplimiento de un habito en una fecha especificada.
     *
     28      * @param habitoId   ID del habito.
     * @param fecha      Fecha en formato yyyy-MM-dd.
     * @param completado Estado de cumplimiento.
     */

    public void guardarEstadoHabitoFecha(int habitoId, String fecha, boolean completado) {
        guardarEstadoHabitoFechaConDetalle(habitoId, fecha, completado, 0.0f, 0L);
    }

    /**
     * Guarda el estado detallado de un habito en una fecha, incluyendo distancia y tiempo en caso de una actividad fisica.
     *
     * @param habitoId       ID del habito.
     * @param fecha          Fecha en formato yyyy-MM-dd.
     * @param completado     Estado de cumplimiento.
     * @param distancia      Distancia recorrida en metros.
     * @param tiempoSegundos Duracion total de la actividad en segundos.
     */

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

    /**
     * Verifica si un habito se euncuentra completado en la fecha dada.
     *
     * @param habitoId ID del habito.
     * @param fecha    Fecha a consultar.
     * @return boolean True si esta completado, false en el caso contrario.
     */

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

    /**
     * Obtiene el nombre de un habito a partir de su ID.
     *
     * @param habitoId ID del habito.
     * @return String con el nombre del habito o valor por defecto.
     */

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

    /**
     * Genera la lista historica dinamica de cumplimientos para un habito, considerando su fecha de creacion y meta de dias.
     *
     * @param habitoId ID del habito.
     * @return Lista de objetos {@link EstadoDiaHabito} con el desglose diario.
     */

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

    /**
     * Obtiene todas las categorias registradas en el base de datos.
     *
     * @return Cursor con las categorias.
     */
    public Cursor obtenerCategorias() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(HabitContract.CategoriaEntry.TABLE_NAME, null, null, null, null, null, null);
    }

    /**
     * Obtiene los habitos que se han completado en una fecha especifica junto con sus detalles fisicos.
     *
     * @param fecha Fecha a consultar en formato yyyy-MM-dd.
     * @return Cursor con los habitos completados.
     */

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

    /**
     * Elimina un habito seleccinado con todos sus registros historicos asociados de forma en cascada.
     *
     * @param id ID del habito a eliminar.
     */

    public void eliminarHabito(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.HabitEntry.TABLE_NAME, HabitContract.HabitEntry._ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(HabitContract.RegistroEntry.TABLE_NAME, HabitContract.RegistroEntry.COLUMN_HABITO_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Limpia y reinicia todos los datos de registros y habitos de la aplicacion.
     */

    public void reiniciarTodo() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HabitContract.RegistroEntry.TABLE_NAME, null, null);
        db.delete(HabitContract.HabitEntry.TABLE_NAME, null, null);
    }

    /**
     * Calcula la racha consecutiva actual del cumplimiento para un habito.
     *
     * @param habitoId ID del habito.
     * @return int Numero de dias consecutivos cumplidos.
     */

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
        return racha;
    }
}
