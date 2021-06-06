package org.mcnative.rolloutserver.template;

import net.pretronic.libraries.document.Document;

import java.util.UUID;

public class Template {

    private final String name;
    private final UUID id;
    private final Document configuration;
    private String definition;

    public Template(String name, UUID id, Document configuration, String definition) {
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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }
}
