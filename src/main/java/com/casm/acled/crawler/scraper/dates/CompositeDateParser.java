package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

class CompositeDateParser implements DateParser {

    private final List<DateParser> parsers;

    public CompositeDateParser(List<DateParser> parsers) {
        this.parsers = parsers;
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        Optional<LocalDateTime> attempt =  Optional.empty();

        for(DateParser parser : parsers ) {
            Optional<LocalDateTime> parse = parser.parse(date);
            if(parse.isPresent()) {
                attempt = parse;
                break;
            }
        }

        return attempt;
    }
}
