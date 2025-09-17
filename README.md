# 🚕 Gestión Taxi - Group Fasata

**Versión 2.0** - Solución profesional para taxistas

Una aplicación móvil completa para la gestión integral de servicios de taxi, desarrollada con tecnologías modernas de Android.

## 📱 Características Principales

### 🚗 Gestión de Carreras
- Registro detallado de carreras de taxi
- Cálculo automático de tarifas
- Múltiples métodos de pago
- Historial completo de servicios

### 💰 Control Financiero
- Seguimiento de ingresos diarios/mensuales
- Gestión de gastos operativos
- Generación de reportes financieros
- Estadísticas detalladas de rendimiento

### 📊 Análisis y Reportes
- Dashboard con métricas en tiempo real
- Gráficos de ingresos y gastos
- Exportación de datos en PDF
- Análisis por períodos personalizados

### 🧾 Facturación
- Generación automática de facturas
- Gestión de datos de facturación
- Exportación y compartir documentos
- Cumplimiento fiscal

### 💾 Respaldo de Datos
- Copias de seguridad locales
- Exportación de datos
- Restauración de datos
- Gestión de archivos

## 🛠️ Tecnologías Utilizadas

### Frontend
- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI moderna y declarativa
- **Material Design 3** - Diseño consistente y elegante
- **Navigation Compose** - Navegación fluida

### Backend y Datos
- **Room Database** - Base de datos local SQLite
- **DataStore** - Almacenamiento de preferencias
- **Coroutines & Flow** - Programación asíncrona

### Funcionalidades Adicionales
- **iText PDF** - Generación de documentos PDF
- **MVVM Architecture** - Arquitectura limpia y mantenible
- **Repository Pattern** - Abstracción de datos
- **Dependency Injection** - Gestión de dependencias

## 📋 Requisitos del Sistema

- **Android 7.0** (API nivel 24) o superior
- **2 GB RAM** mínimo recomendado
- **100 MB** de espacio libre
- **Conexión a Internet** para sincronización (opcional)

## 🚀 Instalación

### Para Desarrolladores

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tu-usuario/taxi-app.git
   cd taxi-app
   ```

2. **Configurar el proyecto**
   - Abrir en Android Studio
   - Sincronizar dependencias

3. **Compilar y ejecutar**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### Para Usuarios

- Descargar desde Google Play Store (próximamente)
- O instalar APK desde [Releases](https://github.com/tu-usuario/taxi-app/releases)

## 📖 Guía de Uso

### Primer Uso
1. **Métodos de pago**: Configurar formas de cobro disponibles
2. **Tarifas**: Definir precios base y recargos

### Registro de Carreras
1. Abrir la app y tocar "Nueva Carrera"
2. Ingresar origen y destino
3. Seleccionar método de pago
4. Confirmar tarifa y guardar

### Gestión de Gastos
1. Ir a "Gastos" desde el menú principal
2. Tocar "+" para agregar nuevo gasto
3. Categorizar y describir el gasto
4. Guardar con fecha y monto

### Reportes y Estadísticas
1. Acceder a "Estadísticas" desde el menú
2. Seleccionar período de análisis
3. Revisar gráficos y métricas
4. Exportar reportes si es necesario

## 🏗️ Arquitectura del Proyecto

```
app/
├── src/main/java/com/moham/taxi/
│   ├── data/
│   │   ├── database/          # Room Database
│   │   ├── model/             # Modelos de datos
│   │   └── repository/        # Repositorios
│   ├── ui/
│   │   ├── components/        # Componentes reutilizables
│   │   ├── navigation/        # Navegación
│   │   ├── screens/           # Pantallas de la app
│   │   ├── theme/             # Tema y estilos
│   │   └── viewmodel/         # ViewModels
│   ├── utils/                 # Utilidades
│   ├── services/              # Servicios
│   └── GestionTaxiApplication.kt
└── build.gradle.kts
```

## 🔧 Configuración de Desarrollo

### Variables de Entorno
Crear archivo `local.properties`:
```properties
sdk.dir=/path/to/android/sdk
```

### Dependencias Principales
```kotlin
// UI y Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Base de datos
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Todas las dependencias están incluidas en build.gradle.kts

// PDF
implementation("com.itextpdf:itext7-core:7.2.5")
```

## 🧪 Testing

```bash
# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests de instrumentación
./gradlew connectedAndroidTest

# Generar reporte de cobertura
./gradlew jacocoTestReport
```

## 📦 Build y Release

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Generar APK firmado
1. Configurar keystore en `app/build.gradle.kts`
2. Ejecutar: `./gradlew assembleRelease`

## 🤝 Contribución

1. **Fork** el proyecto
2. Crear **feature branch** (`git checkout -b feature/nueva-funcionalidad`)
3. **Commit** cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. **Push** a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear **Pull Request**

### Estándares de Código
- Seguir [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Usar nombres descriptivos para variables y funciones
- Documentar funciones públicas
- Mantener funciones pequeñas y enfocadas

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Equipo de Desarrollo

**Group Fasata** - Soluciones tecnológicas profesionales

- **Desarrollador Principal**: Mohammed Lahrouri Laasiri 
- **UI/UX Design**: Mohammed Lahrouri Laasiri 
- **QA Testing**: Grupo Fasata

## 🔄 Changelog

### Versión 2.0 (Actual)
- ✨ Nueva interfaz con Material Design 3
- 🚀 Mejoras de rendimiento significativas
- 📊 Dashboard rediseñado con métricas avanzadas
- 🧾 Sistema de facturación mejorado
- 💾 Respaldo local de datos
- 🔒 Mejoras de seguridad y privacidad

### Versión 1.20
- 🐛 Corrección de errores menores
- 📱 Optimizaciones de UI
- 🔧 Mejoras de estabilidad

## 🎯 Roadmap

### Próximas Funcionalidades
- [ ] **Modo Offline Completo** - Funcionamiento sin conexión
- [ ] **Integración GPS** - Tracking automático de rutas
- [ ] **Notificaciones Push** - Alertas y recordatorios
- [ ] **Multi-idioma** - Soporte para múltiples idiomas
- [ ] **Tema Oscuro** - Modo nocturno
- [ ] **Backup Automático** - Respaldos programados
- [ ] **API REST** - Integración con sistemas externos
- [ ] **Versión Web** - Dashboard web para gestión

---

**¿Te gusta el proyecto?** ⭐ ¡Dale una estrella en GitHub!

**¿Encontraste un bug?** 🐛 [Reporta el issue](https://github.com/tu-usuario/taxi-app/issues/new)

**¿Tienes una idea?** 💡 [Comparte tu sugerencia](https://github.com/tu-usuario/taxi-app/discussions)

---

*Desarrollado con ❤️ por Group Fasata*