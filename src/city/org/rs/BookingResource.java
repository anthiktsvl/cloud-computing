package city.org.rs;

import java.net.URI;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {
    private AppDAO dao = AppDAO.getInstance();
    @GET public Response list(@Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); return Response.ok(dao.listBookingsFor(u)).build(); }
    @POST public Response add(Booking b, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); if(!AuthUtil.isAdmin(u)) b.setDriverUsername(u.getUsername()); try{ Booking saved=dao.addBooking(b); return Response.created(URI.create("/bookings/"+saved.getBookingId())).entity(saved).build(); }catch(IllegalArgumentException e){ return Response.status(409).entity(new ApiResponse(e.getMessage())).build(); } }
    @PUT @Path("/{id}") public Response update(@PathParam("id") int id, Booking b, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); try{ return dao.updateBooking(id,b,u)?Response.ok(dao.getBooking(id)).build():Response.status(404).build(); }catch(SecurityException e){return AuthUtil.forbidden();}catch(IllegalStateException e){return Response.status(409).entity(new ApiResponse(e.getMessage())).build();}catch(IllegalArgumentException e){return Response.status(409).entity(new ApiResponse(e.getMessage())).build();} }
    @DELETE @Path("/{id}") public Response cancel(@PathParam("id") int id, @Context HttpHeaders headers){ User u=AuthUtil.authenticate(headers); if(u==null)return AuthUtil.unauthorized(); try{ return dao.cancelBooking(id,u)?Response.ok(new ApiResponse("Booking cancelled")).build():Response.status(404).build(); }catch(SecurityException e){return AuthUtil.forbidden();}catch(IllegalStateException e){return Response.status(409).entity(new ApiResponse(e.getMessage())).build();} }
}
