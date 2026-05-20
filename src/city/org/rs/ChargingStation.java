package city.org.rs;

public class ChargingStation {
    private int stationId;
    private String stationName;
    private String address;
    private double latitude;
    private double longitude;

    public ChargingStation() {}
    public ChargingStation(int stationId, String stationName, String address, double latitude, double longitude) {
        this.stationId = stationId; this.stationName = stationName; this.address = address;
        this.latitude = latitude; this.longitude = longitude;
    }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
