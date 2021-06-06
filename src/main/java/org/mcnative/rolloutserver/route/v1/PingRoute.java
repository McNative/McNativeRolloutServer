package org.mcnative.rolloutserver.route.v1;

import io.javalin.Javalin;
import net.pretronic.libraries.document.type.DocumentFileType;
import org.mcnative.licensing.exceptions.CloudNotCheckoutLicenseException;
import org.mcnative.rolloutserver.ServerAuthenticator;
import org.mcnative.rolloutserver.license.LicenseController;
import org.mcnative.rolloutserver.profile.Profile;
import org.mcnative.rolloutserver.profile.ProfileController;

public class PingRoute {

    public PingRoute(Javalin app) {
        setupRoutes(app);
    }

    private void setupRoutes(Javalin app){
        app.get("v1/ping", context -> context.status(200));
    }
}
