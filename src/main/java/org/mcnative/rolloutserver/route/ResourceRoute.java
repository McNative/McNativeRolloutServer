package org.mcnative.rolloutserver.route;

import io.javalin.Javalin;
import io.javalin.http.Context;
import net.pretronic.libraries.document.Document;
import net.pretronic.libraries.document.type.DocumentFileType;
import net.pretronic.libraries.resourceloader.VersionInfo;
import org.jetbrains.annotations.NotNull;
import org.mcnative.rolloutserver.ServerAuthenticator;
import org.mcnative.rolloutserver.resource.Resource;
import org.mcnative.rolloutserver.resource.ResourceController;
import org.mcnative.rolloutserver.resource.ResourceProfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ResourceRoute {

    private final ResourceController resourceController;
    private final ServerAuthenticator authenticator;

    public ResourceRoute(Javalin app, ResourceController resourceController, ServerAuthenticator authenticator) {
        this.resourceController = resourceController;
        this.authenticator = authenticator;
        setupRoutes(app);
    }

    private void setupRoutes(Javalin app){
        app.get("v1/:resourceId/versions/latest", context -> {
            UUID id = readRequestResourceId(context);
            if (authenticate(context)) return;

            if(id == null){
                context.res.sendError(404);
                return;
            }

            Resource resource = resourceController.getResource(id);
            if(resource == null){
                context.res.sendError(404);
                return;
            }

            String qualifier = context.req.getParameter("qualifier");
            if(qualifier == null) qualifier = "RELEASE";

            ResourceProfile profile = resource.getProfile(qualifier);

            if(profile == null){
                context.res.sendError(404);
                return;
            }

            VersionInfo versionInfo = profile.getInstallVersion();

            context.result(versionInfo.getName()+";" + versionInfo.getBuild() + ";" + versionInfo.getQualifier());
        });


        app.get("v1/:resourceId/versions/:buildId/download", context -> {
            UUID id = readRequestResourceId(context);
            if(id == null) return;
            if (authenticate(context)) return;

            Resource resource = resourceController.getResource(id);

            if(resource == null){
                context.res.sendError(404);
                return;
            }

            String edition = context.req.getParameter("edition");

            InputStream stream = resource.getResourceData(context.pathParam("buildId",Integer.class).get(),edition);
            if(stream == null){
                context.res.sendError(404);
                return;
            }
            context.result(stream);
        });
    }

    private boolean authenticate(Context context) throws IOException {
        String serverId =context.header("serverId");
        String serverSecret =context.header("serverSecret");

        if(serverId == null || serverSecret == null){
            context.res.sendError(400);
            return true;
        }

        if(!authenticator.isValid(serverId,serverSecret)){
            context.res.sendError(401);
            return true;
        }
        return false;
    }

    private UUID readRequestResourceId(@NotNull Context context) throws IOException {
        try {
            return UUID.fromString(context.pathParam("resourceId"));
        }catch (Exception ignored){
            context.res.sendError(400);
        }
        return null;
    }


}
