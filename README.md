# Meteory Optimizer — Native Android App

**Package:** `com.meteory.optimizer`  
**Min SDK:** 29 (Android 10)  
**Target:** 35 (Android 15)  
**Stack:** Kotlin + Jetpack Compose + Material 3 + Hilt + Room + DataStore

---

## Compilar a APK

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requiere keystore configurado)
./gradlew assembleRelease
```

El APK generado estará en `app/build/outputs/apk/`

---

## Estructura del Proyecto

```
app/src/main/kotlin/com/meteory/optimizer/
├── MainActivity.kt               # Actividad principal (Hilt + Edge to Edge)
├── MeteoryApplication.kt         # Application class (Hilt + WorkManager + Channels)
├── navigation/
│   ├── NavRoutes.kt              # Rutas + iconos de pantallas
│   └── MeteoryNavGraph.kt        # NavHost con todas las pantallas
├── ui/theme/
│   ├── Color.kt                  # Paleta de 6 ramps (primary, accent, secondary, neutral, success, warning, error)
│   ├── Theme.kt                  # Material 3 dark/light theme + edge-to-edge
│   └── Type.kt                   # Tipografía completa (13 estilos)
├── ui/components/
│   ├── BottomNavBar.kt           # Nav inferior animada con indicador
│   ├── MetricCard.kt             # Tarjetas, GlowButton, LinearProgress, SectionHeader
│   ├── CircularChart.kt          # Gauge circular, Pie chart, SparkLine
│   └── HudContent.kt             # Contenido del HUD superpuesto
├── screens/
│   ├── home/HomeScreen.kt        # Dashboard + score + acceso rápido
│   ├── gaming/GamingScreen.kt    # Gaming Mode Brutal + perfiles por juego
│   ├── cleaning/CleaningScreen.kt # Limpieza + auto-clean + historial
│   ├── performance/PerformanceScreen.kt # CPU/RAM/Temp + perfiles
│   ├── battery/BatteryScreen.kt  # Batería + protección + historial
│   ├── privacy/PrivacyScreen.kt  # Escáner de permisos + bóveda
│   ├── network/NetworkScreen.kt  # Velocidad + optimización TCP
│   ├── ai/AiScreen.kt            # Recomendaciones IA en tiempo real
│   ├── tools/ToolsScreen.kt      # Diagnóstico + info dispositivo
│   └── assistant/AssistantScreen.kt # Meteory Asistente FPS (chat + clave)
├── viewmodels/                   # HomeVM, GamingVM, CleaningVM, PerformanceVM, BatteryVM, AssistantVM
├── data/
│   ├── AppDatabase.kt            # Room: GameProfile, SystemHealthLog, CleaningHistory, BatteryHistory
│   ├── DeviceProfiles.kt         # Perfiles: Samsung, Xiaomi, Honor, Motorola, Realme, Tecno, Infinix...
│   └── PreferencesManager.kt     # DataStore: todos los ajustes persistentes
├── services/
│   ├── HudOverlayService.kt      # Servicio HUD flotante (TYPE_APPLICATION_OVERLAY + Compose)
│   ├── GamingService.kt          # Monitor de juegos activos + apply/restore gaming mode
│   ├── MonitorService.kt         # Logging periódico de métricas del sistema
│   └── CleaningWorker.kt         # WorkManager: limpieza automática programada
├── utils/
│   ├── ShellUtils.kt             # Ejecución de comandos shell con coroutines
│   ├── SystemInfo.kt             # CPU/RAM/Temp/Batería/Red/Almacenamiento desde /proc y APIs Android
│   ├── AdbCommands.kt            # Comandos ADB/setprop/sysctl para CPU/GPU/red/DnD
│   ├── ShizukuUtils.kt           # Integración Shizuku para comandos privilegiados sin root
│   └── PermissionUtils.kt        # Verificación y apertura de settings de permisos especiales
├── di/AppModule.kt               # Hilt: Room DB + DAOs
└── receivers/BootReceiver.kt     # Auto-start MonitorService en boot
```

---

## Características Implementadas

### Modo Gaming Brutal
- Toggle principal que activa governor CPU `performance` en todos los núcleos
- GPU en modo `performance` (Qualcomm Adreno)
- Vulkan/OpenGL ES selectores + auto-test 30s comparativo
- Selector de Hz: 60/90/120/144 via SurfaceFlinger
- DnD automático al entrar al juego
- Perfiles guardados por juego (Room DB)

### HUD Superpuesto
- Servicio foreground con `TYPE_APPLICATION_OVERLAY`
- Arrrastrable libremente, opacidad ajustable
- Métricas: FPS*, CPU%, RAM libre, Temp°C, Ping*
- Botón ocultar/mostrar

### Limpieza
- Escaneo de caché de apps, temp, APKs en Descargas, thumbnails
- Limpieza selectiva por categoría
- Auto-limpieza configurable (70-95% de ocupación)
- Historial persistente en Room

### Rendimiento
- Gráficos sparkline CPU/RAM en tiempo real
- Perfiles: Equilibrado / Velocidad Máxima / Ahorro
- Kill de procesos en segundo plano
- Score del sistema 0-100 con acciones recomendadas

### Batería
- Nivel, salud, temperatura, voltaje, capacidad real
- Protección de carga (límite al 80% configurable)
- Modos: Equilibrado, Ahorro, Alto Ahorro, Ultra Ahorro
- Historial gráfico de nivel

### Meteory Asistente FPS
- Clave de activación: `AQ.Ab8RN6ImI_JcS20AEsH768nBz2Vjt2G5Qlcvpw5fLr6ZqV9fSg`
- Chat conversacional, solo responde sobre optimización
- Carga perfil específico por marca/modelo (20+ modelos)
- Auto-aplica configuración vía Shizuku/ADB

### Shizuku
- Detección automática + banner en pantalla principal
- Todos los comandos privilegiados van vía Shizuku si disponible, sino vía shell normal
- Sin root requerido

---

## Dependencias Principales

| Librería | Versión |
|---|---|
| Compose BOM | 2024.10.01 |
| Material 3 | BOM |
| Navigation Compose | 2.8.4 |
| Hilt | 2.52 |
| Room | 2.6.1 |
| DataStore | 1.1.1 |
| WorkManager | 2.9.1 |
| Shizuku | 13.1.5 |
| Accompanist | 0.36.0 |

---

## Notas de Compilación

1. Android Studio Hedgehog+ o Gradle CLI con JDK 17
2. Shizuku: la app requiere que el usuario active Shizuku aparte para funciones avanzadas
3. HUD: requiere permiso `ACTION_MANAGE_OVERLAY_PERMISSION` (se solicita automáticamente)
4. Uso de stats: requiere `ACTION_USAGE_ACCESS_SETTINGS` (se solicita automáticamente)
5. Sin anuncios, sin rastreo, sin servicios externos — 100% offline
