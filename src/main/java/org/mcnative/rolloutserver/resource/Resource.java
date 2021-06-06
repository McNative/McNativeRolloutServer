package org.mcnative.rolloutserver.resource;

import io.javalin.Javalin;
import net.pretronic.libraries.resourceloader.VersionInfo;
import net.pretronic.libraries.utility.io.FileUtil;
import org.mcnative.rolloutserver.config.RolloutServerConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Resource {

    private static final String LATEST_URL = "https://mirror.mcnative.org/v1/{id}/versions/latest?plain=true";
    private static final String DOWNLOAD_URL = "https://mirror.mcnative.org/v1/{id}/versions/{build}/download";

    private final UUID id;
    private Map<String,String> latestVersionInfos;

    public Resource(UUID id) {
        this.id = id;
        latestVersionInfos = new ConcurrentHashMap<>();
    }

    public UUID getId() {
        return id;
    }

    public String getLatestVersionInfo(String qualifier) {
        if(!latestVersionInfos.containsKey(qualifier) && !lookupLatestVersion(qualifier)) return "Unknown";
        return latestVersionInfos.get(qualifier);
    }

    public VersionInfo getLatestVersionInfoAsInfo(String qualifier) {
        String info = getLatestVersionInfo(qualifier);
        return VersionInfo.parse(info.split(";")[0]);
    }

    private File buildLocation(VersionInfo info){
        return buildLocation(info.getBuild());
    }

    private File buildLocation(int build){
        return new File(RolloutServerConfig.RESOURCE_FOLDER,id+"/resource-"+build+".jar");
    }

    private File buildLatestLocation(String qualifier){
        return new File(RolloutServerConfig.RESOURCE_FOLDER,id+"/latest-"+qualifier+".txt");
    }

    public boolean download(VersionInfo info) {
        return download(info.getBuild());
    }

    public boolean download(int build) {
        File file = buildLocation(build);
        if(file.exists()) return true;
        try {
            Javalin.log.info("Downloading "+id+" (Build: "+build+")");

            String url = DOWNLOAD_URL.replace("{id}",id.toString()).replace("{build}",String.valueOf(build));

            HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("User-Agent", "McNative Rollout Server");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty("rolloutServerId",RolloutServerConfig.ID);
            connection.setRequestProperty("rolloutServerSecret",RolloutServerConfig.SECRET);

            connection.connect();
            if (connection.getResponseCode() != 200) {
                Javalin.log.error("Downloading "+id+" (Build: "+build+") failed");
                Javalin.log.error("Error: "+connection.getResponseCode()+" - "+connection.getResponseMessage());
                return false;
            } else {
                file.getParentFile().mkdirs();
                Files.copy(connection.getInputStream(), file.toPath());
                Javalin.log.info("Downloaded "+id+" (Build: "+build+") successfully");
                return true;
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return false;
    }

    public boolean lookupLatestVersion(String qualifier) {
        File file = buildLatestLocation(qualifier);
        if(file.exists()){
            latestVersionInfos.put(qualifier,FileUtil.readContent(FileUtil.newFileInputStream(file)));
            return true;
        }
        try {
            Javalin.log.info("Looking up latest version for "+id);

            String url = LATEST_URL.replace("{id}",id.toString());

            HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("User-Agent", "McNative Rollout Server");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty("rolloutServerId",RolloutServerConfig.ID);
            connection.setRequestProperty("rolloutServerSecret",RolloutServerConfig.SECRET);

            connection.connect();
            if (connection.getResponseCode() != 200) {
                Javalin.log.error("Could not lookup latest version for "+id+": ");
                Javalin.log.error("Error: "+connection.getResponseCode()+" - "+connection.getResponseMessage());
                return false;
            } else {
                file.getParentFile().mkdirs();
                Files.copy(connection.getInputStream(), file.toPath());
                String latestVersionInfo = FileUtil.readContent(FileUtil.newFileInputStream(file));
                latestVersionInfos.put(qualifier,latestVersionInfo);
                Javalin.log.info("Found latest version for "+id+": "+latestVersionInfo);
                return true;
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return false;
    }

    public void refreshVersions(){
        for (String qualifier : latestVersionInfos.keySet()) lookupLatestVersion(qualifier);
    }

    public InputStream getResourceData(int buildNumber) throws IOException {
        File file = buildLocation(buildNumber);

        if(!file.exists()){
            if(!download(buildNumber)) return null;
        }

        return Files.newInputStream(file.toPath());
    }

}
