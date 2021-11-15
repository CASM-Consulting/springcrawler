package com.casm.acled.entities.change;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.change.versions.Change_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class ChangeVersions extends VersionedEntitySupplier<Change> {

    public ChangeVersions() {
        super(ImmutableMap.<String, Constructor<? extends Change>>builder()
                .put("v1", constructorConstructor(Change_v1.class))
                .build(), Change.class);

    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
