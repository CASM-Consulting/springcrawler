package com.casm.acled.entities.locationdesk;

import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.casm.acled.entities.locationdesk.versions.LocationDesk_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class LocationDeskVersions extends VersionedEntityLinkSupplier<LocationDesk> {

    public LocationDeskVersions() {
        super(ImmutableMap.<String, Constructor<? extends LocationDesk>>builder()
                .put("v1", constructorConstructor(LocationDesk_v1.class))
                .build(), LocationDesk.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
