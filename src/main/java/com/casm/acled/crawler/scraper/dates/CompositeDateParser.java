package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.SimpleDateFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CompositeDateParser implements DateParser {

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

    public static DateParser of(List<String> formatSpecs) {

        List<DateParser> parsers = new ArrayList<>();

        for(String formatSpec : formatSpecs) {
            int n = formatSpec.indexOf(":");
            String protocol = formatSpec.substring(0, n);
            String spec = formatSpec.substring(n+1);
            switch (protocol) {
                case "ISO": {
                    DateFormatParser dfp = new DateFormatParser(spec);
                    parsers.add(dfp);
                    break;
                }
                case "NL":
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


    @Override
    public List<String> getFormatSpec() {
        return parsers.stream().flatMap(p->p.getFormatSpec().stream()).collect(Collectors.toList());
    }

}
