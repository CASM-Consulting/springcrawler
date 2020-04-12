package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

class DateFormatParser implements DateParser {

    private final DateTimeFormatter formatter;

    public DateFormatParser(DateTimeFormatter formatter, Locale locale) {
        this.formatter = formatter;
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        Optional<LocalDateTime> attempt = Optional.empty();
        try {
            LocalDateTime parsed = LocalDateTime.from(formatter.parse(date));
            attempt = Optional.of(parsed);
        } catch (DateTimeParseException e) {

            DateUtil.logger.warn(e.getMessage());
        }
        return attempt;
    }
}
