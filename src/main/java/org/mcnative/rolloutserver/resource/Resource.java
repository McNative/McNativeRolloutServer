package org.mcnative.rolloutserver.resource;

import io.javalin.Javalin;
import net.pretronic.libraries.resourceloader.VersionInfo;
import net.pretronic.libraries.utility.Iterators;
import org.mcnative.rolloutserver.config.RolloutServerConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.UUID;

public class Resource {

    private static final String DOWNLOAD_URL = "https://mirror.mcnative.org/v1/{id}/versions/{build}/download";

    private final String name;
    private final UUID id;
    private final Collection<String> editions;
    private final Collection<ResourceProfile> profiles;

    public Resource(String name, UUID id, Collection<String> editions, Collection<ResourceProfile> profiles) {
        this.name = name;
        this.id = id;
        this.editions = editions;
        this.profiles = profiles;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public Collection<String> getEditions() {
        return editions;
    }

    public Collection<ResourceProfile> getProfiles() {
        return profiles;
    }

    public ResourceProfile getProfile(String name){
        return Iterators.findOne(this.profiles, profile -> profile.getName().equalsIgnoreCase(name));
    }

    public boolean isVersionAvailable(VersionInfo info){
        return isVersionAvailable(info,null);
    }

    public boolean isVersionAvailable(VersionInfo info,String edition){
        return buildLocation(info,edition).exists();
    }

    private File buildLocation(VersionInfo info,String edition){
        return buildLocation(info.getBuild(),edition);
    }

    private File buildLocation(int build,String edition){
        if(edition == null) edition = "default";
        return new File(RolloutServerConfig.RESOURCE_FOLDER,id+"/resource-"+build+"-"+edition+".jar");
    }

    public boolean download(VersionInfo info) {
        return download(info,null);
    }

    public boolean download(VersionInfo info,String edition) {
        return download(info.getBuild(),edition);
    }

    public boolean download(int build,String edition) {
        try {
            Javalin.log.info("Downloading "+name+" (Build: "+build+")");

            String url = DOWNLOAD_URL.replace("{id}",id.toString()).replace("{build}",String.valueOf(build));
            if(edition != null) url +="?edition="+edition;

            HttpURLConnection connection = (HttpURLConnection)(new URL(url)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setRequestProperty("User-Agent", "McNative Rollout Server");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty("rolloutId",RolloutServerConfig.ID);
            connection.setRequestProperty("rolloutSecret",RolloutServerConfig.SECRET);

            connection.connect();
            if (connection.getResponseCode() != 200) {
                Javalin.log.error("Downloading "+name+" (Build: "+build+", Edition: "+edition+") failed");
                Javalin.log.error("Error: "+connection.getResponseCode()+" - "+connection.getResponseMessage());
                return false;
            } else {
                File file = buildLocation(build,edition);
                file.getParentFile().mkdirs();
                Files.copy(connection.getInputStream(), file.toPath());
                Javalin.log.info("Downloaded "+name+" (Build: "+build+", Edition: "+edition+") successfully");
                return true;
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return false;
    }

    public void downloadInstallVersions(){
        for (ResourceProfile profile : getProfiles()) {
            VersionInfo installVersion = profile.getInstallVersion();

            for (String edition : getEditions()) {
                if(!isVersionAvailable(installVersion,edition)){
                    download(installVersion,edition);
                }
            }
        }
    }

    public InputStream getResourceData(int buildNumber,String edition) throws IOException {
        File file = buildLocation(buildNumber,edition);

        if(!file.exists()){
            if(!download(buildNumber, edition)) return null;
        }

        return Files.newInputStream(file.toPath());
    }

}
