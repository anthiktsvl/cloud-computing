package city.org.rs;

public class Booking {
    private int bookingId;
    private String driverUsername;
    private int stationId;
    private int connectorId;
    private String date;      // yyyy-MM-dd
    private String startTime; // HH:mm
    private String endTime;   // HH:mm
    private String bookingStatus; // ACTIVE or CANCELLED

    public Booking() {}
    public Booking(int bookingId, String driverUsername, int stationId, int connectorId, String date, String startTime, String endTime, String bookingStatus) {
        this.bookingId = bookingId; this.driverUsername = driverUsername; this.stationId = stationId;
        this.connectorId = connectorId; this.date = date; this.startTime = startTime; this.endTime = endTime;
        this.bookingStatus = bookingStatus;
    }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getDriverUsername() { return driverUsername; }
    public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public int getConnectorId() { return connectorId; }
    public void setConnectorId(int connectorId) { this.connectorId = connectorId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
}
