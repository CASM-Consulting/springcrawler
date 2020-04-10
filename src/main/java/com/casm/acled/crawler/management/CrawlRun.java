package com.casm.acled.crawler.management;

import com.casm.acled.entities.source.Source;

import java.net.URI;
import java.time.LocalDateTime;

public class CrawlRun {

    private final Source source;

    public static final String SOURCE_ID = "SOURCE_ID";

    private final LocalDateTime from;
    private final LocalDateTime to;


    public CrawlRun(Source source, NorconexConfiguration config, LocalDateTime from, LocalDateTime to) {
        this.source = source;
        this.from = from;
        this.to= to;
    }

    public void recordEvent(URI url, CrawlEvent event) {

    }
}
