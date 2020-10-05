package com.casm.acled.crawler;

import com.norconex.collector.http.recrawl.IRecrawlableResolver;
import com.norconex.collector.http.recrawl.PreviousCrawlData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DontRecrawlResolver implements IRecrawlableResolver {

    public final Set<String> startURLs;
    public final Pattern pattern;

    public DontRecrawlResolver(String[] startURLs, Pattern pattern) {
        this.startURLs = new HashSet<>();
        Collections.addAll(this.startURLs, startURLs);
        this.pattern = pattern;
    }

    @Override
    public boolean isRecrawlable(PreviousCrawlData prevCrawlData) {
        String ref = prevCrawlData.getReference();
        if(startURLs.contains(ref) || (pattern != null && pattern.matcher(ref).matches())) {
            return true;
        } else {
            return false;
        }
    }
}
