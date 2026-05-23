package city.org.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/slots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SlotResource {
    private final AppDAO dao = AppDAO.getInstance();

    @GET
    @Path("/available")
    public Response listAvailable(
            @QueryParam("stationId") Integer stationId,
            @QueryParam("connectorId") Integer connectorId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime
    ) {
        return Response.ok(dao.listAvailableSlots(stationId, connectorId, date, startTime, endTime)).build();
    }
}
