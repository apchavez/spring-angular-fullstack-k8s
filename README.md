# Reactive Customer Service API

API REST reactiva desarrollada con **Spring Boot WebFlux** para gestionar clientes con operaciones básicas de registro y consulta por estado.

## Tecnologías

* Java 17
* Spring Boot 3
* Spring WebFlux
* Spring Data R2DBC
* H2 Database
* Gradle
* Lombok

## Funcionalidades

* Crear clientes
* Validar datos de entrada
* Evitar IDs duplicados
* Consultar clientes activos
* Manejo de errores funcional
* Persistencia reactiva con R2DBC

## Ejecutar localmente

```bash
./gradlew bootRun
```

La aplicación inicia en:

```text
http://localhost:8080
```

---

## Endpoints disponibles

### Crear cliente

**POST** `/customers`

#### Request Body

```json
{
  "id": 1,
  "name": "Alex",
  "lastname": "Prieto",
  "state": "active",
  "age": 30
}
```

#### Respuesta esperada

```text
201 Created
```

---

### Validar ID duplicado

**POST** `/customers`

#### Request Body

```json
{
  "id": 1,
  "name": "Juan",
  "lastname": "Perez",
  "state": "active",
  "age": 25
}
```

#### Respuesta esperada

```text
400 Bad Request
```

---

### Listar clientes activos

**GET** `/customers/active`

#### Respuesta esperada

```json
[
  {
    "id": 1,
    "name": "Alex",
    "lastname": "Prieto",
    "state": "active",
    "age": 30
  }
]
```

---

## Ejemplos con cURL

### Crear cliente

```bash
curl -X POST http://localhost:8080/customers \
-H "Content-Type: application/json" \
-d '{
  "id":1,
  "name":"Alex",
  "lastname":"Prieto",
  "state":"active",
  "age":30
}'
```

### Consultar clientes activos

```bash
curl http://localhost:8080/customers/active
```

---

## Arquitectura

```text
src/main/java/com/apchavez/customers
├── controller
├── model
├── repository
├── service
└── CustomerServiceApplication.java
```

---

## Notas

* Proyecto desarrollado con enfoque reactivo usando `Mono` y `Flux`.
* Base de datos en memoria H2 para pruebas locales.
* Incluye pruebas unitarias con JUnit, Mockito y StepVerifier.
* Ideal como demo técnica de backend moderno con Java.

---

## Mejoras futuras

* Swagger / OpenAPI
* Dockerización
* PostgreSQL
* Seguridad con Spring Security
* Logging centralizado
* Paginación y filtros dinámicos

---

## Autor

Proyecto orientado a demostrar habilidades en desarrollo backend reactivo con Java y Spring Boot.
