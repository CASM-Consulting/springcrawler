package com.casm.acled.entities.location;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.location.versions.Location_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class LocationVersions extends VersionedEntitySupplier<Location> {



    public LocationVersions() {
        super(ImmutableMap.<String, Constructor<? extends Location>>builder()
                .put("v1", constructorConstructor(Location_v1.class))
                .build(), Location.class);
    }

    @Override
    public Class<Location> getBaseClass() {
        return Location.class;
    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
