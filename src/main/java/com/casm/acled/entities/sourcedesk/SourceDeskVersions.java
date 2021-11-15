package com.casm.acled.entities.sourcedesk;

import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.casm.acled.entities.sourcedesk.versions.SourceDesk_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class SourceDeskVersions extends VersionedEntityLinkSupplier<SourceDesk> {

    public SourceDeskVersions() {
        super(ImmutableMap.<String, Constructor<? extends SourceDesk>>builder()
                .put("v1", constructorConstructor(SourceDesk_v1.class))
                .build(), SourceDesk.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
