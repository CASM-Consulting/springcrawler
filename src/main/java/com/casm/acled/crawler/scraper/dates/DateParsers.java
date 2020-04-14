package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;

public class DateParsers {

    public static final DateParser dp1 = CompositeDateParser.of(ImmutableList.of("ISO:MMMM d, yyyy:en"));
}
