package org.mcnative.rolloutserver;

import io.javalin.Javalin;
import net.pretronic.libraries.document.Document;
import net.pretronic.libraries.document.type.DocumentFileType;
import org.mcnative.rolloutserver.config.RolloutServerConfig;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeployHandler {

    private static final String CONFIGURATION_CHECKOUT_URL = "https://api.mcnative.org/rollout/{serverId}/checkout";

    private static final File DEPLOY_CONFIGURATION_LOCATION = new File("deploy.dat");

    public static Document loadDeployConfiguration(){
        Javalin.log.info("Loading deploy configuration");
        if(DEPLOY_CONFIGURATION_LOCATION.exists()){
            return DocumentFileType.JSON.getReader().read(DEPLOY_CONFIGURATION_LOCATION);
        }
        Javalin.log.info("Loaded deploy configuration successfully");
        return Document.newDocument();
    }

    public static Document checkoutDeployConfiguration(){
        Javalin.log.info("Checking for new deploy configuration");

        try{
            HttpURLConnection connection = (HttpURLConnection)(new URL(CONFIGURATION_CHECKOUT_URL
                    .replace("{serverId}", RolloutServerConfig.ID))).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Pretronic Resource Loader");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty("secret",RolloutServerConfig.SECRET);
            connection.setRequestProperty("rolloutServerEndpoint", "https://"+RolloutServerConfig.HOST+":"+RolloutServerConfig.PORT);

            connection.connect();
            if (connection.getResponseCode() != 200) {
                Javalin.log.error("Could not check latest deploy configuration");
                Javalin.log.error("Error: "+connection.getResponseCode()+" - "+connection.getResponseMessage());
            } else {
                Document configuration = DocumentFileType.JSON.getReader().read(connection.getInputStream());
                DocumentFileType.JSON.getWriter().write(DEPLOY_CONFIGURATION_LOCATION,configuration,false);
                Javalin.log.info("Loaded deploy configuration successfully");
                return configuration;
            }
        }catch (Exception exception){
            Javalin.log.error("Could not check latest deploy configuration");
            Javalin.log.error("Error: "+exception.getMessage());
        }
        return null;
    }

}
