package org.alex73.korpus.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test")
public class TestImpl {
    @Path("initial")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getInitialData() throws Exception {
        return "zzzz";
    }
}
