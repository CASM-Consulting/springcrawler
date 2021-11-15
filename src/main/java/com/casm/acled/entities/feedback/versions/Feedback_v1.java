package com.casm.acled.entities.feedback.versions;

import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.feedback.Feedback;

import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

public class Feedback_v1 extends Feedback {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
//            .business()
            .add(builder(
                    TEXT, "Feedback", String.class)
                    .displayType("textarea")
                    .putMeta("minimisable", false).build()
            )
            .add(AUTHOR, String.class)
            .add(RECIPIENT, String.class)
            ;

    public Feedback_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }
}
