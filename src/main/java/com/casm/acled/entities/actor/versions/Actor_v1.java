package com.casm.acled.entities.actor.versions;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.validation.FieldValidators;
import com.casm.acled.entities.validation.FrontendValidators;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

@Component
public class Actor_v1 extends Actor {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(builder(ACTOR_NAME, "Name", String.class)
                    .displayType("suggestValue")
                    .putMeta("minimisable", false)
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .build())
            .add(builder(INTER, "Inter", String.class)
                    .putMeta("minimisable", true)
                    .build())
            .add(MAIN_COUNTRY, "Main Country", String.class, "suggestValue")
            .add(DATE_ADDED, "Date Added", LocalDate.class)
            .add(MANDATORY_ASSOCIATE_ACTOR, "Mandatory Assoc. Actor", String.class)
            .add(MANDATORY_ACTOR1_2, "Mandatory Actor 1-2", String.class)
            .add(ALIAS_CODING_NOTES, "Alias Coding Notes", String.class, "suggestValue")
            .add(DESCRIPTION, "Description", String.class)
            .add(LINKS, "Links", String.class)
            .add(PAUSED_MANDATORY_ASSOCIATE_ACTOR, "Paused Mandatory Assoc. Actor", String.class)
//            .add(VERIFIED, "Verified", Boolean.class, "label")
            .add(builder(VERIFIED, "Verified", Boolean.class).putMeta(
                    Process.CONTEXT_CONDITION,
                    ImmutableMap.of(
                            Process.ENTITY_REVIEW, Process.EDIT,
                            Process.ADD_ACTOR, Process.HIDE
                    )
            ).build())
            .unique(ACTOR_NAME)
//            .deletable()
            ;

    public Actor_v1(){
        super(SPECIFICATION, "v1", ImmutableMap.of(), null);
    }

    public Actor_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }

}
