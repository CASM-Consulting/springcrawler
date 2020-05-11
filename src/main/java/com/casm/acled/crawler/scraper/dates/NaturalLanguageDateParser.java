package com.casm.acled.crawler.scraper.dates;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.cxf.jaxrs.client.WebClient;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * TODO: none of these work in other lang - farm this out to python date parser https://pypi.org/project/dateparser/ over HTTP
 */
class NaturalLanguageDateParser implements DateParser {

    public static final String PROTOCOL = "NL";

    private static final String PARSING_SERVICE = "http://localhost:5555/parse";
    private static final String PARSING_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String languages;

    private final String[] triggers;
    private final String spec;

    public NaturalLanguageDateParser(String spec) {
        this(spec, ImmutableList.of(ULocale.getDefault()));
    }
    public NaturalLanguageDateParser(String spec, List<ULocale> locales) {
        this.spec = spec;
        this.triggers = spec.split(",");

        languages = "[\""+locales.stream().map(ULocale::getLanguage).collect(Collectors.joining("\",\""))+"\"]";
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

            WebClient webClient = WebClient.create(PARSING_SERVICE,
                    Collections.singletonList(new JacksonJsonProvider()))
                    .accept(MediaType.APPLICATION_JSON_TYPE);

            webClient.query("relative_expression", date);
            webClient.query("languages", languages);

            Response response = webClient.get();
            int status = response.getStatus();
            if(status >= 200 && status < 300) {
                Map<String,String> data = response.readEntity(new GenericType<Map<String,String>>(){});

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PARSING_FORMAT);

                LocalDateTime parsed = LocalDateTime.parse(data.get("parsed"), formatter);

                attempt = Optional.of(parsed);
            } else {
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

    public static void main(String[] args) {

        String relativeExpression = "4 hours ago";

        NaturalLanguageDateParser nldp = new NaturalLanguageDateParser("ago");

        Optional<LocalDateTime> maybeDate = nldp.parse(relativeExpression);

        System.out.println(maybeDate.get());
    }
}
