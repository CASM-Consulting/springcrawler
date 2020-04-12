package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface DateParser {
    Optional<LocalDateTime> parse(String date);
}
