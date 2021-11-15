package com.casm.acled.entities.feedback;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.feedback.versions.Feedback_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class FeedbackVersions extends VersionedEntitySupplier<Feedback> {

    public FeedbackVersions() {
        super(ImmutableMap.<String, Constructor<? extends Feedback>>builder()
                .put("v1", constructorConstructor(Feedback_v1.class))
                .build(), Feedback.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
