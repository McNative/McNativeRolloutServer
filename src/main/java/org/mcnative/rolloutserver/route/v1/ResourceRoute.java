package org.mcnative.rolloutserver.route.v1;

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
            if(id == null) return;
            if (authenticator.authenticate(context)) return;

            Resource resource = resourceController.getResource(id);
            if(resource == null){
                context.res.sendError(404,"Resource not found");
                return;
            }

            String qualifier = context.req.getParameter("qualifier");
            if(qualifier == null) qualifier = "RELEASE";

            ResourceProfile profile = resource.getProfile(qualifier);

            if(profile == null){
                context.res.sendError(404,"Rollout profile (qualifier) not found");
                return;
            }

            VersionInfo versionInfo = profile.getInstallVersion();

            String serverName = context.header("serverName");
            Javalin.log.info("Minecraft server ("+serverName+") from "+context.req.getRemoteAddr()+" checked latest version of "+resource.getName());

            context.result(versionInfo.getName()+";" + versionInfo.getBuild() + ";" + versionInfo.getQualifier());
        });

        app.get("v1/:resourceId/versions/:buildId/download", context -> {
            UUID id = readRequestResourceId(context);
            if(id == null) return;
            if (authenticator.authenticate(context)) return;

            Resource resource = resourceController.getResource(id);

            if(resource == null){
                context.res.sendError(404,"resource not found");
                return;
            }

            String edition = context.req.getParameter("edition");

            int buildId = context.pathParam("buildId",Integer.class).get();
            InputStream stream = resource.getResourceData(buildId,edition);
            if(stream == null){
                context.res.sendError(404,"Resource not loaded");
                return;
            }
            String serverName = context.header("serverName");
            Javalin.log.info("Minecraft server ("+serverName+") from "+context.req.getRemoteAddr()+" downloaded "+resource.getName()+" ["+buildId+"]");
            context.result(stream);
        });
    }

    private UUID readRequestResourceId(@NotNull Context context) throws IOException {
        try {
            return UUID.fromString(context.pathParam("resourceId"));
        }catch (Exception ignored){
            context.res.sendError(400,"Invalid resource id");
        }
        return null;
    }
}
