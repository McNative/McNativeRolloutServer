package org.mcnative.rolloutserver.template;

import net.pretronic.libraries.utility.Iterators;

import java.util.ArrayList;
import java.util.Collection;

public class TemplateController {

    private Collection<Template> templates;

    public TemplateController() {
        this.templates = new ArrayList<>();
    }

    public void setTemplates(Collection<Template> templates) {
        this.templates = templates;
        for (Template template : templates) {
            template.setDefinition(template.getDefinition().replace("\\n","\n"));
        }
    }

    public Template getTemplate(String name){
        return Iterators.findOne(this.templates, template -> template.getName().equalsIgnoreCase(name));
    }
}
