package org.mcnative.rolloutserver.utils;

import net.pretronic.libraries.document.Document;
import net.pretronic.libraries.document.adapter.DocumentAdapter;
import net.pretronic.libraries.document.entry.DocumentBase;
import net.pretronic.libraries.document.entry.DocumentEntry;
import net.pretronic.libraries.resourceloader.VersionInfo;
import net.pretronic.libraries.utility.reflect.TypeReference;

public class VersionInfoDocumentAdapter implements DocumentAdapter<VersionInfo> {

    @Override
    public VersionInfo read(DocumentBase documentBase, TypeReference<VersionInfo> typeReference) {
        if(documentBase.isPrimitive()){
            return VersionInfo.parse(documentBase.toPrimitive().getAsString());
        }
        throw new IllegalArgumentException("Version is not a primitive entry");
    }

    @Override
    public DocumentEntry write(String key, VersionInfo info) {
        return Document.factory().newPrimitiveEntry(key,info.toString());
    }
}
