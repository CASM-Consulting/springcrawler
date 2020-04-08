package com.casm.acled.crawler.dates;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class CompositeDateParser implements DateParser {

    private final List<DateParser> parsers;

    public CompositeDateParser(List<DateParser> parsers) {
        this.parsers = parsers;
    }

    @Override
    public Optional<LocalDate> parse(String date) {
        Optional<LocalDate> attempt =  Optional.empty();

        for(DateParser parser : parsers ) {
            Optional<LocalDate> parse = parser.parse(date);
            if(parse.isPresent()) {
                attempt = parse;
                break;
            }
        }

        return attempt;
    }
}
