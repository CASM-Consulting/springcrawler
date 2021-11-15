package com.casm.acled.entities.article;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.article.versions.Article_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class ArticleVersions extends VersionedEntitySupplier<Article> {

    public ArticleVersions() {
        super(ImmutableMap.<String, Constructor<? extends Article>>builder()
                .put("v1", constructorConstructor(Article_v1.class))
                .build(), Article.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }
}
