package org.mcnative.rolloutserver.route.v1;

import io.javalin.Javalin;
import net.pretronic.libraries.document.type.DocumentFileType;
import org.mcnative.licensing.License;
import org.mcnative.licensing.exceptions.CloudNotCheckoutLicenseException;
import org.mcnative.rolloutserver.ServerAuthenticator;
import org.mcnative.rolloutserver.license.LicenseController;
import org.mcnative.rolloutserver.profile.Profile;
import org.mcnative.rolloutserver.profile.ProfileController;
import org.mcnative.rolloutserver.template.Template;

public class ProfileRoute {

    private final ProfileController profileController;
    private final ServerAuthenticator authenticator;

    public ProfileRoute(Javalin app, ProfileController profileController, ServerAuthenticator authenticator) {
        this.profileController = profileController;
        this.authenticator = authenticator;
        setupRoutes(app);
    }

    private void setupRoutes(Javalin app){
        app.get("v1/profiles/:profileName", context -> {
            String id = context.pathParam("profileName");
            if (authenticator.authenticate(context)) return;

            try{
                Profile profile = profileController.getProfile(id);
                if(profile == null){
                    context.res.sendError(404,"Profile not found");
                    return;
                }

                String serverName = context.header("serverName");
                Javalin.log.info("Minecraft server ("+serverName+") from "+context.req.getRemoteAddr()+" pulled profile "+profile.getName());

                String plain0 = context.queryParam("plain");
                boolean plain = plain0 != null && plain0.equalsIgnoreCase("true");

                if(plain) context.result(profile.getDefinition());
                else context.result(DocumentFileType.JSON.getWriter().write(profile.getConfiguration()));

            }catch (CloudNotCheckoutLicenseException e){
                context.res.sendError(500,e.getMessage());
            }
        });
    }
}
