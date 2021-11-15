package com.casm.acled.entities.sourcesourcelist;

import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.casm.acled.entities.sourcesourcelist.versions.SourceSourceList_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class SourceSourceListVersions extends VersionedEntityLinkSupplier<SourceSourceList> {

    public SourceSourceListVersions() {
        super(ImmutableMap.<String, Constructor<? extends SourceSourceList>>builder()
                .put("v1", constructorConstructor(SourceSourceList_v1.class))
                .build(), SourceSourceList.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
