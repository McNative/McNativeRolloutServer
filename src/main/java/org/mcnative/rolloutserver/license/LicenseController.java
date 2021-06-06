package org.mcnative.rolloutserver.license;

import io.javalin.Javalin;
import net.pretronic.libraries.utility.Iterators;
import net.pretronic.libraries.utility.io.IORuntimeException;
import org.mcnative.licensing.License;
import org.mcnative.licensing.exceptions.CloudNotCheckoutLicenseException;
import org.mcnative.rolloutserver.config.RolloutServerConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class LicenseController {

    private static final String CHECKOUT_URL = "https://mirror.mcnative.org/v1/licenses/{resourceId}/checkout";
    private final Collection<License> licenses;

    public LicenseController() {
        this.licenses = new ArrayList<>();
        this.loadLicenses();
    }

    public License getLicense(String resourceId, String deviceId){
        License license = Iterators.findOne(this.licenses, license1 ->
                license1.getResourceId().equals(resourceId) && license1.getDeviceId().equals(deviceId));

        if(license == null){
            return checkoutLicense(resourceId, deviceId);
        }else if(license.shouldRefresh()){
            try {
                License newLicense = checkoutLicense(resourceId, deviceId);
                this.licenses.remove(license);
                return newLicense;
            }catch (IORuntimeException | IllegalArgumentException ignored){}
        }
        return license;
    }

    public void cleanExpiredLicenses(){
        List<License> licenses =  Iterators.remove(this.licenses, License::isExpired);
        for (License license : licenses) {
            new File(RolloutServerConfig.LICENSE_FOLDER,license.getId()+".dat").delete();
        }
       if(licenses.size() > 0){
           Javalin.log.error("Cleaned "+licenses.size() +" expired licenses");
       }
    }

    private void loadLicenses(){
        if(RolloutServerConfig.LICENSE_FOLDER.exists()){
            File[] files = RolloutServerConfig.LICENSE_FOLDER.listFiles();
            if(files != null){
                for (File file : files) {
                    try {
                        licenses.add(License.read(file));
                    } catch (IOException e) {
                        Javalin.log.error("Could not load license file "+file.getName(),e);
                    }
                }
            }
        }
    }

    private License checkoutLicense(String resourceId, String deviceId){
        Javalin.log.info("Checking out license for "+resourceId);
        try{
            HttpURLConnection connection = (HttpURLConnection)(new URL(CHECKOUT_URL
                    .replace("{resourceId}", resourceId))).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "McNative Rollout Server");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            connection.setRequestProperty("deviceId",deviceId);
            connection.setRequestProperty("rolloutId",RolloutServerConfig.ID);
            connection.setRequestProperty("rolloutSecret",RolloutServerConfig.SECRET);

            connection.connect();
            if (connection.getResponseCode() != 200) {
                Javalin.log.error("Could not checkout license for "+resourceId);
                Javalin.log.error("Error: "+connection.getResponseCode()+" - "+connection.getResponseMessage());
                if(connection.getResponseCode() == 500){
                    throw new IllegalArgumentException("("+connection.getResponseCode()+")"+connection.getResponseMessage());
                }else{
                    throw new CloudNotCheckoutLicenseException("("+connection.getResponseCode()+")"+connection.getResponseMessage());
                }
            } else {
                InputStream response = connection.getInputStream();
                String content;
                try (Scanner scanner = new Scanner(response)) {
                    content = scanner.useDelimiter("\\A").next();
                }
                response.close();
                License license = License.read(content);
                license.save(new File(RolloutServerConfig.LICENSE_FOLDER,license.getId()+".dat"));
                this.licenses.add(license);
                Javalin.log.info("Loaded deploy configuration successfully");
                return license;
            }
        }catch (IOException exception){
            Javalin.log.error("Could not check latest deploy configuration");
            Javalin.log.error("Error: "+exception.getMessage());
            throw new IORuntimeException(exception);
        }
    }
}
