package city.org.rs;

public class Connector {
    private int connectorId;
    private String connectorType;
    private int stationId;

    public Connector() {}
    public Connector(int connectorId, String connectorType, int stationId) {
        this.connectorId = connectorId; this.connectorType = connectorType; this.stationId = stationId;
    }
    public int getConnectorId() { return connectorId; }
    public void setConnectorId(int connectorId) { this.connectorId = connectorId; }
    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
}
