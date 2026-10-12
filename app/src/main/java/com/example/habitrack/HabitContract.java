package com.example.habitrack;

import android.provider.BaseColumns;

public final class HabitContract {

    private HabitContract() {}

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

    public static class RegistroEntry implements BaseColumns {
        public static final String TABLE_NAME = "registros";
        public static final String COLUMN_HABITO_ID = "habito_id";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_COMPLETADO = "completado";
    }

    public static class CategoriaEntry implements BaseColumns {
        public static final String TABLE_NAME = "categorias";
        public static final String COLUMN_NOMBRE = "nombre";
    }

    public static class RecordatorioEntry implements BaseColumns {
        public static final String TABLE_NAME = "recordatorios";
        public static final String COLUMN_HABITO_ID = "habito_id";
        public static final String COLUMN_HORA = "hora";
        public static final String COLUMN_MINUTO = "minuto";
    }
}
