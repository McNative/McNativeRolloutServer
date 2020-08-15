package org.mcnative.rolloutserver.config;

import net.pretronic.libraries.document.annotations.DocumentIgnored;
import net.pretronic.libraries.document.annotations.DocumentKey;
import net.pretronic.libraries.document.annotations.OnDocumentConfigurationLoad;

import java.io.File;

public class RolloutServerConfig {

    public static String ID = "00000-00000-00000";

    public static String SECRET = "00000-00000-00000";

    @DocumentKey("resourceFolder")
    public static String RESOURCE_FOLDER_NAME ="resources/";

    @DocumentIgnored
    public static File RESOURCE_FOLDER = new File("resources/");

    public static String HOST = "0.0.0.0";

    public static int PORT = 8090;

    @OnDocumentConfigurationLoad
    public static void onLoad(){
        RESOURCE_FOLDER = new File(RESOURCE_FOLDER_NAME);
        if(!RESOURCE_FOLDER.exists()) RESOURCE_FOLDER.mkdirs();
    }
}