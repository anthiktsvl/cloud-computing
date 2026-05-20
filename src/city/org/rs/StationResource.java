package city.org.rs;

import java.net.URI;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StationResource {
    private AppDAO dao = AppDAO.getInstance();
    @GET public Response list(){ return Response.ok(dao.listStations()).build(); }
    @GET @Path("/{id}") public Response get(@PathParam("id") int id){ ChargingStation s=dao.getStation(id); return s==null?Response.status(404).build():Response.ok(s).build(); }
    @POST public Response add(ChargingStation station, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); ChargingStation saved=dao.addStation(station); return Response.created(URI.create("/stations/"+saved.getStationId())).entity(saved).build(); }
    @PUT @Path("/{id}") public Response update(@PathParam("id") int id, ChargingStation station, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); return dao.updateStation(id,station)?Response.ok(station).build():Response.status(404).build(); }
    @DELETE @Path("/{id}") public Response delete(@PathParam("id") int id, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); return dao.deleteStation(id)?Response.ok(new ApiResponse("Station deleted")).build():Response.status(404).build(); }
}
