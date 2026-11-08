HabitTrack

Aplicacion movil nativa para Android diseñada para el seguimiento y registro diario de habitos personales, conteo de rachas, gestion de recordatorioss y control de progreso mediante persistencia local segura y autentificacion en la nube.

Herramientas y tecnologias usadas:

![Badge de Android](https://img.shields.io/badge/Platform-Android-green)
![Java](https://img.shields.io/badge/Language-Java-orange)
![SQLite](https://img.shields.io/badge/Database-SQLite-blue)
![Firebase Auth](https://img.shields.io/badge/Backend-Firebase-yellow)
![Gradle](https://img.shields.io/badge/Gradle-Build-blueviolet)

=============================================================

Caracteristicas principales

Autenticacion segura

Cuenta con un sistema de inicio de seion y registro de usuarios integrado con Firebas Authentication

Persistencia local

Base de datos SQLite optimizda con consultas parametrizadas para la prevencion estrictas de inyecciones SQL

Seguimiento y rachas

Panel principal interactivo con calculo automatico de rachas consecutivas y barra de progreso dinamica por cumplimiento diario

Recordatorios

Sistema de notificaciones automaticas programadas mediante AlarmManager y BroadcastReciever

Accesibilidad y Personalizacion

Soporte para modo oscuro

=============================================================

Arquitectura y tecnologias

Este projecto sigue las guias de desarrollo nativo en Android y utiliza las siguientes herramientes y librerias:

Lenguaje: Java, HTML5, CSS3, JavaScript

Interfaz de usuario: XML Layouts nativos Y WebView para visualizacion de mapas de web interactivos.

Mapas y geolocalizacion: Leaflet.js integrado mediante WeViews y JavaScript Bridge.

Persistencia local: SQLite con contrato estructurado.

Autentificacion: Firebas Authentication

Gestion de alarmas y servicios: AlarmManager SDK y servicios de rastreo.

Control de versiones: Git y GitHub

Como instalar

1. Clonar el repositorio en GitHub: https://github.com/LuisCastroST/HabiTrack

2. Abrir projecto en Android Studio.

3. Selecciona "Open an Existing Project" y busca la carpeta donde se clono el repositorio.

4. Esperar que Gradle sincronice todas las dependencias del projecto.

5. Configurar Firebase asegurando de incluir el archivo de configuracion de google-services.json dentro de la carpeta app del projecto.

6. Ejecutar la app.

7. Conectar un dispositivo fisico por USB o usar un emulador de Android con API nivel 24 o superior.

=============================================================

Estructura del projecto

app/
├── src/
│   ├── main/
│   │   ├── java/com/example/habitrack/
│   │   │   ├── MainActivity.java             Panel principal y menu superior
│   │   │   ├── Pantalladeinicio.java         Pantalla de bienvenida inicial
│   │   │   ├── PantallaLogin.java            Inicio de sesion con Firebase Authentication
│   │   │   ├── PantallaRegistro.java         Registro de nuevos usuarios
│   │   │   ├── GestionarHabitos.java         Modulo de creacion y administracion de habitos
│   │   │   ├── PantallaConfiguracion.java    Gestion de configuraciones, accesibilidad, cuenta y permisos.
│   │   │   ├── CalendarioActivity.java       Vista detallada de cumplimiento historico
│   │   │   ├── CalendarioAdapter.java        Adaptador para el grid/lista del calendario
│   │   │   ├── PantallaActividadFisica.java  Gestion de actividad fisica y mapas
│   │   │   ├── ServicioSeguimiento.java      Servicio en segundo plano para rastreo GPS
│   │   │   ├── DatabaseHelper.java           Capa de persistencia SQLite segura
│   │   │   ├── HabitContract.java            Definicion de tablas y columnas
│   │   │   ├── EstadoDiaHabito.java          Modelo de datos para estados diarios
│   │   │   └── NotificationReceiver.java     Receptor de alarmas diarias
│   │   └── res/                              Recursos de la app
│   │       ├── layout/                       Interfaces visuales XML
│   │       ├── menu/                         Menus de la barra superior
│   │       ├── assets/                       Archivos estáticos como mapa.html de Leaflet
│   │       └── drawable/                     Iconos
└── build.gradle.kts                          Configuracion de dependencias de Gradle

=============================================================

Licencia

Este projecto esta bajo la licencia de MIT.




