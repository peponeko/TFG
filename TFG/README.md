# Easy4You

Easy4You es una aplicacion web de estudio inteligente para organizar asignaturas, temas y documentos, y generar contenido de apoyo con IA local.

## Stack tecnologico

- Backend: Spring Boot, Spring Security JWT, JPA/Hibernate, MySQL
- Frontend: Thymeleaf, Tailwind CSS, JavaScript vanilla
- IA local: Ollama con `tinyllama:latest`

## Requisitos

- Java 17+
- Maven 3.9+
- MySQL 8
- Ollama instalado y activo

## Instalacion rapida

1. Crear base de datos y usuario:
   - Ejecuta `easy4you_db.sql` en MySQL.
2. Instalar modelo IA:
   - `ollama pull tinyllama`
   - `ollama run tinyllama`
3. Arrancar backend:
   - `mvn spring-boot:run`
4. Abrir aplicacion:
   - [http://localhost:8080](http://localhost:8080)

## Estructura de carpetas

- `src/main/java/com/easy4you`: backend (controllers, services, security, repos)
- `src/main/resources/templates`: vistas Thymeleaf
- `src/main/resources/static/css`: estilos CSS
- `src/main/resources/static/js`: logica frontend
- `easy4you_db.sql`: esquema y datos base

## Rutas principales

- `/app/login`: acceso
- `/app/home`: home de asignaturas
- `/app/home/{asignaturaId}/trimestres`: selector por trimestre
- `/app/home/{asignaturaId}/trimestre/{trimestre}/temas`: lista de temas
- `/app/notebooks`: dashboard notebook
- `/app/notebooks/{id}`: detalle notebook
- `/app/notas`: notas

## Capturas de pantalla

- Home: pendiente
- Trimestres: pendiente
- Temas: pendiente
- Notebook: pendiente
