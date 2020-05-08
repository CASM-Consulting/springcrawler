package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;


/**
 * TODO: none of these work in other lang - farm this out to python date parser https://pypi.org/project/dateparser/ over HTTP
 */
class NaturalLanguageDateParser implements DateParser {

    public static final String PROTOCOL = "NL";

//    private static final String

    private final String[] triggers;
    private final String spec;
    public NaturalLanguageDateParser(String spec) {
        this.spec = spec;
        this.triggers = spec.split(",");

//        WebClient.builder().baseUrl()
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
        return ImmutableList.of(PROTOCOL+":"+spec);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        NaturalLanguageDateParser that = (NaturalLanguageDateParser) o;

        return new EqualsBuilder()
                .append(spec, that.spec)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(spec)
                .toHashCode();
    }
}
