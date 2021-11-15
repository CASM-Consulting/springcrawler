package com.casm.acled.entities.locationdesk.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.locationdesk.LocationDesk;

import java.util.Map;

public class LocationDesk_v1 extends LocationDesk {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(LOCATION_ID, Integer.class)
            .add(DESK_ID, Integer.class, "desk");

    public LocationDesk_v1(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(SPECIFICATION, "v1", data, id, id1, id2);
    }

}
