package org.alex73.korpus.server;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/localization")
public class LocalizationServiceImpl {
    private final static Logger LOGGER = Logger.getLogger(LocalizationServiceImpl.class.getName());

    @Context
    HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getLocalization() throws Exception {
        LOGGER.info("getLocalization from " + request.getRemoteAddr());
        try {
            return ApplicationWeb.instance.localization;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "getLocalization", ex);
            throw ex;
        }
    }
}
