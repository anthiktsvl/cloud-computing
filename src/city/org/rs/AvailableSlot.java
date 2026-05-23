package city.org.rs;

public class AvailableSlot {
    private int slotId;
    private int stationId;
    private int connectorId;
    private String date;
    private String startTime;
    private String endTime;
    private boolean configuredAvailable;
    private boolean freeNow;
    private boolean available;

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

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

    public boolean isConfiguredAvailable() { return configuredAvailable; }
    public void setConfiguredAvailable(boolean configuredAvailable) { this.configuredAvailable = configuredAvailable; }

    public boolean isFreeNow() { return freeNow; }
    public void setFreeNow(boolean freeNow) { this.freeNow = freeNow; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
