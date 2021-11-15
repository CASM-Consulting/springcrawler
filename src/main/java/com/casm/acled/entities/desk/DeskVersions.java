package com.casm.acled.entities.desk;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.desk.versions.Desk_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class DeskVersions extends VersionedEntitySupplier<Desk> {

    public DeskVersions() {
        super(ImmutableMap.<String, Constructor<? extends Desk>>builder()
                .put("v1", constructorConstructor(Desk_v1.class))
                .build(), Desk.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
