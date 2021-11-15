package com.casm.acled.entities.event.versions;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.camunda.variables.Process;
import com.casm.acled.camunda.variables.Properties;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.actor.Actor;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.event.eventtypes.EventType_v1;
import com.casm.acled.entities.event.validation.EventValidators;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.validation.FieldValidators;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.casm.acled.entities.EntityField.builder;
import static com.casm.acled.entities.validation.FrontendValidators.Field.*;

@Component
public class Event_v1 extends Event {

    private static String SMALL_COL = "56";

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()

            .add(builder(EVENT_DATE, "Date", LocalDate.class)
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .build())

            .add(builder(SOURCES, "Sources", List.class)
                    .encodingException((n, oc)-> Arrays.asList(n.asText().split(";")))
                    .hide(Process.ALL)
                    .build())

            .add(builder(API_ID, "API ID", Integer.class)
                    .hide(Process.ALL)
                    .build())

            .add(builder(TIME_PRECISION, "Time Precision", String.class)
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .displayType("precision")
                    .putMeta(Properties.DISPLAY_WIDTH, SMALL_COL)
                    .build()
            )

//            .add(builder(EVENT_TYPE, "Type", String.class)
//                    .displayType("event")
//                    .putMeta(Process.CONTEXT_CONDITION,
//                        ImmutableMap.of(
//                            Process.RM_REVIEW, Process.HIDE
//                        )
//                    )
//                    .validators(FieldValidators.ofFrontend(REQUIRED))
//                    .build()
//            )

            .add(builder(SUB_EVENT_TYPE, "Sub Type", String.class)
                    .displayType("subEvent")
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .build())

            .add(builder(EVENT_ID_CNTY,"Event ID Cnty", String.class)
                    .hide(Process.ALL)
                    .build())

            .add(builder(EVENT_ID_NO_CNTY,"Event ID Cnty", Integer.class)
                    .hide(Process.ALL)
                    .build())

            .add(builder(ACTOR1, "Actor 1", Integer.class)
                    .displayType(Entities.ACTOR)
                    .validators(FieldValidators.ofFrontend(
                            REQUIRED, ACTOR_BATTLE, FOREIGN_ACTOR))
                    .build())

            .add(builder(ASSOC_ACTOR_1, "Assoc. Actor 1", List.class)
                    .displayType("list[actor]")
                    .validators(FieldValidators.ofFrontend(ASSOC_ACTOR_BATTLE))
                    .putMeta("associate", true)
                    .build())

            .add(builder(ACTOR2, "Actor 2", Integer.class)
                    .displayType("actor")
                    .validators(FieldValidators.ofFrontend(
                            ACTOR_BATTLE, FOREIGN_ACTOR))
                    .build())

            .add(builder(ASSOC_ACTOR_2, "Assoc. Actor 2", List.class)
                    .displayType("list[actor]")
                    .validators(FieldValidators.ofFrontend(ASSOC_ACTOR_BATTLE))
                    .putMeta("associate", true)
                    .build()
            )

            .add(builder(INTERACTION, "Interaction", String.class)
                    .displayType("interaction")
                    .hide(Process.ALL)
                    .putMeta(Properties.DISPLAY_WIDTH, SMALL_COL)
                    .build()
            )

            .add(builder(EVENT_LOCATION, "Location", Integer.class)
                    .displayType("location")
                    .validators(FieldValidators.ofFrontend(
                            REQUIRED, EMPTY_LOCATION_FIELDS))
                    .build())


            .add(builder(LOCATION_PRECISION, "Geo Precision", String.class)
                    .displayType("precision")
                    .putMeta(Properties.DISPLAY_WIDTH, SMALL_COL)
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .build())

            .add(builder(FATALITIES, "Fatalities", Integer.class)
                    .displayType("positiveInt")
                    .validators(FieldValidators.ofFrontend(FATALITY, REQUIRED))
                    .putMeta(Properties.DISPLAY_WIDTH, SMALL_COL)
                    .build())

            .add(builder(FATALITIES_PRECISION, "Fatalities Precision", String.class)
                    .displayType("precision")
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .putMeta(Properties.DISPLAY_WIDTH, SMALL_COL)
                    .build()
            )

            .add(builder(NOTES, "Notes", String.class)
                    .putMeta(Process.STICKY, true)
                    .displayType("textarea")
                    .validators(FieldValidators.ofFrontend(
                            REQUIRED, MIN_CHARS_20))
                    .putMeta(Process.STICKY, true)
                    .build())

            .add(MISC, "Misc", String.class)

            .add(builder(HIGHLIGHT, "Highlight", Map.class)
                    .displayType("highlights")
                    .encodingException((node, oc) -> {
                        String s = node.asText();
                        JsonParser p = oc.getFactory().createParser(s);
                        JsonNode parsed = p.getCodec().readTree(p);
                        return p.getCodec().treeToValue(parsed, Map.class);
                    })
                    .build())

            .add(builder(RM_FEEDBACK, "RM Feedback", String.class)
                    .displayType("textarea")
                    .putMeta(Process.CONTEXT_CONDITION,
                        ImmutableMap.of(
                            Process.RM_REVIEW, Process.EDIT,
                            Process.SOURCE_CODE, Process.HIDE
                        )
                    )
                    .build()
            )

            .add(builder(RM_CHECK, "RM Check", String.class)
//                    .displayType("checkbox")
                    .putMeta(Process.CONTEXT_CONDITION,
                        ImmutableMap.of(
                            Process.RM_REVIEW, Process.EDIT,
                            Process.SOURCE_CODE, Process.HIDE
                        ))
                    .build()
            )

//            .add(builder(RM_VERIFIED, "RM Verified", Boolean.class)
//                    .displayType("checkbox")
//                    .putMeta(Process.CONTEXT_CONDITION,
//                        ImmutableMap.of(
//                            Lists.newArrayList(Process.RM_REVIEW), Process.EDIT,
//                            Process.SOURCE_CODE, Process.HIDE
//                        ))
//                    .build()
//            )

            .add(builder(GRM_FEEDBACK, "GRM Feedback", String.class)
                    .displayType("textarea")
                    .putMeta(Process.CONTEXT_CONDITION,
                        ImmutableMap.of(
                            Process.GRM_REVIEW, Process.EDIT,
                            Process.RM_REVIEW, Process.HIDE,
                            Process.SOURCE_CODE, Process.HIDE
                        ))
                    .build()
            )

            .add(builder(SCALE_PRIMARY, "Source Scale (Primary)", String.class)
                .putMeta(Process.CONTEXT_CONDITION, ImmutableMap.of(
                    Process.OVERVIEW, Process.HIDE,
                    Process.SOURCE_CODE, Process.HIDE
                )).build()
            )

        .add(builder(SCALE_SECONDARY, "Source Scale (Secondary)", String.class)
            .putMeta(Process.CONTEXT_CONDITION, ImmutableMap.of(
                Process.OVERVIEW, Process.HIDE,
                Process.SOURCE_CODE, Process.HIDE
            )).build()
        )

//            .add(builder(GRM_VERIFIED, "GRM Verified", Boolean.class)
//                    .displayType("checkbox")
//                    .putMeta(Process.CONTEXT_CONDITION,
//                        ImmutableMap.of(
//                            Process.GRM_REVIEW, Process.EDIT,
//                            Process.SOURCE_CODE, Process.HIDE
//                        ))
//                    .build()
//            )
//            .business()
//            .maybeHistorical()
//            .deletable();
;
    @Autowired
    private EventValidators eventValidators;

    public Event_v1(){
        super(SPECIFICATION,
                "v1",
                ImmutableMap.of(),
                null);
    }

    public EventValidators getValidators() {
        return eventValidators;
    }

    public Event_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION
                , "v1", data, id);
    }

    public Event_v1(Map<String, Object> data, Integer id, List<Article> articles, List<Source> sources, Actor actor1, List<Actor> assocActor1,
                    Actor actor2, List<Actor> assocActor2, Location location) {
        super(SPECIFICATION
                , "v1", data, id, articles, sources, actor1, assocActor1, actor2, assocActor2, location);
    }


    @Override
    public Set<String> getTypes() {
        return EventType_v1.TYPES.keySet();
    }
}
