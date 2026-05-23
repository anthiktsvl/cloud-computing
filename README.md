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
- `POST /connectors` admin only
- `PUT /connectors/{id}` admin only
- `DELETE /connectors/{id}` admin only
- `GET /bookings` driver sees own bookings, admin sees all
- `POST /bookings` creates a booking
- `PUT /bookings/{id}` modifies a booking before it starts
- `DELETE /bookings/{id}` cancels a booking before it starts

## Implemented assignment rules

- Drivers can only view, modify, and cancel their own bookings.
- Admin can manage stations, connectors, and all bookings.
- The DAO rejects connector double-bookings for overlapping time periods.
- The DAO rejects overlapping bookings for the same driver.
- A booking cannot be modified or cancelled after its start time.
- Request logging is implemented in `RequestLoggingFilter` with timestamp, method, URI, status code, processing time, and instance identifier.
- The client UI includes a Leaflet/OpenStreetMap station map.

## Important limitation

The uploaded starter code used an in-memory DAO, so this version keeps the same style to make it easy to run in Eclipse/Tomcat. For the final cloud submission, replace `AppDAO` with a JDBC/cloud database DAO and externalise the database URL, username, and password via environment variables.

## Suggested deployment configuration

Use environment variables such as:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `INSTANCE_ID`

The request logger already reads `INSTANCE_ID`, which supports multi-instance cloud deployments.
