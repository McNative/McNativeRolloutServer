package org.mcnative.rolloutserver;

import io.javalin.Javalin;
import net.pretronic.libraries.concurrent.TaskScheduler;
import net.pretronic.libraries.concurrent.simple.SimpleTaskScheduler;
import net.pretronic.libraries.document.Document;
import net.pretronic.libraries.document.DocumentRegistry;
import net.pretronic.libraries.document.type.DocumentFileType;
import net.pretronic.libraries.resourceloader.VersionInfo;
import net.pretronic.libraries.utility.interfaces.ObjectOwner;
import net.pretronic.libraries.utility.reflect.TypeReference;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.mcnative.rolloutserver.config.RolloutServerConfig;
import org.mcnative.rolloutserver.license.LicenseController;
import org.mcnative.rolloutserver.resource.Resource;
import org.mcnative.rolloutserver.resource.ResourceController;
import org.mcnative.rolloutserver.resource.ResourceProfile;
import org.mcnative.rolloutserver.route.v1.LicenseRoute;
import org.mcnative.rolloutserver.route.v1.ResourceRoute;
import org.mcnative.rolloutserver.utils.VersionInfoDocumentAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class McNativeRolloutServer {

    static {
        DocumentRegistry.getDefaultContext().registerAdapter(VersionInfo.class,new VersionInfoDocumentAdapter());
    }

    private final Javalin app;
    private final TaskScheduler scheduler;

    private final ResourceController resourceController;
    private final LicenseController licenseController;
    private final ServerAuthenticator authenticator;

    public McNativeRolloutServer(){
        Javalin.log.info("");
        Javalin.log.info("    __  ___       _   __        __   _            ");
        Javalin.log.info("   /  |/  /_____ / | / /____ _ / /_ (_)_   __ ___ ");
        Javalin.log.info("  / /|_/ // ___//  |/ // __ `// __// /| | / // _ \\");
        Javalin.log.info(" / /  / // /__ / /|  // /_/ // /_ / / | |/ //  __/");
        Javalin.log.info("/_/  /_/ \\___//_/ |_/ \\__,_/ \\__//_/  |___/ \\___/");
        Javalin.log.info("                                    Rollout Server");
        Javalin.log.info("");

        loadConfig();
        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.enforceSsl = true;
            config.server(() -> {
                Server server = new Server();
                ServerConnector sslConnector = new ServerConnector(server, getSslContextFactory());
                sslConnector.setPort(RolloutServerConfig.PORT);
                sslConnector.setHost(RolloutServerConfig.HOST);
                server.setConnectors(new Connector[]{sslConnector});
                return server;
            });
        });

        this.scheduler = new SimpleTaskScheduler();

        this.resourceController = new ResourceController();
        this.licenseController = new LicenseController();
        this.authenticator = new ServerAuthenticator();

        new ResourceRoute(this.app,this.resourceController,this.authenticator);
        new LicenseRoute(this.app,this.licenseController,this.authenticator);
    }

    public void start(){
        if(RolloutServerConfig.ID.equalsIgnoreCase("00000-00000-00000")
                || RolloutServerConfig.SECRET.equalsIgnoreCase("00000-00000-00000")){
            Javalin.log.info("----------------------------------");
            Javalin.log.info("No rollout id or secret defined");
            Javalin.log.info("----------------------------------");
            System.exit(0);
            return;
        }

        Document deploy = DeployHandler.loadDeployConfiguration();
        readDeploy(deploy);

        startDeployListener();

        app.start();
    }

    private void loadConfig(){
        File file = new File("config.yml");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }

        Document document = DocumentFileType.YAML.getReader().read(file);
        Document.loadConfigurationClass(RolloutServerConfig.class,document,true);
        DocumentFileType.YAML.getWriter().write(file,document,true);
    }

    private void startDeployListener(){
        scheduler.createTask(ObjectOwner.SYSTEM)
                .delay(5, TimeUnit.SECONDS)
                .interval(5,TimeUnit.MINUTES)
                .execute(() -> {
                    Document result = DeployHandler.checkoutDeployConfiguration();
                    if(result != null ) readDeploy(result);
                    this.resourceController.downloadDeployedResourceVersions();
                    this.licenseController.cleanExpiredLicenses();
        }).addListener(task -> {
            if(task.isFailed()) Javalin.log.error("",task.getThrowable());
        });
    }

    private void readDeploy(Document deploy){
        List<Resource> resources = deploy.getObject("configuration.resources",new TypeReference<List<Resource>>(){}.getType());
        List<ServerAuthenticator.ServerCredential> credentials = deploy.getObject("credentials",new TypeReference<List<ServerAuthenticator.ServerCredential>>(){}.getType());

        resourceController.setResources(resources != null ? resources : new ArrayList<>());
        authenticator.setCredentials(credentials != null ? credentials : new ArrayList<>());

        if(resources != null){
            Javalin.log.info("-------------------------");
            for (Resource resource : resources) {
                Javalin.log.info(resource.getName()+" ["+resource.getId()+"] ");
                for (ResourceProfile profile : resource.getProfiles()) {
                    Javalin.log.info(" -> "+profile.getName()+" "+profile.getInstallVersion().getName());
                }
            }
            Javalin.log.info("-------------------------");
        }
    }

    private static SslContextFactory getSslContextFactory() {
        SslContextFactory sslContextFactory =  new SslContextFactory.Client();
        sslContextFactory.setKeyStorePath(McNativeRolloutServer.class.getResource("/keystore.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("McNative");
        return sslContextFactory;
    }
}
