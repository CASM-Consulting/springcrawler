package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.util.Optional;

public interface DateParser {
    Optional<LocalDate> parse(String date);
}
