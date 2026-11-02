package com.example.habitrack;

import android.provider.BaseColumns;

/**
 * Clase de contrato que define el esquema relacional de la base de datos SQLite.
 * Centraliza los nombres de tablas y constantes de columnas para evitar cadenas duras
 * y desacoplar la persistencia.
 */

public final class HabitContract {

    /**
     * Constructor privado para prevenir la instanciacion accidental de la clase contrato.
     */

    private HabitContract() {}

    /**
     * Estructura y definicion de columnas para la tabla de habitos.
     */

    public static class HabitEntry implements BaseColumns {
        public static final String TABLE_NAME = "habitos";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_DIAS = "dias";
        public static final String COLUMN_COLOR = "color";

        public static final String COLUMN_CATEGORIA_ID = "categoria_id";
        public static final String COLUMN_EN_DASHBOARD = "en_dashboard";
        public static final String COLUMN_LATITUD = "latitud";
        public static final String COLUMN_LONGITUD = "longitud";
    }

    /**
     * Estructura y definicion de columnas para la tabla de registros diarios e historicos.
     */

    public static class RegistroEntry implements BaseColumns {
        public static final String TABLE_NAME = "registros";
        public static final String COLUMN_HABITO_ID = "habito_id";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_COMPLETADO = "completado";
    }

    /**
     * Estructura y definicion de columnas para la tabla de categorias de habitos.
     */

    public static class CategoriaEntry implements BaseColumns {
        public static final String TABLE_NAME = "categorias";
        public static final String COLUMN_NOMBRE = "nombre";
    }

    /**
     * Estructura y definicion de columnas para la tabla de recordatorios y alarmas.
     */

    public static class RecordatorioEntry implements BaseColumns {
        public static final String TABLE_NAME = "recordatorios";
        public static final String COLUMN_HABITO_ID = "habito_id";
        public static final String COLUMN_HORA = "hora";
        public static final String COLUMN_MINUTO = "minuto";
    }
}
