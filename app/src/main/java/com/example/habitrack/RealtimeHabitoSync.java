package com.example.habitrack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gestiona la sincronizaciOn de habitos en Firebase Realtime Database para generar
 * identificadores unicos y para escuchar cambios en tiempo real.
 */
public class RealtimeHabitoSync {

    private static final String NODO_USUARIOS = "usuarios";
    private static final String NODO_HABITOS = "habitos";

    private final FirebaseAuth auth;
    private final DatabaseReference usuariosRef;
    private final Map<ValueEventListener, DatabaseReference> listenersActivos = new WeakHashMap<>();

    /**
     * Inicializa las instancias de @link FirebaseAuth y @link DatabaseReference
     * al nodo raiz de usuarios en Realtime Database.
     */
    public RealtimeHabitoSync() {
        this.auth = FirebaseAuth.getInstance();
        this.usuariosRef = FirebaseDatabase.getInstance().getReference(NODO_USUARIOS);
    }

    /**
     * Obtiene la referencia a los habitos del usuario que esta actualmente autenticado.
     *
     * @return Referencia a usuariosId y habitosId, da null si no hay sesion activa.
     */
    @Nullable
    private DatabaseReference obtenerReferenciaHabitosUsuario() {
        FirebaseUser usuarioActual = auth.getCurrentUser();
        if (usuarioActual == null) {
            return null;
        }
        return usuariosRef.child(usuarioActual.getUid()).child(NODO_HABITOS);
    }

    /**
     * Guarda un nuevo habito en Realtime Database generando una clave unica.
     *
     * @param nombre        Nombre o descripcion del habito.
     * @param frecuencia    Frecuencia configurada para el habito.
     * @param completadoHoy Indica si el habito esta marcado como completado en el dia actual.
     * @return Clave generada en Realtime Database si se inicio la escritura, o null si fallo.
     */
    @Nullable
    public String guardarHabito(@NonNull String nombre, @NonNull String frecuencia, boolean completadoHoy) {
        DatabaseReference habitosRef = obtenerReferenciaHabitosUsuario();
        if (habitosRef == null) {
            return null;
        }

        String claveHabito = habitosRef.push().getKey();
        if (claveHabito == null) {
            return null;
        }

        Map<String, Object> datosHabito = new HashMap<>();
        datosHabito.put("id", claveHabito);
        datosHabito.put("nombre", nombre);
        datosHabito.put("frecuencia", frecuencia);
        datosHabito.put("completadoHoy", completadoHoy);
        datosHabito.put("timestamp", System.currentTimeMillis());

        habitosRef.child(claveHabito).setValue(datosHabito);
        return claveHabito;
    }

    /**
     * Registra un @link ValueEventListener para recibir actualizaciones en tiempo real
     * sobre los habitos del usuario autenticado.
     *
     * @param callback Receptor de eventos de actualizacion o error.
     * @return Instancia del @link ValueEventListener registrado, o null si no hay sesion activa.
     */
    @Nullable
    public ValueEventListener escucharHabitos(@NonNull HabitosSyncCallback callback) {
        DatabaseReference habitosRef = obtenerReferenciaHabitosUsuario();
        if (habitosRef == null) {
            callback.onError("Usuario no autenticado.");
            return null;
        }

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int cantidad = (int) snapshot.getChildrenCount();
                callback.onHabitosSincronizados(cantidad, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String detalle = error.getMessage();
                callback.onError(detalle != null ? detalle : "Lectura cancelada.");
            }
        };

        habitosRef.addValueEventListener(listener);
        listenersActivos.put(listener, habitosRef);
        return listener;
    }

    /**
     * Elimina un @link ValueEventListener previamente registrado para evitar fugas de memoria
     * o callbacks cuando la actividad deja de estar visible.
     *
     * @param listener listener para borrar; si es null, no realiza ninguna accion.
     */
    public void detenerEscucha(@Nullable ValueEventListener listener) {
        if (listener == null) {
            return;
        }
        DatabaseReference refRegistrada = listenersActivos.remove(listener);
        if (refRegistrada != null) {
            refRegistrada.removeEventListener(listener);
        }
    }

    /**
     * Interfaz de callback para notificar cambios en tiempo real o errores de lectura.
     */
    public interface HabitosSyncCallback {

        /**
         * Se invoca cada vez que cambian los datos en la rama de habitos del usuario.
         *
         * @param totalHabitos Cantidad total de nodos de habitos encontrados.
         * @param snapshot     Instantanea completa de los datos recibidos.
         */
        void onHabitosSincronizados(int totalHabitos, @NonNull DataSnapshot snapshot);

        /**
         * Se invoca cuando ocurre un error o se cancela la lectura en Realtime Database.
         *
         * @param mensajeError Descripción del error reportado.
         */
        void onError(@NonNull String mensajeError);
    }
}