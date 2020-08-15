package org.mcnative.rolloutserver.resource;

import net.pretronic.libraries.resourceloader.VersionInfo;

public class ResourceProfile {

    private String name;
    private VersionInfo installVersion;

    public String getName() {
        return name;
    }

    public VersionInfo getInstallVersion() {
        return installVersion;
    }
}
