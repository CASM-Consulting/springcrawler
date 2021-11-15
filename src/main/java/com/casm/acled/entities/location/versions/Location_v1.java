package com.casm.acled.entities.location.versions;

import com.casm.acled.camunda.variables.Entities;
import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.location.Location;
import com.casm.acled.entities.validation.FieldValidators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;
import static com.casm.acled.entities.validation.FrontendValidators.Field.LAT_LON;
import static com.casm.acled.entities.validation.FrontendValidators.Field.REQUIRED;

@Component
public class Location_v1 extends Location {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(builder(
                LOCATION, "Name", String.class)
                    .displayType("suggestValue")
                    .validators(FieldValidators.ofFrontend(REQUIRED))
                    .putMeta("minimisable", false).build())
            .add(builder(
                COUNTRY, "Country", String.class)
                    .displayType("suggestValue")
                    .putMeta(Entities.CONSTRAINT_GROUPS, ImmutableSet.of("loc"))
                    .putMeta("minimisable", false).build())
            .add(builder(
                ADMIN1, "Admin 1", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .putMeta("minimisable", false).build())
            .add(builder(
                ADMIN2, "Admin 2", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .putMeta("minimisable", true).build())
            .add(builder(
                ADMIN3, "Admin 3", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .putMeta("minimisable", true).build())
            .add(builder(
                    ISO, "ISO", Integer.class)
                    .putMeta("minimisable", true).build())
            .add(builder(
                LATITUDE, "Latitude", Double.class)
                    .putMeta("minimisable", false)
                    .putMeta(Process.CONTEXT_CONDITION,
                        ImmutableMap.of(
                            Process.RM_REVIEW, Process.HIDE,
                            Process.OVERVIEW, Process.HIDE
                        )
                    )
                    .validators(FieldValidators.ofFrontend(LAT_LON))
                    .build())
            .add(builder(
                LONGITUDE, "Longitude", Double.class)
                    .putMeta("minimisable", false)
                    .putMeta(Process.CONTEXT_CONDITION,
                        ImmutableMap.of(
                            Process.RM_REVIEW, Process.HIDE,
                            Process.OVERVIEW, Process.HIDE
                        )
                    )
                    .validators(FieldValidators.ofFrontend(LAT_LON))
                    .build())
            .add(builder(
                GEO_PRECISION, "Geo Precision", String.class)
                    .displayType("precision")
                    .putMeta("collapsible", true)
                    .build())
            .add(builder(ADM1_CAPITAL, "Adm1 Capital", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .build())
            .add(builder(ADM2_CAPITAL, "Adm2 Capital", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .build())
            .add(builder(ADM3_CAPITAL, "Adm3 Capital", String.class)
                    .displayType("suggestValue")
                    .constraintGroup("loc")
                    .build())
            .add(ALIAS, "Alias", String.class, "suggestValue")
            .add(ALIAS_NON_ENGLISH, "Alias Non-English", String.class, "suggestValue")
            .add(NOTES, "Notes", String.class)
            .add(builder(VERIFIED, "Verified", Boolean.class).putMeta(
                Process.CONTEXT_CONDITION,
                ImmutableMap.of(
                    Process.ENTITY_REVIEW, Process.EDIT,
                    Process.ADD_LOCATION, Process.HIDE
                )
            ).build())
//            .deletable();
;
    public Location_v1(){
        super(SPECIFICATION, "v1", ImmutableMap.of(), null);
    }

    public Location_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }
}
