package com.casm.acled.entities.sourcelist;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.sourcelist.versions.SourceList_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class SourceListVersions extends VersionedEntitySupplier<SourceList> {

    public SourceListVersions() {
        super(ImmutableMap.<String, Constructor<? extends SourceList>>builder()
                .put("v1", constructorConstructor(SourceList_v1.class))
                .build(), SourceList.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
