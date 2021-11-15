package com.casm.acled.entities.event;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.event.versions.Event_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class EventVersions extends VersionedEntitySupplier<Event> {

    public EventVersions() {
        super(ImmutableMap.<String, Constructor<? extends Event>>builder()
                .put("v1", constructorConstructor(Event_v1.class))
                .build(), Event.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
