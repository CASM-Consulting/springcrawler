package com.casm.acled.entities.desk.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.desk.Desk;

import java.util.Map;

public class Desk_v1 extends Desk {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(DESK_NAME, String.class);

    public Desk_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }

}
