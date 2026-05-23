package city.org.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        User user = AppDAO.getInstance().authenticate(request.getUsername(), request.getPassword());
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponse("Invalid username or password")).build();
        User safeUser = new User(user.getUsername(), null, user.getRole());
        return Response.ok(safeUser).build();
    }
}
