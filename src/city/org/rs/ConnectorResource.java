package city.org.rs;

import java.net.URI;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/connectors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectorResource {
    private AppDAO dao = AppDAO.getInstance();
    @GET public Response list(@QueryParam("stationId") Integer stationId){ return Response.ok(dao.listConnectors(stationId)).build(); }
    @POST public Response add(Connector c, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); Connector saved=dao.addConnector(c); return Response.created(URI.create("/connectors/"+saved.getConnectorId())).entity(saved).build(); }
    @PUT @Path("/{id}") public Response update(@PathParam("id") int id, Connector c, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); return dao.updateConnector(id,c)?Response.ok(c).build():Response.status(404).build(); }
    @DELETE @Path("/{id}") public Response delete(@PathParam("id") int id, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u))return AuthUtil.forbidden(); return dao.deleteConnector(id)?Response.ok(new ApiResponse("Connector deleted")).build():Response.status(404).build(); }
}
