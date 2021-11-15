package com.casm.acled.entities.actordesk;

import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.casm.acled.entities.actordesk.versions.ActorDesk_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class ActorDeskVersions extends VersionedEntityLinkSupplier<ActorDesk> {

    public ActorDeskVersions() {
        super(ImmutableMap.<String, Constructor<? extends ActorDesk>>builder()
                .put("v1", constructorConstructor(ActorDesk_v1.class))
                .build(), ActorDesk.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
