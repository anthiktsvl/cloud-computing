package city.org.rs;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public class AuthUtil {
    public static User authenticate(HttpHeaders headers) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Basic ")) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6)), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) return null;
            return AppDAO.getInstance().authenticate(parts[0], parts[1]);
        } catch (Exception e) { return null; }
    }
    public static Response unauthorized() { return Response.status(Response.Status.UNAUTHORIZED).entity(new ApiResponse("Login required or invalid credentials")).build(); }
    public static Response forbidden() { return Response.status(Response.Status.FORBIDDEN).entity(new ApiResponse("You are not allowed to perform this action")).build(); }
    public static boolean isAdmin(User user) { return user != null && "ADMIN".equals(user.getRole()); }
}
