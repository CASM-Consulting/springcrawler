package com.casm.acled.entities.actor;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.actor.versions.Actor_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class ActorVersions extends VersionedEntitySupplier<Actor> {

    public ActorVersions() {
        super(ImmutableMap.<String, Constructor<? extends Actor>>builder()
                .put("v1", constructorConstructor(Actor_v1.class))
                .build(), Actor.class);

    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
