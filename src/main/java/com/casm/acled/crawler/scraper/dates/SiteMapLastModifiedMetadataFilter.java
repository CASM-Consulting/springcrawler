package com.casm.acled.crawler.scraper.dates;

import com.norconex.collector.core.filter.IMetadataFilter;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.commons.lang.map.Properties;

import java.time.LocalDate;
import java.time.ZoneId;

public class SiteMapLastModifiedMetadataFilter implements IMetadataFilter {

    private final long now;

    public SiteMapLastModifiedMetadataFilter(LocalDate from) {
        //now minus a day for timezone safety
        now = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - (24 * 60 * 60 * 1000);
    }

    @Override
    public boolean acceptMetadata(String reference, Properties metadata) {
        if(metadata.get(HttpMetadata.COLLECTOR_SM_LASTMOD) != null && metadata.get(HttpMetadata.COLLECTOR_SM_LASTMOD).isEmpty()) {
            return true;
        } else {
            //milliseconds
            long lastMod = metadata.getLong(HttpMetadata.COLLECTOR_SM_LASTMOD);
            if(lastMod < now) {
                return false;
            } else {
                return true;
            }
        }
    }
}
