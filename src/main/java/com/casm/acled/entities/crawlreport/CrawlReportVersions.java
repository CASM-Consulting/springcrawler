package com.casm.acled.entities.crawlreport;

import com.casm.acled.entities.VersionedEntitySupplier;
import com.casm.acled.entities.crawlreport.versions.CrawlReport_v1;
import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Constructor;

public class CrawlReportVersions extends VersionedEntitySupplier<CrawlReport> {

    public CrawlReportVersions() {
        super(ImmutableMap.<String, Constructor<? extends CrawlReport>>builder()
                .put("v1", constructorConstructor(CrawlReport_v1.class))
                .build(), CrawlReport.class);
    }

    @Override
    public String currentVersion() {
        return "v1";
    }

}