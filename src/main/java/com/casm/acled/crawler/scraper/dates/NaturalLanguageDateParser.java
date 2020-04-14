package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

class NaturalLanguageDateParser implements DateParser {

    private final String[] triggers;
    private final String spec;
    public NaturalLanguageDateParser(String spec) {
        this.spec = spec;
        this.triggers = spec.split(",");
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

    @Override
    public List<String> getFormatSpec() {
        return ImmutableList.of(spec);
    }
}
