# EV Charging Station Booking System

This project replaces the original Product REST example with an EV charging station booking application.

## Demo users

- Driver: `driver1` / `password`
- Driver: `driver2` / `password`
- Admin: `admin` / `admin`

## Main REST endpoints

Base path: `/MyWebsite/rest`

- `POST /auth/login`
- `GET /stations`
- `POST /stations` admin only
- `PUT /stations/{id}` admin only
- `DELETE /stations/{id}` admin only
- `GET /connectors?stationId=1`
- `GET /slots/available?stationId=1&date=2026-06-05&startTime=10:00&endTime=11:00`
- `POST /connectors` admin only
- `PUT /connectors/{id}` admin only
- `DELETE /connectors/{id}` admin only
- `GET /bookings` driver sees own bookings, admin sees all
- `POST /bookings` creates a booking
- `PUT /bookings/{id}` modifies a booking before it starts
- `DELETE /bookings/{id}` cancels a booking before it starts
- `DELETE /bookings/{id}?hardDelete=true` admin hard delete

## Implemented assignment rules

- Drivers can only view, modify, and cancel their own bookings.
- Admin can manage stations, connectors, and all bookings.
- The DAO rejects connector double-bookings for overlapping time periods.
- The DAO rejects overlapping bookings for the same driver.
- A booking cannot be modified or cancelled after its start time.
- Available charging slots are stored in a database table and can be queried by station/connector/date/time.
- Request logging is implemented in `RequestLoggingFilter` with timestamp, method, URI, status code, processing time, and instance identifier.
- The client UI includes a Leaflet/OpenStreetMap station map.

## Database

`AppDAO` is JDBC-based and initializes schema + seed data automatically.

- Local default (no env vars): H2 file database
- Cloud option: set PostgreSQL JDBC URL and credentials via env vars

## Suggested deployment configuration

Use environment variables such as:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `INSTANCE_ID`

The request logger already reads `INSTANCE_ID`, which supports multi-instance cloud deployments.

## PaaS deployment steps (for coursework)

1. Provision a PostgreSQL database add-on/service on your PaaS provider.
2. Set app environment variables:
	- `DATABASE_URL` (JDBC URL, for example `jdbc:postgresql://host:5432/dbname`)
	- `DATABASE_USERNAME`
	- `DATABASE_PASSWORD`
	- `INSTANCE_ID` (for example `ev-booking-instance-1`)
3. Deploy the WAR to your servlet runtime (Tomcat/Jakarta-compatible runtime on your platform).
	- Optional: use the provided `Dockerfile` for container-based PaaS deployment.
4. Send test requests to:
	- `/rest/auth/login`
	- `/rest/stations`
	- `/rest/connectors`
	- `/rest/slots/available`
	- `/rest/bookings`
5. Confirm logs include required fields on each request:
	- `timestamp`
	- `method`
	- `uri`
	- `status`
	- `processingTimeMs`
	- `instanceId`

## Coursework evidence checklist

Capture screenshots or API traces for each of the following:

- Authentication success and failure.
- DRIVER can only view own bookings.
- DRIVER cannot modify/cancel another driver's booking.
- ADMIN can create/update/delete stations and connectors.
- ADMIN can hard delete a booking (`hardDelete=true`).
- Overlap rejection for same connector and same driver.
- Rejection when trying to modify/cancel after start time.
- Map-based station selection and slot availability check.
- Cloud deployment URL and cloud database connection details.
- Request logs with all required logging fields.
