package city.org.rs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AppDAO {
    private static AppDAO instance;
    private final List<User> users = new ArrayList<User>();
    private final List<ChargingStation> stations = new ArrayList<ChargingStation>();
    private final List<Connector> connectors = new ArrayList<Connector>();
    private final List<Booking> bookings = new ArrayList<Booking>();
    private int nextStationId = 4, nextConnectorId = 7, nextBookingId = 2;

    private AppDAO() {
        users.add(new User("driver1", "password", "DRIVER"));
        users.add(new User("driver2", "password", "DRIVER"));
        users.add(new User("admin", "admin", "ADMIN"));
        stations.add(new ChargingStation(1, "City College EV Hub", "Leontos Sofou 3, Thessaloniki", 40.6381, 22.9444));
        stations.add(new ChargingStation(2, "White Tower Charger", "Nikis Avenue, Thessaloniki", 40.6264, 22.9484));
        stations.add(new ChargingStation(3, "Athens Central EV", "Syntagma Square, Athens", 37.9755, 23.7348));
        connectors.add(new Connector(1, "Type 2", 1)); connectors.add(new Connector(2, "CCS", 1));
        connectors.add(new Connector(3, "CHAdeMO", 2)); connectors.add(new Connector(4, "Type 2", 2));
        connectors.add(new Connector(5, "CCS", 3)); connectors.add(new Connector(6, "Type 2", 3));
        bookings.add(new Booking(1, "driver1", 1, 1, "2026-06-02", "10:00", "11:00", "ACTIVE"));
    }
    public static synchronized AppDAO getInstance() { if (instance == null) instance = new AppDAO(); return instance; }

    public synchronized User authenticate(String username, String password) { for (User u:users) if (u.getUsername().equals(username)&&u.getPassword().equals(password)) return u; return null; }
    public synchronized List<ChargingStation> listStations(){ return new ArrayList<ChargingStation>(stations); }
    public synchronized ChargingStation getStation(int id){ for(ChargingStation s:stations) if(s.getStationId()==id) return s; return null; }
    public synchronized ChargingStation addStation(ChargingStation s){ s.setStationId(nextStationId++); stations.add(s); return s; }
    public synchronized boolean updateStation(int id, ChargingStation s){ for(int i=0;i<stations.size();i++) if(stations.get(i).getStationId()==id){s.setStationId(id); stations.set(i,s); return true;} return false; }
    public synchronized boolean deleteStation(int id){ return stations.removeIf(s -> s.getStationId()==id); }

    public synchronized List<Connector> listConnectors(Integer stationId){ List<Connector> out=new ArrayList<Connector>(); for(Connector c:connectors) if(stationId==null||c.getStationId()==stationId) out.add(c); return out; }
    public synchronized Connector getConnector(int id){ for(Connector c:connectors) if(c.getConnectorId()==id) return c; return null; }
    public synchronized Connector addConnector(Connector c){ c.setConnectorId(nextConnectorId++); connectors.add(c); return c; }
    public synchronized boolean updateConnector(int id, Connector c){ for(int i=0;i<connectors.size();i++) if(connectors.get(i).getConnectorId()==id){c.setConnectorId(id); connectors.set(i,c); return true;} return false; }
    public synchronized boolean deleteConnector(int id){ return connectors.removeIf(c -> c.getConnectorId()==id); }

    public synchronized List<Booking> listBookingsFor(User user){ List<Booking> out=new ArrayList<Booking>(); for(Booking b:bookings) if(AuthUtil.isAdmin(user)||b.getDriverUsername().equals(user.getUsername())) out.add(b); return out; }
    public synchronized Booking getBooking(int id){ for(Booking b:bookings) if(b.getBookingId()==id) return b; return null; }
    public synchronized String validateBooking(Booking b, Integer ignoreBookingId) {
        if (getStation(b.getStationId()) == null) return "Station does not exist";
        Connector connector = getConnector(b.getConnectorId());
        if (connector == null || connector.getStationId() != b.getStationId()) return "Connector does not exist for this station";
        if (b.getStartTime() == null || b.getEndTime() == null || b.getDate() == null || b.getStartTime().compareTo(b.getEndTime()) >= 0) return "Invalid date/time";
        for (Booking existing: bookings) {
            if (ignoreBookingId != null && existing.getBookingId() == ignoreBookingId.intValue()) continue;
            if (!"ACTIVE".equals(existing.getBookingStatus())) continue;
            boolean sameConnector = existing.getConnectorId() == b.getConnectorId() && existing.getDate().equals(b.getDate());
            boolean timesOverlap = b.getStartTime().compareTo(existing.getEndTime()) < 0 && b.getEndTime().compareTo(existing.getStartTime()) > 0;
            if (sameConnector && timesOverlap) return "This connector is already booked during that time";
            boolean sameDriver = existing.getDriverUsername().equals(b.getDriverUsername()) && existing.getDate().equals(b.getDate());
            if (sameDriver && timesOverlap) return "Driver already has an overlapping booking";
        }
        return null;
    }
    public synchronized Booking addBooking(Booking b){ String err=validateBooking(b,null); if(err!=null) throw new IllegalArgumentException(err); b.setBookingId(nextBookingId++); b.setBookingStatus("ACTIVE"); bookings.add(b); return b; }
    public synchronized boolean updateBooking(int id, Booking updated, User user){ Booking old=getBooking(id); if(old==null) return false; if(!AuthUtil.isAdmin(user)&&!old.getDriverUsername().equals(user.getUsername())) throw new SecurityException(); if(hasStarted(old)) throw new IllegalStateException("Booking already started"); updated.setBookingId(id); updated.setDriverUsername(AuthUtil.isAdmin(user) && updated.getDriverUsername()!=null ? updated.getDriverUsername() : old.getDriverUsername()); updated.setBookingStatus("ACTIVE"); String err=validateBooking(updated,id); if(err!=null) throw new IllegalArgumentException(err); bookings.set(bookings.indexOf(old), updated); return true; }
    public synchronized boolean cancelBooking(int id, User user){ Booking b=getBooking(id); if(b==null) return false; if(!AuthUtil.isAdmin(user)&&!b.getDriverUsername().equals(user.getUsername())) throw new SecurityException(); if(hasStarted(b)) throw new IllegalStateException("Booking already started"); b.setBookingStatus("CANCELLED"); return true; }
    private boolean hasStarted(Booking b){ try { return LocalDateTime.now().isAfter(LocalDateTime.parse(b.getDate()+"T"+b.getStartTime())); } catch(Exception e){ return false; } }
}
