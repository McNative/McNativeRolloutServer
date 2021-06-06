package org.mcnative.rolloutserver.resource;

import net.pretronic.libraries.document.entry.DocumentEntry;
import net.pretronic.libraries.resourceloader.VersionInfo;
import net.pretronic.libraries.utility.Iterators;
import org.mcnative.rolloutserver.profile.Profile;
import org.mcnative.rolloutserver.profile.ProfileController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class ResourceController {

    private final ProfileController profileController;
    private Collection<Resource> resources;

    public ResourceController(ProfileController profileController) {
        this.profileController = profileController;
        this.resources = new ArrayList<>();
    }

    public void setResources(Collection<Resource> resources) {
        this.resources = resources;
    }

    public Resource getResource(UUID id){
        Resource resource =  Iterators.findOne(this.resources, resource0 -> resource0.getId().equals(id));
        if(resource == null){
            resource = new Resource(id);
            this.resources.add(resource);
        }
        return resource;
    }

    public void downloadDeployedVersions(){
        for (Profile profile : profileController.getProfiles()) {
            for (DocumentEntry resource : profile.getConfiguration().getDocument("resources")) {
                UUID id = resource.toDocument().getObject("id",UUID.class);
                String version = resource.toDocument().getString("version");
                String qualifier = resource.toDocument().getString("qualifier");

                Resource result = getResource(id);
                if(version.equals("LATEST")){
                    result.download(result.getLatestVersionInfoAsInfo(qualifier));
                }else{
                    result.download(VersionInfo.parse(version));
                }
            }
        }
    }

    public void lookupVersions(){
        for (Resource resource : resources) resource.refreshVersions();
    }

}
