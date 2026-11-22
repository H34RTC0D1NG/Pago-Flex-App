# PagoFlex 

App Android (Kotlin + Jetpack Compose) para el caso **PagoFlex** de DSY1105 – Desarrollo de Aplicaciones Móviles.
Reúne en un solo lugar los compromisos de pago de una persona (arriendo, crédito, cuota de club, aporte a fundación),
le recuerda los vencimientos, le deja pagar (pago simulado) y le entrega comprobantes, historial y logros.

## Integrantes

| Integrante | GitHub | Rol en el equipo |
|---|---|---|
| Gonzalo Gallardo | [GonzaloGallardo](https://github.com/Stiven63) | Desarrollador Frontend (UI/UX) |
| Isam Mejias | [H34RTHC0D1NG](https://github.com/H34RTC0D1NG) | Desarrollador Backend |

Tablero Trello : [Caso Pago Flex App - Trello](https://trello.com/b/G03wMvG7/app-pago-flex-caso-semestral).

## Cuentas de prueba

La primera vez que se abre la app se crea la base de datos con estos usuarios:

| Rol | RUT | Contraseña |
|---|---|---|
| Cliente | 15.834.207-3 | `Camila#2026` |
| Ejecutivo (admin) | 12.345.678-5 | `Admin#2026` |

También puedes crear tu propia cuenta desde **Crear cuenta**.

## Qué tiene esta entrega

| Pedido de la retroalimentación | Cómo quedó |
|---|---|
| Inicio de sesión real | Valida RUT + contraseña contra los usuarios guardados en Room. Mensajes distintos si el RUT no existe o la clave está mala, bloqueo de 30 s tras 3 intentos, enlace a registro y **recuperar contraseña** (verifica RUT + correo y deja crear una clave nueva). |
| Registro propio | Se eliminó el formulario de clase ("nivel de experiencia"). Pide RUT, nombre, correo, teléfono, contraseña y repetir contraseña, con validación en vivo en cada campo (✓ verde / error rojo), lista de requisitos de clave segura (8+ caracteres, mayúscula, minúscula, número y carácter especial) y botón **Volver**. |
| Gestión de cuenta | Perfil → **Editar mis datos** (nombre, correo, teléfono) y **Cambiar contraseña** (pide la actual). |
| Roles | `CLIENTE` y `ADMIN` (ejecutivo PagoFlex). El ejecutivo tiene su propio panel: agrega/activa/desactiva organizaciones, carga compromisos a clientes, anula cobros y resuelve reportes. |
| Coherencia de datos | Se usa la fecha real del teléfono (antes estaba fija). Los datos de prueba se generan alrededor de hoy, cada comprobante apunta a un compromiso que existe y el indicador de cumplimiento se calcula con los pagos reales (antes usaba números fijos). |
| Persistencia | Room: 6 entidades, 6 DAO, `AppDatabase` y `RepositorioPagoFlex`. Usuarios, organizaciones, compromisos, comprobantes, reportes y recomendaciones quedan guardados. SharedPreferences solo para ajustes chicos. |
| Recursos nativos | **Notificaciones** de vencimiento (permiso `POST_NOTIFICATIONS`, respeta el horario 22:00–08:00) y **cámara** para la foto de perfil (permiso `CAMERA`). Si se rechaza un permiso, la app lo explica y ofrece abrir Ajustes. Además: vibración y compartir comprobante. |
| Diseño e identidad | Logo propio, ícono de la app, paleta PagoFlex (verde, azul marino y ámbar), modo oscuro y alto contraste. El proyecto se renombró de `miregistro` a `cl.duocuc.pagoflex`. |

## Arquitectura

MVVM con una sola Activity y navegación por estado:

```
UI (Compose)  ->  PagoFlexViewModel (StateFlow<PagoFlexUiState>)  ->  RepositorioPagoFlex  ->  DAO  ->  Room (pagoflex.db)
```

```
app/src/main/java/cl/duocuc/pagoflex/
├── data/
│   ├── entity/        UsuarioEntity, OrganizacionEntity, CompromisoEntity, ComprobanteEntity, ReporteEntity, RecomendacionEntity
│   ├── dao/           un DAO por entidad
│   ├── AppDatabase.kt
│   ├── DatosIniciales.kt   datos de prueba (se cargan una sola vez)
│   ├── RepositorioPagoFlex.kt
│   └── AlmacenLocal.kt     SharedPreferences (ajustes)
├── model/             modelos de dominio, Compromiso (herencia por tipo), Fecha, UiState
├── nativo/            Notificador, Camara, Vibrador, Compartir, Permisos
├── ui/                components, screens/pagoflex, styles, theme
├── util/              Rut, Validaciones, Seguridad (hash SHA-256)
└── viewmodel/         PagoFlexViewModel
```

La contraseña nunca se guarda como texto: se guarda `SHA-256(rut + ":" + clave)`.

## Cómo ejecutar

1. Abrir la carpeta en Android Studio y esperar el *Gradle Sync*.
2. Ejecutar en un emulador o teléfono con Android 7.0 (API 24) o superior.
3. Para probar la cámara en el emulador, usar uno con cámara virtual.
4. Pruebas unitarias: `app/src/test/.../ValidacionesTest.kt` (RUT, clave segura, correo, teléfono y fechas).

> Si Gradle avisa que no encuentra la versión de KSP, cambia `ksp` en `gradle/libs.versions.toml` por la que sugiera Android Studio. Porque por lo menos a mi me dio error y esa es la solución que por lo menos a mi me funciono, no vaya ser Ejem Ejem.

## Historial de cambios (fases)

Cada fase es un commit:

1. Renombre del proyecto a PagoFlex
2. Logo, paleta propia y modo oscuro
3. Migración a Room (entidades, DAO, repositorio)
4. Login real, recuperar contraseña y enlace a registro
5. Registro propio de PagoFlex
6. Editar perfil y cambiar contraseña
7. Roles cliente / ejecutivo
8. Coherencia de datos
9. Notificaciones con permiso
10. Cámara para foto de perfil
11. README, Trello y pruebas
