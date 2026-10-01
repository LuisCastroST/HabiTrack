package com.example.habitrack;

import android.provider.BaseColumns;

public final class HabitContract {

    private HabitContract() {}

    public static class HabitEntry implements BaseColumns {
        public static final String TABLE_NAME = "habitos";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_CATEGORIA = "categoria";
        public static final String COLUMN_DIAS = "dias";
        public static final String COLUMN_COLOR = "color";
        public static final String COLUMN_EN_DASHBOARD = "en_dashboard";
    }

    public static class RegistroEntry implements BaseColumns {
        public static final String TABLE_NAME = "registros";
        public static final String COLUMN_HABITO_ID = "habito_id";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_COMPLETADO = "completado";
    }
}