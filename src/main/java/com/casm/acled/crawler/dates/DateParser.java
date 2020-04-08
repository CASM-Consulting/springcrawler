package com.casm.acled.crawler.dates;

import java.time.LocalDate;
import java.util.Optional;

interface DateParser {
    Optional<LocalDate> parse(String date);
}
