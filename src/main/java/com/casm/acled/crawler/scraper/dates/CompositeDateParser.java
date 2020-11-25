package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

public class CompositeDateParser implements DateParser {

    private final List<DateParser> parsers;

    public CompositeDateParser(List<DateParser> parsers) {
        this.parsers = Lists.newArrayList(parsers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CompositeDateParser that = (CompositeDateParser) o;

        return new EqualsBuilder()
                .append(parsers, that.parsers)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(parsers)
                .toHashCode();
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


    @Override
    public DateParser locale(List<ULocale> locales) {
        List<DateParser> newParsers = new ArrayList<>();

        for(DateParser dateParser : parsers) {
            newParsers.add(dateParser.locale(locales));
        }

        return new CompositeDateParser(newParsers);
    }

    public static DateParser of(List<String> formatSpecs) {

        List<DateParser> parsers = new ArrayList<>();

        for(String formatSpec : formatSpecs) {
            int n = formatSpec.indexOf(":");
            String protocol = formatSpec.substring(0, n);
            String spec = formatSpec.substring(n+1);
            switch (protocol) {
                case DateFormatParser.PROTOCOL: {
                    DateFormatParser dfp = new DateFormatParser(spec);
                    parsers.add(dfp);
                    break;
                }
                case NaturalLanguageDateParser.PROTOCOL:
                    NaturalLanguageDateParser nldp = new NaturalLanguageDateParser(spec);
                    parsers.add(nldp);
                    break;
                default:{
                    throw new RuntimeException("Date parser protocol not found: " + protocol);
                }
            }
        }

        DateParser dateParser = new CompositeDateParser(parsers);

        return dateParser;
    }

    public static DateParser of(List<String> formatSpecs, String timezone) {

        List<DateParser> parsers = new ArrayList<>();

        for(String formatSpec : formatSpecs) {
            int n = formatSpec.indexOf(":");
            String protocol = formatSpec.substring(0, n);
            String spec = formatSpec.substring(n+1);
            switch (protocol) {
                case DateFormatParser.PROTOCOL: {
                    DateFormatParser dfp = new DateFormatParser(spec);
                    parsers.add(dfp);
                    break;
                }
                case NaturalLanguageDateParser.PROTOCOL:
                    NaturalLanguageDateParser nldp = new NaturalLanguageDateParser(spec, timezone);
                    parsers.add(nldp);
                    break;
                default:{
                    throw new RuntimeException("Date parser protocol not found: " + protocol);
                }
            }
        }

        DateParser dateParser = new CompositeDateParser(parsers);

        return dateParser;
    }

    @Override
    public List<String> getFormatSpec() {
        return parsers.stream().flatMap(p->p.getFormatSpec().stream()).collect(Collectors.toList());
    }

}
