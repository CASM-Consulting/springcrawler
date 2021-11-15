package com.casm.acled.entities.articleevent;

import com.casm.acled.entities.VersionedEntityLinkSupplier;
import com.casm.acled.entities.articleevent.versions.ArticleEvent_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class ArticleEventVersions extends VersionedEntityLinkSupplier<ArticleEvent> {

    public ArticleEventVersions() {
        super(ImmutableMap.<String, Constructor<? extends ArticleEvent>>builder()
                .put("v1", constructorConstructor(ArticleEvent_v1.class))
                .build(), ArticleEvent.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}
