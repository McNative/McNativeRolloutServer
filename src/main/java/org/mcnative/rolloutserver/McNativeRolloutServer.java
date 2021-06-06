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
import org.mcnative.rolloutserver.profile.Profile;
import org.mcnative.rolloutserver.profile.ProfileController;
import org.mcnative.rolloutserver.resource.ResourceController;
import org.mcnative.rolloutserver.route.v1.*;
import org.mcnative.rolloutserver.template.Template;
import org.mcnative.rolloutserver.template.TemplateController;
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

    private final ProfileController profileController;
    private final TemplateController templateController;
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

        this.profileController = new ProfileController();
        this.templateController = new TemplateController();
        this.resourceController = new ResourceController(profileController);
        this.licenseController = new LicenseController();
        this.authenticator = new ServerAuthenticator();

        new TemplateRoute(this.app,this.templateController,this.authenticator);
        new ProfileRoute(this.app,this.profileController,this.authenticator);
        new ResourceRoute(this.app,this.resourceController,this.authenticator);
        new LicenseRoute(this.app,this.licenseController,this.authenticator);
        new PingRoute(this.app);
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
        if(deploy != null) readDeploy(deploy);

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
                .interval(10,TimeUnit.MINUTES)
                .execute(() -> {
                    Document deploy = DeployHandler.checkoutDeployConfiguration();
                    if(deploy != null) readDeploy(deploy);
                    this.resourceController.downloadDeployedVersions();
                    this.licenseController.cleanExpiredLicenses();
                    this.resourceController.lookupVersions();
        }).addListener(task -> {
            if(task.isFailed()) Javalin.log.error("",task.getThrowable());
        });
    }

    private void readDeploy(Document deploy){
        List<Profile> profiles = deploy.getObject("profiles",new TypeReference<List<Profile>>(){}.getType());
        List<Template> templates = deploy.getObject("templates",new TypeReference<List<Template>>(){}.getType());
        List<ServerAuthenticator.ServerCredential> credentials = deploy.getObject("credentials",new TypeReference<List<ServerAuthenticator.ServerCredential>>(){}.getType());

        templateController.setTemplates(templates != null ? templates : new ArrayList<>());
        profileController.setProfiles(profiles != null ? profiles : new ArrayList<>());
        authenticator.setCredentials(credentials != null ? credentials : new ArrayList<>());
    }

    private static SslContextFactory getSslContextFactory() {
        SslContextFactory sslContextFactory =  new SslContextFactory.Client();
        sslContextFactory.setKeyStorePath(McNativeRolloutServer.class.getResource("/keystore.jks").toExternalForm());
        sslContextFactory.setKeyStorePassword("McNative");
        return sslContextFactory;
    }
}
