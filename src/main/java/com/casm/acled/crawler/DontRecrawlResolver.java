package com.casm.acled.crawler;

import com.norconex.collector.http.recrawl.IRecrawlableResolver;
import com.norconex.collector.http.recrawl.PreviousCrawlData;

public class DontRecrawlResolver implements IRecrawlableResolver {



    @Override
    public boolean isRecrawlable(PreviousCrawlData prevCrawlData) {
        return false;
    }
}
