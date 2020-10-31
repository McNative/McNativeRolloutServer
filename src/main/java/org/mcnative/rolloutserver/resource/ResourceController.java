package org.mcnative.rolloutserver.resource;

import net.pretronic.libraries.utility.Iterators;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class ResourceController {

    private Collection<Resource> resources;

    public ResourceController() {
        this.resources = new ArrayList<>();
    }

    public void setResources(Collection<Resource> resources) {
        this.resources = resources;
    }

    public Resource getResource(UUID id){
        return Iterators.findOne(this.resources, resource -> resource.getId().equals(id));
    }

    public void downloadDeployedResourceVersions(){
        for (Resource resource : this.resources) {
            resource.downloadInstallVersions();
        }
    }
}
