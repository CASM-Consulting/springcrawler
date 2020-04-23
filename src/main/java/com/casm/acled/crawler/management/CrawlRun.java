package com.casm.acled.crawler.management;

import com.casm.acled.crawler.utils.Util;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.norconex.collector.http.HttpCollector;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class CrawlRun {

    private final Source source;

    public static final String SOURCE_ID = "SOURCE_ID";

    private final LocalDateTime from;
    private final LocalDateTime to;

    private final NorconexConfiguration config;

    public CrawlRun(SourceList sourceList, Source source, NorconexConfiguration config, LocalDateTime from, LocalDateTime to) {
        this.source = source;
        this.from = from;
        this.to = to;
        this.config = config;

        ZoneId zoneId = ZoneId.of(source.get(Source.TIMEZONE));

        List<String> query = resolveQuery(sourceList, source);

        config.setFilter(ZonedDateTime.of(from, zoneId), ZonedDateTime.of(to, zoneId), query);
    }

    private List<String> resolveQuery(SourceList sourceList, Source list) {
        return Util.KEYWORDS_LUCENE;
    }

    public void run() {
        HttpCollector collector = new HttpCollector(config.collector());
        collector.start(true);
    }
}
