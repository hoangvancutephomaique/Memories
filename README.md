# Guestbook

A full-stack guestbook built with **Spring Boot (Java)** and **React (TypeScript + Vite)**.

## Stack

| Layer    | Tech                          |
|----------|-------------------------------|
| Backend  | Java 21, Spring Boot 3, H2    |
| Frontend | React 18, TypeScript, Vite    |
| Styles   | Plain CSS (dark theme)        |

## Project structure

```
guestbook/
├── backend/   – Spring Boot Maven project
└── frontend/  – React + Vite app
```

## Running locally

### One command (recommended)

```bash
npm run dev
```

This starts both the backend (port 8080) and the frontend dev server (port 5173) together using `concurrently`. Open **http://localhost:5173**.

### Or start them separately

```bash
# terminal 1
cd backend && ./mvnw spring-boot:run

# terminal 2
cd frontend && npm run dev
```

H2 console (dev only): http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:file:./data/guestbook`

## Production build (single process)

```bash
cd backend
./mvnw package -Pprod -DskipTests
java -jar target/guestbook-backend-0.0.1-SNAPSHOT.jar
```

The Maven build runs `npm run build` in `frontend/`, bundles the React output into the JAR, and Spring Boot serves everything at **http://localhost:8080**. No separate frontend server needed.

## API

| Method | Endpoint           | Description          |
|--------|--------------------|----------------------|
| GET    | /api/entries       | List all entries     |
| POST   | /api/entries       | Create a new entry   |
| DELETE | /api/entries/{id}  | Delete an entry      |

### POST body

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "message": "Hello from 1843!"
}
```
