package com.casm.acled.crawler.dates;

import java.time.LocalDate;
import java.util.Optional;

class NaturalLanguageDateParser implements DateParser {

    private final String[] triggers;
    public NaturalLanguageDateParser(String[] triggers) {
        this.triggers = triggers;
    }

    @Override
    public Optional<LocalDate> parse(String date) {
        boolean makeAttempt = false;
        Optional<LocalDate> attempt = Optional.empty();
        for(String trigger : triggers) {
            if(date.toLowerCase().contains(trigger.toLowerCase())) {
                makeAttempt = true;
            }
        }
        if(makeAttempt) {
            attempt = DateUtil.getDateWithoutNormalisation(date);
        }
        return attempt;
    }
}
