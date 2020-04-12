package com.casm.acled.crawler.scraper.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

class NaturalLanguageDateParser implements DateParser {

    private final String[] triggers;
    public NaturalLanguageDateParser(String[] triggers) {
        this.triggers = triggers;
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        boolean makeAttempt = false;
        Optional<LocalDateTime> attempt = Optional.empty();
        for(String trigger : triggers) {
            if(date.toLowerCase().contains(trigger.toLowerCase())) {
                makeAttempt = true;
            }
        }
        if(makeAttempt) {
            if(attempt.isPresent()) {
                attempt = Optional.of(LocalDateTime.from(DateUtil.getDateWithoutNormalisation(date).get()));
            }
        }
        return attempt;
    }
}
