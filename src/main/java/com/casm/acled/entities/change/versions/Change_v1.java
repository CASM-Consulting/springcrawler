package com.casm.acled.entities.change.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.change.Change;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import java.time.LocalDateTime;
import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

public class Change_v1 extends Change {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
//            .business()
            .add(TARGET_TYPE, String.class)
            .add(TARGET_ID, Integer.class)
            .add(TARGET_FIELD, String.class)
            .add(builder(FROM, FROM, String.class)
                    .encodingException((node, oc) -> new ObjectMapper().writeValueAsString(node))
                    .build())
            .add(builder(TO, TO, String.class)
                    .encodingException((node, oc) -> new ObjectMapper().writeValueAsString(node))
                    .build())
            .add(BY, String.class)
            .add(DATE_TIME, LocalDateTime.class)
            .add(DELETE, Boolean.class)
            .add(IMPLEMENTED, Boolean.class)
            ;


    public Change_v1(){
        super(SPECIFICATION, "v1", ImmutableMap.of(), null);
    }

    public Change_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }

}
