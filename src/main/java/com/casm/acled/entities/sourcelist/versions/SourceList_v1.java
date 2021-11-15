package com.casm.acled.entities.sourcelist.versions;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.time.LocalDate;
import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

public class SourceList_v1 extends SourceList {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(LIST_NAME, "Name", String.class)
            .add(builder(DESK_ID, "Desk", Integer.class)
                    .displayType("desk")
                    .putMeta(Process.CONTEXT_CONDITION,
                            ImmutableMap.of(
                                    Process.ADD_SOURCE, Process.HIDE
                            )
                    )
                    .build()
            )
            .add(KEYWORDS, String.class)
            .add(TIMEZONE, String.class)
            .add(CRAWL_ACTIVE, Boolean.class)
            .add(MANUAL_ACTIVE, Boolean.class)
            .add(BACK_CODING, Boolean.class)
            .add(builder(FROM, "From", LocalDate.class)
                    .putMeta(Process.CONTEXT_CONDITION,
                            ImmutableMap.of(
                                    Process.BACK_CODING_CONFIG, Process.EDIT,
                                    Process.LIVE_CODING_CONFIG, Process.HIDE
                            )
                    )
                    .build()
            )
            .add(builder(TO, "To", LocalDate.class)
                    .putMeta(Process.CONTEXT_CONDITION,
                            ImmutableMap.of(
                                    Process.BACK_CODING_CONFIG, Process.EDIT,
                                    Process.LIVE_CODING_CONFIG, Process.HIDE
                            )
                    )
                    .build()
            )
            .add(builder(NOTES, "Notes", String.class)
                    .displayType("textarea")
                    .build()
            )
            .unique(ImmutableSet.of(LIST_NAME))
            ;

    public SourceList_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }
}
