package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface DateParser {
    Optional<LocalDateTime> parse(String date);

    DateParser locale(List<ULocale> locale);

    default DateParser locale(ULocale locale) {
        return locale(ImmutableList.of(locale));
    }

    List<String> getFormatSpec();
}
