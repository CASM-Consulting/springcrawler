package com.casm.acled.entities.source;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.source.versions.Source_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class SourceVersions extends VersionedEntitySupplier<Source> {

    public SourceVersions() {
        super(ImmutableMap.<String, Constructor<? extends Source>>builder()
                .put("v1", constructorConstructor(Source_v1.class))
                .build(), Source.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
