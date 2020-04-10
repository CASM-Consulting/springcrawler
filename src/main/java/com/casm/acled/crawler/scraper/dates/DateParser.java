package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface DateParser {
    Optional<ZonedDateTime> parse(String date);
}
