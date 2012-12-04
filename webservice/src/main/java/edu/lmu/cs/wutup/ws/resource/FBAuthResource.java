package edu.lmu.cs.wutup.ws.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.lmu.cs.wutup.ws.exception.NoSuchUserException;
import edu.lmu.cs.wutup.ws.model.User;
import edu.lmu.cs.wutup.ws.service.FBAuthService;
import edu.lmu.cs.wutup.ws.service.UserService;

@Component
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/auth")
public class FBAuthResource {

    @Autowired
    UserService userService;
    
    @Autowired
    FBAuthService fbService;

    @GET
    @Path("/{facebookId}")
    public Response retrieveUserByFacebookId(@DefaultValue("") @PathParam("facebookId") String facebookId) {

        try {
            return Response
                    .ok(userService.findUserByFacebookId(facebookId))
                    .build();

        } catch (NoSuchUserException e) {
            return Response
                    .ok("{}")
                    .build();
        }
    }

    @GET
    @Path("/facebook")
    public Response authenticate(
            @DefaultValue("") @QueryParam("state") String state,
            @DefaultValue("") @QueryParam("code") String code,
            @DefaultValue("") @QueryParam("error_reason") String errorReason,
            @DefaultValue("") @QueryParam("error") String error,
            @DefaultValue("") @QueryParam("error_description") String errorDescription) {

        final String redirectUri = "http://localhost:8080/wutup/auth/facebook";

        if (!error.equals("")) {
            return Response.status(Response.Status.UNAUTHORIZED).build();

        } else if (code.equals("")) {
            try {
                return fbService.fetchFBCode(redirectUri);

            } catch (Exception e) {
                e.printStackTrace();
                return Response.serverError().build();
            }
        }

        try {
            final String accessToken = fbService.getAccessToken(code, redirectUri);
            User u = fbService.findOrCreateFBUser(accessToken, fbService.getUserIdFromFB(fbService.getFBUser(accessToken)));
            return Response
                    .ok(u)
                    .build();

        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/facebook/sync")
    public Response syncEventsWithFB(
            @DefaultValue("") @QueryParam("state") String state,
            @DefaultValue("") @QueryParam("code") String code,
            @DefaultValue("") @QueryParam("error_reason") String errorReason,
            @DefaultValue("") @QueryParam("error") String error,
            @DefaultValue("") @QueryParam("error_description") String errorDescription) {

        final String redirectUri = "http://localhost:8080/wutup/auth/facebook/sync";

        if (!error.equals("")) {
            return Response.status(Response.Status.UNAUTHORIZED).build();

        } else if (code.equals("")) {
            try {
                return fbService.fetchFBCode(redirectUri);

            } catch (Exception e) {
                e.printStackTrace();
                return Response.serverError().build();
            }
        }

        try {
            return Response
                    .ok(fbService.syncUser(fbService.getAccessToken(code, redirectUri)))
                    .build();

        } catch (Exception e) {
//            e.printStackTrace();
            return Response.serverError().build();
        }
    }
}
