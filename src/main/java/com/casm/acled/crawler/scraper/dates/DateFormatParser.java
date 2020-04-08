package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

class DateFormatParser implements DateParser {

    private final DateTimeFormatter formatter;
    public DateFormatParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public Optional<LocalDate> parse(String date) {
        Optional<LocalDate> attempt = Optional.empty();
        try {
            LocalDate parsed = LocalDate.from(formatter.parse(date));
            attempt = Optional.of(parsed);
        } catch (DateTimeParseException e) {

            DateUtil.logger.warn(e.getMessage());
        }
        return attempt;
    }
}
