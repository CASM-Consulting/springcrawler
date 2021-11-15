package com.casm.acled.entities.sourcesourcelist.versions;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.sourcesourcelist.SourceSourceList;

import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

public class SourceSourceList_v1 extends SourceSourceList {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(builder(SOURCE_ID, "Source", Integer.class)
                    .displayType("source")
                    .hide(Process.ADD_SOURCE)
                    .hide(Process.ENTITY_REVIEW)
                    .build()
            )
            .add(builder(SOURCE_LIST_ID, "Source List", Integer.class)
                    .displayType("sourceList")
                    .hide(Process.BACK_CODING_CONFIG)
                    .hide(Process.LIVE_CODING_CONFIG)
                    .build()
            )
            .add(KEYWORDS_DIFF, String.class)
            ;

    public SourceSourceList_v1(Map<String, Object> data, Integer id, Integer id1, Integer id2) {
        super(SPECIFICATION, "v1", data, id, id1, id2);
    }

}
