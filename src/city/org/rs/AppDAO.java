package city.org.rs;

import java.net.URI;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class AppDAO {
    private static AppDAO instance;
    private final String jdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    private AppDAO() {
        String envUrl = System.getenv("DATABASE_URL");
        boolean hasExternalDbUrl = envUrl != null && !envUrl.trim().isEmpty();
        if (hasExternalDbUrl) {
            DbConfig cfg = parseExternalDbConfig(envUrl);
            this.jdbcUrl = cfg.jdbcUrl;
            this.dbUser = getOrDefault("DATABASE_USERNAME", cfg.dbUser == null ? "" : cfg.dbUser);
            this.dbPassword = getOrDefault("DATABASE_PASSWORD", cfg.dbPassword == null ? "" : cfg.dbPassword);
        } else {
            this.jdbcUrl = "jdbc:h2:file:./evbookingdb;MODE=PostgreSQL;AUTO_SERVER=TRUE";
            this.dbUser = getOrDefault("DATABASE_USERNAME", "sa");
            this.dbPassword = getOrDefault("DATABASE_PASSWORD", "");
        }
        initializeDatabase();
    }

    public static synchronized AppDAO getInstance() { if (instance == null) instance = new AppDAO(); return instance; }

    public synchronized User authenticate(String username, String password) {
        String sql = "SELECT username, password, role FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String stored = rs.getString("password");
                if (!safeEquals(stored, password)) return null;
                return new User(rs.getString("username"), stored, rs.getString("role"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database authentication error", e);
        }
    }

    public synchronized List<ChargingStation> listStations() {
        List<ChargingStation> out = new ArrayList<ChargingStation>();
        String sql = "SELECT station_id, station_name, address, latitude, longitude FROM charging_stations ORDER BY station_id";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(mapStation(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list stations", e);
        }
    }

    public synchronized ChargingStation getStation(int id) {
        String sql = "SELECT station_id, station_name, address, latitude, longitude FROM charging_stations WHERE station_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapStation(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch station", e);
        }
    }

    public synchronized ChargingStation addStation(ChargingStation s) {
        String sql = "INSERT INTO charging_stations (station_name, address, latitude, longitude) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getStationName());
            ps.setString(2, s.getAddress());
            ps.setDouble(3, s.getLatitude());
            ps.setDouble(4, s.getLongitude());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setStationId(keys.getInt(1));
            }
            return s;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add station", e);
        }
    }

    public synchronized boolean updateStation(int id, ChargingStation s) {
        String sql = "UPDATE charging_stations SET station_name = ?, address = ?, latitude = ?, longitude = ? WHERE station_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getStationName());
            ps.setString(2, s.getAddress());
            ps.setDouble(3, s.getLatitude());
            ps.setDouble(4, s.getLongitude());
            ps.setInt(5, id);
            int rows = ps.executeUpdate();
            s.setStationId(id);
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update station", e);
        }
    }

    public synchronized boolean deleteStation(int id) {
        try (Connection conn = getConnection()) {
            if (count(conn, "SELECT COUNT(*) FROM bookings WHERE station_id = ?", id) > 0) {
                throw new IllegalStateException("Cannot delete station with existing bookings");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate station deletion", e);
        }
        String sql = "DELETE FROM charging_stations WHERE station_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete station", e);
        }
    }

    public synchronized List<Connector> listConnectors(Integer stationId) {
        List<Connector> out = new ArrayList<Connector>();
        String sql = stationId == null
                ? "SELECT connector_id, connector_type, station_id FROM connectors ORDER BY connector_id"
                : "SELECT connector_id, connector_type, station_id FROM connectors WHERE station_id = ? ORDER BY connector_id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (stationId != null) ps.setInt(1, stationId.intValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapConnector(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list connectors", e);
        }
    }

    public synchronized Connector getConnector(int id) {
        String sql = "SELECT connector_id, connector_type, station_id FROM connectors WHERE connector_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapConnector(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connector", e);
        }
    }

    public synchronized Connector addConnector(Connector c) {
        if (getStation(c.getStationId()) == null) throw new IllegalArgumentException("Station does not exist");
        String sql = "INSERT INTO connectors (connector_type, station_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getConnectorType());
            ps.setInt(2, c.getStationId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setConnectorId(keys.getInt(1));
            }
            seedAvailableSlotsForConnector(c.getConnectorId(), c.getStationId());
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add connector", e);
        }
    }

    public synchronized boolean updateConnector(int id, Connector c) {
        if (getStation(c.getStationId()) == null) throw new IllegalArgumentException("Station does not exist");
        String sql = "UPDATE connectors SET connector_type = ?, station_id = ? WHERE connector_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getConnectorType());
            ps.setInt(2, c.getStationId());
            ps.setInt(3, id);
            int rows = ps.executeUpdate();
            c.setConnectorId(id);
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update connector", e);
        }
    }

    public synchronized boolean deleteConnector(int id) {
        try (Connection conn = getConnection()) {
            if (count(conn, "SELECT COUNT(*) FROM bookings WHERE connector_id = ?", id) > 0) {
                throw new IllegalStateException("Cannot delete connector with existing bookings");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate connector deletion", e);
        }
        String sql = "DELETE FROM connectors WHERE connector_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete connector", e);
        }
    }

    public synchronized List<Booking> listBookingsFor(User user) {
        List<Booking> out = new ArrayList<Booking>();
        String sql = AuthUtil.isAdmin(user)
                ? "SELECT booking_id, driver_username, station_id, connector_id, booking_date, start_time, end_time, booking_status FROM bookings ORDER BY booking_date, start_time"
                : "SELECT booking_id, driver_username, station_id, connector_id, booking_date, start_time, end_time, booking_status FROM bookings WHERE driver_username = ? ORDER BY booking_date, start_time";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!AuthUtil.isAdmin(user)) ps.setString(1, user.getUsername());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapBooking(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list bookings", e);
        }
    }

    public synchronized Booking getBooking(int id) {
        String sql = "SELECT booking_id, driver_username, station_id, connector_id, booking_date, start_time, end_time, booking_status FROM bookings WHERE booking_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get booking", e);
        }
    }

    public synchronized String validateBooking(Booking b, Integer ignoreBookingId) {
        if (b == null || b.getDate() == null || b.getStartTime() == null || b.getEndTime() == null) return "Invalid date/time";
        LocalDate date;
        LocalTime start;
        LocalTime end;
        try {
            date = LocalDate.parse(b.getDate());
            start = LocalTime.parse(b.getStartTime());
            end = LocalTime.parse(b.getEndTime());
        } catch (Exception e) {
            return "Invalid date/time";
        }
        if (!start.isBefore(end)) return "Invalid date/time";
        if (getStation(b.getStationId()) == null) return "Station does not exist";
        Connector connector = getConnector(b.getConnectorId());
        if (connector == null || connector.getStationId() != b.getStationId()) return "Connector does not exist for this station";

        String overlapSql = "SELECT COUNT(*) FROM bookings WHERE booking_status = 'ACTIVE' AND booking_date = ? AND connector_id = ? AND booking_id <> ? AND ? < end_time AND ? > start_time";
        String overlapSqlNoIgnore = "SELECT COUNT(*) FROM bookings WHERE booking_status = 'ACTIVE' AND booking_date = ? AND connector_id = ? AND ? < end_time AND ? > start_time";
        String driverSql = "SELECT COUNT(*) FROM bookings WHERE booking_status = 'ACTIVE' AND booking_date = ? AND driver_username = ? AND booking_id <> ? AND ? < end_time AND ? > start_time";
        String driverSqlNoIgnore = "SELECT COUNT(*) FROM bookings WHERE booking_status = 'ACTIVE' AND booking_date = ? AND driver_username = ? AND ? < end_time AND ? > start_time";

        try (Connection conn = getConnection()) {
            if (ignoreBookingId != null) {
                if (count(conn, overlapSql, b.getDate(), b.getConnectorId(), ignoreBookingId.intValue(), b.getStartTime(), b.getEndTime()) > 0) {
                    return "This connector is already booked during that time";
                }
                if (count(conn, driverSql, b.getDate(), b.getDriverUsername(), ignoreBookingId.intValue(), b.getStartTime(), b.getEndTime()) > 0) {
                    return "Driver already has an overlapping booking";
                }
            } else {
                if (count(conn, overlapSqlNoIgnore, b.getDate(), b.getConnectorId(), b.getStartTime(), b.getEndTime()) > 0) {
                    return "This connector is already booked during that time";
                }
                if (count(conn, driverSqlNoIgnore, b.getDate(), b.getDriverUsername(), b.getStartTime(), b.getEndTime()) > 0) {
                    return "Driver already has an overlapping booking";
                }
            }

            String slotSql = "SELECT COUNT(*) FROM available_slots WHERE station_id = ? AND connector_id = ? AND slot_date = ? AND start_time <= ? AND end_time >= ? AND is_available = TRUE";
            if (count(conn, slotSql, b.getStationId(), b.getConnectorId(), Date.valueOf(date), Time.valueOf(start), Time.valueOf(end)) <= 0) {
                return "Requested time is outside available slots";
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate booking", e);
        }
    }

    public synchronized Booking addBooking(Booking b) {
        String err = validateBooking(b, null);
        if (err != null) throw new IllegalArgumentException(err);
        String sql = "INSERT INTO bookings (driver_username, station_id, connector_id, booking_date, start_time, end_time, booking_status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getDriverUsername());
            ps.setInt(2, b.getStationId());
            ps.setInt(3, b.getConnectorId());
            ps.setDate(4, Date.valueOf(b.getDate()));
            ps.setTime(5, Time.valueOf(LocalTime.parse(b.getStartTime())));
            ps.setTime(6, Time.valueOf(LocalTime.parse(b.getEndTime())));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setBookingId(keys.getInt(1));
            }
            b.setBookingStatus("ACTIVE");
            return b;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create booking", e);
        }
    }

    public synchronized boolean updateBooking(int id, Booking updated, User user) {
        Booking old = getBooking(id);
        if (old == null) return false;
        if (!AuthUtil.isAdmin(user) && !old.getDriverUsername().equals(user.getUsername())) throw new SecurityException();
        if (hasStarted(old)) throw new IllegalStateException("Booking already started");

        updated.setBookingId(id);
        updated.setDriverUsername(AuthUtil.isAdmin(user) && updated.getDriverUsername() != null ? updated.getDriverUsername() : old.getDriverUsername());
        updated.setBookingStatus("ACTIVE");
        String err = validateBooking(updated, Integer.valueOf(id));
        if (err != null) throw new IllegalArgumentException(err);

        String sql = "UPDATE bookings SET driver_username = ?, station_id = ?, connector_id = ?, booking_date = ?, start_time = ?, end_time = ?, booking_status = 'ACTIVE' WHERE booking_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updated.getDriverUsername());
            ps.setInt(2, updated.getStationId());
            ps.setInt(3, updated.getConnectorId());
            ps.setDate(4, Date.valueOf(updated.getDate()));
            ps.setTime(5, Time.valueOf(LocalTime.parse(updated.getStartTime())));
            ps.setTime(6, Time.valueOf(LocalTime.parse(updated.getEndTime())));
            ps.setInt(7, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update booking", e);
        }
    }

    public synchronized boolean cancelBooking(int id, User user) {
        Booking b = getBooking(id);
        if (b == null) return false;
        if (!AuthUtil.isAdmin(user) && !b.getDriverUsername().equals(user.getUsername())) throw new SecurityException();
        if (hasStarted(b)) throw new IllegalStateException("Booking already started");

        String sql = "UPDATE bookings SET booking_status = 'CANCELLED' WHERE booking_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel booking", e);
        }
    }

    public synchronized boolean hardDeleteBooking(int id, User user) {
        if (!AuthUtil.isAdmin(user)) throw new SecurityException();
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete booking", e);
        }
    }

    public synchronized List<AvailableSlot> listAvailableSlots(Integer stationId, Integer connectorId, String date, String startTime, String endTime) {
        List<AvailableSlot> out = new ArrayList<AvailableSlot>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.slot_id, s.station_id, s.connector_id, s.slot_date, s.start_time, s.end_time, s.is_available, ")
           .append("CASE WHEN EXISTS (")
           .append("SELECT 1 FROM bookings b WHERE b.booking_status='ACTIVE' AND b.connector_id=s.connector_id AND b.booking_date=s.slot_date ")
           .append("AND b.start_time < s.end_time AND b.end_time > s.start_time")
           .append(") THEN FALSE ELSE TRUE END AS free_now ")
           .append("FROM available_slots s WHERE 1=1 ");
        if (stationId != null) sql.append("AND s.station_id = ? ");
        if (connectorId != null) sql.append("AND s.connector_id = ? ");
        if (date != null && !date.trim().isEmpty()) sql.append("AND s.slot_date = ? ");
        if (startTime != null && !startTime.trim().isEmpty()) sql.append("AND s.end_time > ? ");
        if (endTime != null && !endTime.trim().isEmpty()) sql.append("AND s.start_time < ? ");
        sql.append("ORDER BY s.slot_date, s.start_time");

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (stationId != null) ps.setInt(idx++, stationId.intValue());
            if (connectorId != null) ps.setInt(idx++, connectorId.intValue());
            if (date != null && !date.trim().isEmpty()) ps.setDate(idx++, Date.valueOf(date));
            if (startTime != null && !startTime.trim().isEmpty()) ps.setTime(idx++, Time.valueOf(LocalTime.parse(startTime)));
            if (endTime != null && !endTime.trim().isEmpty()) ps.setTime(idx++, Time.valueOf(LocalTime.parse(endTime)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AvailableSlot slot = new AvailableSlot();
                    slot.setSlotId(rs.getInt("slot_id"));
                    slot.setStationId(rs.getInt("station_id"));
                    slot.setConnectorId(rs.getInt("connector_id"));
                    slot.setDate(rs.getDate("slot_date").toString());
                    slot.setStartTime(rs.getTime("start_time").toLocalTime().toString());
                    slot.setEndTime(rs.getTime("end_time").toLocalTime().toString());
                    slot.setConfiguredAvailable(rs.getBoolean("is_available"));
                    slot.setFreeNow(rs.getBoolean("free_now"));
                    slot.setAvailable(slot.isConfiguredAvailable() && slot.isFreeNow());
                    out.add(slot);
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list slots", e);
        }
    }

    private boolean hasStarted(Booking b) {
        try {
            return LocalDateTime.now().isAfter(LocalDateTime.parse(b.getDate() + "T" + b.getStartTime()));
        } catch (Exception e) {
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            if (jdbcUrl.startsWith("jdbc:postgresql:")) {
                Class.forName("org.postgresql.Driver");
            } else if (jdbcUrl.startsWith("jdbc:h2:")) {
                Class.forName("org.h2.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found for URL: " + jdbcUrl, e);
        }

        if (dbUser == null || dbUser.trim().isEmpty()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (username VARCHAR(100) PRIMARY KEY, password VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS charging_stations (station_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, station_name VARCHAR(255) NOT NULL, address VARCHAR(255) NOT NULL, latitude DOUBLE PRECISION NOT NULL, longitude DOUBLE PRECISION NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS connectors (connector_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, connector_type VARCHAR(100) NOT NULL, station_id INTEGER NOT NULL, CONSTRAINT fk_connector_station FOREIGN KEY (station_id) REFERENCES charging_stations(station_id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE IF NOT EXISTS available_slots (slot_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, station_id INTEGER NOT NULL, connector_id INTEGER NOT NULL, slot_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, is_available BOOLEAN NOT NULL DEFAULT TRUE, CONSTRAINT fk_slot_station FOREIGN KEY (station_id) REFERENCES charging_stations(station_id) ON DELETE CASCADE, CONSTRAINT fk_slot_connector FOREIGN KEY (connector_id) REFERENCES connectors(connector_id) ON DELETE CASCADE)");
            st.execute("CREATE TABLE IF NOT EXISTS bookings (booking_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, driver_username VARCHAR(100) NOT NULL, station_id INTEGER NOT NULL, connector_id INTEGER NOT NULL, booking_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, booking_status VARCHAR(20) NOT NULL, CONSTRAINT fk_booking_user FOREIGN KEY (driver_username) REFERENCES users(username), CONSTRAINT fk_booking_station FOREIGN KEY (station_id) REFERENCES charging_stations(station_id), CONSTRAINT fk_booking_connector FOREIGN KEY (connector_id) REFERENCES connectors(connector_id))");
            seedIfEmpty(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void seedIfEmpty(Connection conn) throws SQLException {
        if (count(conn, "SELECT COUNT(*) FROM users") > 0) return;

        try (PreparedStatement userPs = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
             PreparedStatement stPs = conn.prepareStatement("INSERT INTO charging_stations (station_name, address, latitude, longitude) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement connPs = conn.prepareStatement("INSERT INTO connectors (connector_type, station_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement bookingPs = conn.prepareStatement("INSERT INTO bookings (driver_username, station_id, connector_id, booking_date, start_time, end_time, booking_status) VALUES (?, ?, ?, ?, ?, ?, ?)")
        ) {
            insertUser(userPs, "driver1", "password", "DRIVER");
            insertUser(userPs, "driver2", "password", "DRIVER");
            insertUser(userPs, "admin", "admin", "ADMIN");

            int s1 = insertStation(stPs, "City College EV Hub", "Leontos Sofou 3, Thessaloniki", 40.6381, 22.9444);
            int s2 = insertStation(stPs, "White Tower Charger", "Nikis Avenue, Thessaloniki", 40.6264, 22.9484);
            int s3 = insertStation(stPs, "Athens Central EV", "Syntagma Square, Athens", 37.9755, 23.7348);

            int c1 = insertConnector(connPs, "Type 2", s1);
            insertConnector(connPs, "CCS", s1);
            insertConnector(connPs, "CHAdeMO", s2);
            insertConnector(connPs, "Type 2", s2);
            insertConnector(connPs, "CCS", s3);
            insertConnector(connPs, "Type 2", s3);

            seedAvailableSlots(conn);

            bookingPs.setString(1, "driver1");
            bookingPs.setInt(2, s1);
            bookingPs.setInt(3, c1);
            bookingPs.setDate(4, Date.valueOf(LocalDate.now().plusDays(2)));
            bookingPs.setTime(5, Time.valueOf("10:00:00"));
            bookingPs.setTime(6, Time.valueOf("11:00:00"));
            bookingPs.setString(7, "ACTIVE");
            bookingPs.executeUpdate();
        }
    }

    private void seedAvailableSlots(Connection conn) throws SQLException {
        if (count(conn, "SELECT COUNT(*) FROM available_slots") > 0) return;
        String connectorsSql = "SELECT connector_id, station_id FROM connectors";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(connectorsSql);
             PreparedStatement slotPs = conn.prepareStatement("INSERT INTO available_slots (station_id, connector_id, slot_date, start_time, end_time, is_available) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            while (rs.next()) {
                int connectorId = rs.getInt("connector_id");
                int stationId = rs.getInt("station_id");
                for (int d = 0; d < 14; d++) {
                    LocalDate date = LocalDate.now().plusDays(d);
                    for (int hour = 8; hour < 22; hour++) {
                        slotPs.setInt(1, stationId);
                        slotPs.setInt(2, connectorId);
                        slotPs.setDate(3, Date.valueOf(date));
                        slotPs.setTime(4, Time.valueOf(LocalTime.of(hour, 0)));
                        slotPs.setTime(5, Time.valueOf(LocalTime.of(hour + 1, 0)));
                        slotPs.addBatch();
                    }
                }
            }
            slotPs.executeBatch();
        }
    }

    private void seedAvailableSlotsForConnector(int connectorId, int stationId) throws SQLException {
        String sql = "INSERT INTO available_slots (station_id, connector_id, slot_date, start_time, end_time, is_available) VALUES (?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = getConnection(); PreparedStatement slotPs = conn.prepareStatement(sql)) {
            for (int d = 0; d < 14; d++) {
                LocalDate date = LocalDate.now().plusDays(d);
                for (int hour = 8; hour < 22; hour++) {
                    slotPs.setInt(1, stationId);
                    slotPs.setInt(2, connectorId);
                    slotPs.setDate(3, Date.valueOf(date));
                    slotPs.setTime(4, Time.valueOf(LocalTime.of(hour, 0)));
                    slotPs.setTime(5, Time.valueOf(LocalTime.of(hour + 1, 0)));
                    slotPs.addBatch();
                }
            }
            slotPs.executeBatch();
        }
    }

    private int insertStation(PreparedStatement ps, String name, String address, double lat, double lng) throws SQLException {
        ps.setString(1, name);
        ps.setString(2, address);
        ps.setDouble(3, lat);
        ps.setDouble(4, lng);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("No key generated");
            return keys.getInt(1);
        }
    }

    private int insertConnector(PreparedStatement ps, String type, int stationId) throws SQLException {
        ps.setString(1, type);
        ps.setInt(2, stationId);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("No key generated");
            return keys.getInt(1);
        }
    }

    private void insertUser(PreparedStatement ps, String username, String password, String role) throws SQLException {
        ps.setString(1, username);
        ps.setString(2, password);
        ps.setString(3, role);
        ps.executeUpdate();
    }

    private int count(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private ChargingStation mapStation(ResultSet rs) throws SQLException {
        return new ChargingStation(
                rs.getInt("station_id"),
                rs.getString("station_name"),
                rs.getString("address"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude")
        );
    }

    private Connector mapConnector(ResultSet rs) throws SQLException {
        return new Connector(
                rs.getInt("connector_id"),
                rs.getString("connector_type"),
                rs.getInt("station_id")
        );
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getInt("booking_id"),
                rs.getString("driver_username"),
                rs.getInt("station_id"),
                rs.getInt("connector_id"),
                rs.getDate("booking_date").toString(),
                rs.getTime("start_time").toLocalTime().toString(),
                rs.getTime("end_time").toLocalTime().toString(),
                rs.getString("booking_status")
        );
    }

    private String getOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private DbConfig parseExternalDbConfig(String rawUrl) {
        String trimmed = rawUrl.trim();
        String uriValue = trimmed;
        if (trimmed.startsWith("jdbc:")) {
            uriValue = trimmed.substring("jdbc:".length());
        }

        URI uri = URI.create(uriValue);
        String scheme = uri.getScheme();
        if (scheme == null || (!"postgres".equals(scheme) && !"postgresql".equals(scheme))) {
            String fallback = trimmed;
            if (fallback.startsWith("postgres://")) {
                fallback = "jdbc:postgresql://" + fallback.substring("postgres://".length());
            } else if (fallback.startsWith("postgresql://")) {
                fallback = "jdbc:" + fallback;
            }
            if (fallback.startsWith("jdbc:postgresql://") && !fallback.contains("sslmode=")) {
                fallback = fallback + (fallback.contains("?") ? "&" : "?") + "sslmode=require";
            }
            return new DbConfig(fallback, null, null);
        }

        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();
        String query = uri.getQuery();
        String userInfo = uri.getUserInfo();

        String parsedUser = null;
        String parsedPassword = null;
        if (userInfo != null && !userInfo.isEmpty()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                parsedUser = userInfo.substring(0, colon);
                parsedPassword = userInfo.substring(colon + 1);
            } else {
                parsedUser = userInfo;
            }
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
        jdbc.append(host == null ? "localhost" : host);
        if (port > 0) jdbc.append(":").append(port);
        if (path != null && !path.isEmpty()) jdbc.append(path);
        if (query != null && !query.isEmpty()) jdbc.append("?").append(query);
        if (jdbc.indexOf("sslmode=") < 0) {
            jdbc.append(jdbc.indexOf("?") >= 0 ? "&" : "?").append("sslmode=require");
        }

        return new DbConfig(jdbc.toString(), parsedUser, parsedPassword);
    }

    private static final class DbConfig {
        private final String jdbcUrl;
        private final String dbUser;
        private final String dbPassword;

        private DbConfig(String jdbcUrl, String dbUser, String dbPassword) {
            this.jdbcUrl = jdbcUrl;
            this.dbUser = dbUser;
            this.dbPassword = dbPassword;
        }
    }
}
