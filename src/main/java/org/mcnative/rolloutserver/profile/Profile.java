package org.mcnative.rolloutserver.profile;

import net.pretronic.libraries.document.Document;

import java.util.UUID;

public class Profile {

    private final String name;
    private final UUID id;
    private final Document configuration;
    private String definition;

    public Profile(String name, UUID id, Document configuration, String definition) {
        this.name = name;
        this.id = id;
        this.configuration = configuration;
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public Document getConfiguration() {
        return configuration;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getDefinition() {
        return definition;
    }

}
