# Conecta Ciudad - Plataforma de Participación Ciudadana

## Tabla de Contenidos
1. [Descripción General](#descripción-general)
2. [Roles del Sistema](#roles-del-sistema)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Entidades Principales](#entidades-principales)
5. [Endpoints de la API](#endpoints-de-la-api)
6. [Seguridad](#seguridad)
7. [Flujos de Trabajo](#flujos-de-trabajo)
8. [Configuración](#configuración)
9. [Despliegue](#despliegue)

## Descripción General

Conecta Ciudad es una plataforma de participación ciudadana que permite a los usuarios colaborar en proyectos comunitarios, gestionar iniciativas y realizar seguimiento a las actividades de la comunidad. El sistema está construido con Spring Boot y utiliza autenticación JWT para la seguridad.

## Roles del Sistema

### 1. ADMIN
- **Responsabilidades**:
  - Gestión completa de usuarios y roles
  - Supervisión de todas las actividades del sistema
  - Configuración del sistema
- **Permisos**: Acceso total al sistema

### 2. CURATOR
- **Responsabilidades**:
  - Revisión y validación de proyectos
  - Aprobación de contenido
  - Moderación de actividades
- **Permisos**:
  - Ver todos los proyectos
  - Aprobar/Rechazar proyectos
  - Moderar contenido

### 3. LIDER_COMUNITARIO
- **Responsabilidades**:
  - Creación y gestión de proyectos
  - Coordinación de equipos
  - Reporte de avances
- **Permisos**:
  - Crear y editar proyectos propios
  - Invitar participantes
  - Gestionar tareas del proyecto

### 4. CIUDADANO
- **Responsabilidades**:
  - Participar en proyectos
  - Realizar acciones ciudadanas
  - Colaborar en iniciativas
- **Permisos**:
  - Ver proyectos públicos
  - Unirse a proyectos
  - Realizar acciones ciudadanas

## Estructura del Proyecto

```
src/main/java/com/unimagdalena/conectaCiudad/
├── config/            # Configuraciones de la aplicación
├── controllers/       # Controladores REST
├── Dto/               # Objetos de Transferencia de Datos
│   ├── access/        # DTOs para accesos
│   ├── action/        # DTOs para acciones
│   ├── auth/          # DTOs para autenticación
│   ├── menu/          # DTOs para menús
│   ├── page/          # DTOs para paginación
│   ├── permission/    # DTOs para permisos
│   ├── project/       # DTOs para proyectos
│   ├── role/          # DTOs para roles
│   ├── status/        # DTOs para estados
│   └── user/          # DTOs para usuarios
├── entities/          # Entidades de la base de datos
├── enums/             # Enumeraciones
├── exceptions/        # Manejo de excepciones
├── repositories/      # Repositorios de datos
├── security/          # Configuración de seguridad
│   └── filters/       # Filtros de autenticación
└── services/          # Lógica de negocio
    ├── access/        # Servicios de acceso
    ├── action/        # Servicios de acciones
    ├── project/       # Servicios de proyectos
    ├── role/          # Servicios de roles
    └── user/          # Servicios de usuarios
```

## Entidades Principales

### Usuario (User)
- **Campos**:
  - id (Long)
  - name (String)
  - nationalId (String)
  - email (String)
  - password (String)
  - active (Boolean)
  - phone (String)
  - createdAt (LocalDateTime)
  - roles (List<Role>)
  - projects (List<Project>)

### Rol (Role)
- **Campos**:
  - id (Long)
  - name (String)
  - permissions (Set<Permission>)

### Proyecto (Project)
- **Campos**:
  - id (Long)
  - name (String)
  - objectives (String)
  - beneficiaryPopulations (String)
  - budgets (String)
  - startAt (LocalDateTime)
  - endAt (LocalDateTime)
  - creator (User)
  - status (ProjectStatus)

## Endpoints de la API

### Autenticación
- `POST /auth/login` - Iniciar sesión
- `POST /auth/refresh-token` - Refrescar token JWT

### Usuarios
- `GET /users` - Listar usuarios (ADMIN)
- `POST /users` - Crear usuario
- `GET /users/{id}` - Obtener usuario por ID
- `PUT /users/{id}` - Actualizar usuario
- `DELETE /users/{id}` - Eliminar usuario (ADMIN)

### Proyectos
- `GET /projects` - Listar proyectos
- `POST /projects` - Crear proyecto (LIDER_COMUNITARIO)
- `GET /projects/{id}` - Obtener proyecto por ID
- `PUT /projects/{id}` - Actualizar proyecto
- `DELETE /projects/{id}` - Eliminar proyecto

### Roles
- `GET /roles` - Listar roles (ADMIN)
- `POST /roles` - Crear rol (ADMIN)
- `PUT /roles/{id}` - Actualizar rol (ADMIN)

## Seguridad

### Autenticación
- JWT (JSON Web Tokens)
- BCrypt para hashing de contraseñas
- Tokens de refresco

### Autorización
- Basada en roles
- Anotaciones `@PreAuthorize`
- Filtros de seguridad personalizados

### CORS
- Orígenes permitidos configurados
- Métodos HTTP permitidos
- Cabeceras permitidas

## Flujos de Trabajo

### 1. Registro de Usuario
1. Usuario se registra con sus datos personales
2. Se asigna rol por defecto (CIUDADANO)
3. Se envía correo de confirmación

### 2. Creación de Proyecto
1. Líder comunitario inicia sesión
2. Crea un nuevo proyecto
3. Proyecto queda en estado "PENDIENTE"
4. Curador revisa y aprueba el proyecto
5. Proyecto cambia a estado "APROBADO"

### 3. Participación Ciudadana
1. Ciudadano inicia sesión
2. Busca proyectos disponibles
3. Se une a un proyecto
4. Realiza acciones ciudadanas

## Configuración

### Variables de Entorno
- `JWT_SECRET`: Clave secreta para JWT
- `DATABASE_URL`: URL de la base de datos
- `EMAIL_HOST`: Servidor de correo
- `EMAIL_PORT`: Puerto del servidor de correo
- `EMAIL_USERNAME`: Usuario de correo
- `EMAIL_PASSWORD`: Contraseña de correo

### Base de Datos
- H2 en memoria para desarrollo
- PostgreSQL para producción
- Configuración en `application.properties`

## Despliegue

### Requisitos
- Java 17 o superior
- Maven 3.6 o superior
- Base de datos (H2/PostgreSQL)

### Pasos para Desplegar
1. Clonar el repositorio
2. Configurar las variables de entorno
3. Ejecutar `mvn clean install`
4. Ejecutar `java -jar target/conecta-ciudad-0.0.1-SNAPSHOT.jar`

### Docker
```bash
# Construir la imagen
docker build -t conecta-ciudad .

# Ejecutar el contenedor
docker run -p 8080:8080 conecta-ciudad
```

## Documentación de la API

La documentación de la API está disponible en:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.
