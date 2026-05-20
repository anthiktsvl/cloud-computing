package city.org.rs;

import java.io.IOException;
import java.time.Instant;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String START_TIME = "start-time";
    @Override public void filter(ContainerRequestContext requestContext) throws IOException { requestContext.setProperty(START_TIME, System.currentTimeMillis()); }
    @Override public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        long start = (Long) req.getProperty(START_TIME);
        long elapsed = System.currentTimeMillis() - start;
        String instance = System.getenv("INSTANCE_ID"); if(instance == null) instance = "local-instance";
        System.out.println(Instant.now()+" method="+req.getMethod()+" uri="+req.getUriInfo().getRequestUri()+" status="+res.getStatus()+" timeMs="+elapsed+" instance="+instance);
        res.getHeaders().add("Access-Control-Allow-Origin", "*");
        res.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        res.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }
}
