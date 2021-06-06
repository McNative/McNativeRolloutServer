package org.mcnative.rolloutserver.route.v1;

import io.javalin.Javalin;
import net.pretronic.libraries.document.type.DocumentFileType;
import org.mcnative.licensing.License;
import org.mcnative.licensing.exceptions.CloudNotCheckoutLicenseException;
import org.mcnative.rolloutserver.ServerAuthenticator;
import org.mcnative.rolloutserver.license.LicenseController;
import org.mcnative.rolloutserver.template.Template;
import org.mcnative.rolloutserver.template.TemplateController;

public class TemplateRoute {

    private final ServerAuthenticator authenticator;
    private final TemplateController templateController;

    public TemplateRoute(Javalin app, TemplateController templateController, ServerAuthenticator authenticator) {
        this.templateController = templateController;
        this.authenticator = authenticator;
        setupRoutes(app);
    }

    private void setupRoutes(Javalin app){
        app.get("v1/templates/:templateName", context -> {
            String id = context.pathParam("templateName");
            if (authenticator.authenticate(context)) return;

            try{
                Template template = templateController.getTemplate(id);
                if(template == null){
                    context.res.sendError(404,"Template not found");
                    return;
                }

                String serverName = context.header("serverName");
                Javalin.log.info("Minecraft server ("+serverName+") from "+context.req.getRemoteAddr()+" pulled template "+template.getName());

                String plain0 = context.queryParam("plain");
                boolean plain = plain0 != null && plain0.equalsIgnoreCase("true");

                if(plain) context.result(template.getDefinition());
                else context.result(DocumentFileType.JSON.getWriter().write(template.getConfiguration()));

            }catch (CloudNotCheckoutLicenseException e){
                context.res.sendError(500,e.getMessage());
            }
        });
    }
}
