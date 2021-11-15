package com.casm.acled.entities.sourcedesk.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.sourcedesk.SourceDesk;

import java.util.Map;

public class SourceDesk_v1 extends SourceDesk {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(SOURCE_ID, "Source", Integer.class)
            .add(DESK_ID, "Desk", Integer.class, "desk");

    public SourceDesk_v1(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(SPECIFICATION, "v1", data, id, id1, id2);
    }

}
