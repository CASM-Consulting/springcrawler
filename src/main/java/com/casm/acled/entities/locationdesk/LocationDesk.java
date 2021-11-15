package com.casm.acled.entities.locationdesk;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.VersionedEntityLink;

import java.util.Map;

public class LocationDesk extends VersionedEntityLink<LocationDesk> {

    public static final String LOCATION_ID = ID1;
    public static final String DESK_ID = ID2;

    public LocationDesk(EntitySpecification entitySpecification, String version, Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(entitySpecification, version, data, id, id1, id2);
    }
}