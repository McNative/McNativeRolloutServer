package org.mcnative.rolloutserver.route.v1;

import io.javalin.Javalin;
import io.javalin.http.Context;
import net.pretronic.libraries.resourceloader.VersionInfo;
import org.jetbrains.annotations.NotNull;
import org.mcnative.licensing.License;
import org.mcnative.licensing.exceptions.CloudNotCheckoutLicenseException;
import org.mcnative.rolloutserver.ServerAuthenticator;
import org.mcnative.rolloutserver.license.LicenseController;
import org.mcnative.rolloutserver.resource.Resource;
import org.mcnative.rolloutserver.resource.ResourceController;
import org.mcnative.rolloutserver.resource.ResourceProfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LicenseRoute {

    private final LicenseController licenseController;
    private final ServerAuthenticator authenticator;

    public LicenseRoute(Javalin app, LicenseController licenseController, ServerAuthenticator authenticator) {
        this.licenseController = licenseController;
        this.authenticator = authenticator;
        setupRoutes(app);
    }

    private void setupRoutes(Javalin app){
        app.get("v1/licenses/:resourceId/checkout", context -> {
            String id = context.pathParam("resourceId");
            if (authenticator.authenticate(context)) return;

            String deviceId = context.header("deviceId");

            try{
                License license = licenseController.getLicense(id,deviceId);
                if(license == null){
                    context.res.sendError(404,"License not found");
                    return;
                }

                String serverName = context.header("serverName");
                Javalin.log.info("Minecraft server ("+serverName+") from "+context.req.getRemoteAddr()+" checked out license for "+license.getResourceId());

                context.result(license.getRaw());
            }catch (CloudNotCheckoutLicenseException e){
                context.res.sendError(500,e.getMessage());
            }
        });
    }
}
