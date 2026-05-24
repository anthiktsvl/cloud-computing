# EV Charging Station Booking System

Access our EV Charging Station through here: cloud-computing-production-ae11.up.railway.app

A cloud-deployed, full-stack web application that allows electric vehicle drivers to browse charging stations on an interactive map, check real-time slot availability, and make/manage bookings. Administrators can manage the entire platform — stations, connectors, and bookings — through a dedicated admin panel.

---

## Overview

The system is built on **Jakarta EE / JAX-RS (Jersey)** running on **Apache Tomcat 10.1**, backed by a **PostgreSQL** database on the cloud or an **H2** database locally. The frontend is a single-page application (SPA) using **Leaflet.js** and **OpenStreetMap** for interactive station maps.

### Key features

- **Interactive map** — stations are displayed as markers on a Leaflet/OpenStreetMap map. Clicking a marker shows station details and connector options.
- **Real-time slot availability** — drivers can query available charging slots by station, connector, date, and time window before making a booking.
- **Booking management** — drivers can create, edit, and cancel their own bookings. All booking operations enforce business rules (no overlaps, no changes after start time).
- **Role-based access control (RBAC)** — two roles: `DRIVER` and `ADMIN`. Drivers manage only their own bookings; admins have full access.
- **Admin CRUD panel** — admins can add, edit, and delete charging stations and connectors via the web UI.
- **Structured request logging** — every REST call is logged in JSON format including timestamp, method, URI, HTTP status, processing time, and instance ID.
- **Cloud-ready** — deployed on Railway.app with a managed PostgreSQL database, fully containerised using Docker.

---

## Technology stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Backend      | Java 8, Jakarta EE, Jersey 3.1      |
| Runtime      | Apache Tomcat 10.1                  |
| Database     | PostgreSQL (cloud) / H2 (local)     |
| Frontend     | HTML, CSS, Vanilla JS               |
| Maps         | Leaflet 1.9.4 + OpenStreetMap       |
| Build        | Maven 3.9                           |
| Container    | Docker (multi-stage build)          |
| Cloud PaaS   | Railway.app                         |

---

## Cloud Deployment — Railway.app

The application is deployed on **[Railway.app](https://railway.app)**, a cloud platform that supports Docker-based deployments with managed PostgreSQL databases.

### Why Railway?

- Free tier with $5/month credit (no credit card required to start)
- Native Docker support — detects `Dockerfile` automatically
- Managed PostgreSQL service with one-click provisioning
- Automatic redeployment on every GitHub push
- Built-in environment variable management

### Deployment architecture

```
GitHub repo (main branch)
        │
        ▼
Railway build (Docker multi-stage)
   Maven compiles → target/MyWebsite.war
        │
        ▼
Tomcat 10.1 container (port 8080)
        │
        ▼
PostgreSQL (Railway managed database)
```

### Environment variables used on Railway

| Variable            | Description                                      |
|---------------------|--------------------------------------------------|
| `DATABASE_URL`      | PostgreSQL connection URL (from Railway Postgres) |
| `DATABASE_USERNAME` | PostgreSQL username                              |
| `DATABASE_PASSWORD` | PostgreSQL password                              |
| `INSTANCE_ID`       | Identifier for this deployment instance (e.g. `railway-1`) |

The app automatically detects whether it is running locally (H2) or on Railway (PostgreSQL) by checking the presence of `DATABASE_URL`.

### How to redeploy

1. Push changes to the `main` branch on GitHub.
2. Railway automatically triggers a new Docker build and deploys.
3. Or manually trigger a redeploy from the Railway dashboard → Deployments → Redeploy.

---

## Demo users

| Role   | Username  | Password  |
|--------|-----------|-----------|
| Driver | `driver1` | `password` |
| Driver | `driver2` | `password` |
| Admin  | `admin`   | `admin`   |

---

## REST API endpoints

Base path: `/rest`

### Authentication
| Method | Path         | Auth required | Description        |
|--------|--------------|---------------|--------------------|
| POST   | `/auth/login` | No           | Login, returns user object |

### Stations
| Method | Path              | Auth required | Description           |
|--------|-------------------|---------------|-----------------------|
| GET    | `/stations`       | No            | List all stations     |
| POST   | `/stations`       | Admin         | Add a new station     |
| PUT    | `/stations/{id}`  | Admin         | Update a station      |
| DELETE | `/stations/{id}`  | Admin         | Delete a station      |

### Connectors
| Method | Path               | Auth required | Description            |
|--------|--------------------|---------------|------------------------|
| GET    | `/connectors`      | No            | List connectors (optionally filter by `?stationId=`) |
| POST   | `/connectors`      | Admin         | Add a connector        |
| PUT    | `/connectors/{id}` | Admin         | Update a connector     |
| DELETE | `/connectors/{id}` | Admin         | Delete a connector     |

### Available slots
| Method | Path                  | Auth required | Description                              |
|--------|-----------------------|---------------|------------------------------------------|
| GET    | `/slots/available`    | No            | Query available slots by `stationId`, `connectorId`, `date`, `startTime`, `endTime` |

### Bookings
| Method | Path                           | Auth required | Description                          |
|--------|--------------------------------|---------------|--------------------------------------|
| GET    | `/bookings`                    | Driver/Admin  | Driver sees own; admin sees all      |
| POST   | `/bookings`                    | Driver/Admin  | Create a booking                     |
| PUT    | `/bookings/{id}`               | Driver/Admin  | Modify booking (before start time)   |
| DELETE | `/bookings/{id}`               | Driver/Admin  | Cancel booking (before start time)   |
| DELETE | `/bookings/{id}?hardDelete=true` | Admin       | Permanently delete a booking         |

---

## Business rules enforced

- Drivers can only view, modify, and cancel their own bookings.
- Admins have full access to all resources.
- A connector cannot be double-booked for overlapping time slots.
- A driver cannot have two overlapping bookings.
- Bookings cannot be modified or cancelled after the start time has passed.
- A booking must fall within a configured available slot in the database.
- All authentication is HTTP Basic Auth (Base64-encoded `username:password`).

---

## Database schema

The schema is created automatically on startup. There are 5 tables:

- `users` — stores usernames, hashed passwords, and roles
- `charging_stations` — station name, address, coordinates
- `connectors` — connector type linked to a station
- `available_slots` — 14-day rolling grid of hourly slots (8am–10pm) per connector
- `bookings` — driver bookings with status (`ACTIVE`, `CANCELLED`)

**Local development** uses H2 file database (no configuration needed).  
**Cloud deployment** uses PostgreSQL via `DATABASE_URL` environment variable.

---

## Request logging

Every REST request is logged to stdout in structured JSON format:

```json
{
  "timestamp": "2026-05-23T18:45:00.123Z",
  "method": "POST",
  "uri": "/rest/bookings",
  "status": 201,
  "processingTimeMs": 42,
  "instanceId": "railway-1"
}
```

The `instanceId` is set via the `INSTANCE_ID` environment variable, enabling log correlation across multiple instances.

---

## Running locally

```bash
mvn clean package -DskipTests
# deploy target/MyWebsite.war to a local Tomcat 10.1 instance
# app runs at http://localhost:8080/
```

No database configuration is needed — H2 will be created automatically.

---

## Coursework evidence checklist

- [X] Authentication success and failure responses
- [X] DRIVER can only view own bookings
- [X] DRIVER cannot modify/cancel another driver's booking (403 response)
- [X] ADMIN can create, update, delete stations and connectors
- [X] ADMIN can permanently delete a booking (`?hardDelete=true`)
- [X] Overlap rejection for same connector (409 response)
- [X] Overlap rejection for same driver (409 response)
- [X] Rejection when modifying/cancelling after start time (409 response)
- [X] Map-based station selection and slot availability check
- [X] Cloud deployment live URL (Railway.app)
- [X] Cloud database connection (Railway PostgreSQL)
- [X] Request logs with all required JSON fields visible in Railway log viewer
