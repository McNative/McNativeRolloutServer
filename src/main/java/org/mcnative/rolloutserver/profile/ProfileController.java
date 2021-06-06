package org.mcnative.rolloutserver.profile;

import net.pretronic.libraries.utility.Iterators;

import java.util.ArrayList;
import java.util.Collection;

public class ProfileController {

    private Collection<Profile> profiles;

    public ProfileController() {
        this.profiles = new ArrayList<>();
    }

    public Collection<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Collection<Profile> profiles) {
        this.profiles = profiles;
        for (Profile profile : profiles) {
            profile.setDefinition(profile.getDefinition().replace("\\n","\n"));
        }
    }

    public Profile getProfile(String name){
        return Iterators.findOne(this.profiles, template -> template.getName().equalsIgnoreCase(name));
    }
}
