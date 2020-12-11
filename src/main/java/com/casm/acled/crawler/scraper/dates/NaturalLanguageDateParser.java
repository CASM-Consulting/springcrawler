package com.casm.acled.crawler.scraper.dates;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * TODO: none of these work in other lang - farm this out to python date parser https://pypi.org/project/dateparser/ over HTTP
 */
class NaturalLanguageDateParser implements DateParser {

    protected static final Logger logger = LoggerFactory.getLogger(NaturalLanguageDateParser.class);

    public static final String PROTOCOL = "NL";

    private static final String PARSING_SERVICE = "http://localhost:5555/parse";
    private static final String PARSING_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String languages;

    private String timezone;

    private final Pattern triggers;
    private final String spec;

    /// for NL parser, timezone is required, so remove constructors without timezone

    public NaturalLanguageDateParser(String spec, String timezone) {
        this(spec, Lists.newArrayList(ULocale.getDefault()), timezone);
    }

    /// for NL parser, timezone is required, so remove constructors without timezone

    public NaturalLanguageDateParser(String spec, List<ULocale> locales, String timezone) {
        this.spec = spec;
        this.timezone = timezone;

        String delim = Pattern.quote(spec.substring(0,1));
        String[] parts = spec.split(delim);

        String pattern = parts[1];
        this.triggers = Pattern.compile(pattern);

        if(parts.length > 2 && !parts[2].isEmpty()) {
            locales.add(new ULocale(parts[2]));
        }

        languages = "[\""+locales.stream().map(ULocale::getLanguage).collect(Collectors.joining("\",\""))+"\"]";
    }

    @Override
    public NaturalLanguageDateParser locale(List<ULocale> locales) {
        return new NaturalLanguageDateParser(spec, locales, this.timezone);
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        boolean makeAttempt = false;
        Optional<LocalDateTime> attempt = Optional.empty();
        if(triggers.matcher(date).find()) {
            makeAttempt = true;
        }
        if(makeAttempt) {

            WebClient webClient = WebClient.create(PARSING_SERVICE,
                    Collections.singletonList(new JacksonJsonProvider()))
                    .accept(MediaType.APPLICATION_JSON_TYPE);

            webClient.query("relative_expression", date);
            webClient.query("languages", languages);
            webClient.query("timezone", timezone);

            Response response = webClient.get();
            int status = response.getStatus();
            if(status >= 200 && status < 300) {
                Map<String,String> data = response.readEntity(new GenericType<Map<String,String>>(){});

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PARSING_FORMAT);

                LocalDateTime parsed = LocalDateTime.parse(data.get("parsed"), formatter);

                attempt = Optional.of(parsed);
            } else {
                logger.info("NL date parsed failed {}", date);
            }
        }
        return attempt;
    }

    @Override
    public List<String> getFormatSpec() {
        return ImmutableList.of(PROTOCOL+":"+spec);
    }

    public static void main(String[] args) {

        String relativeExpression = "4 hours ago";

        NaturalLanguageDateParser nldp = new NaturalLanguageDateParser("ago", "UTC+3");

        Optional<LocalDateTime> maybeDate = nldp.parse(relativeExpression);

        System.out.println(maybeDate.get());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        NaturalLanguageDateParser that = (NaturalLanguageDateParser) o;

        return new EqualsBuilder()
                .append(languages, that.languages)
                .append(spec, that.spec)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(languages)
                .append(spec)
                .toHashCode();
    }
}
