package com.casm.acled.entities.article.versions;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.event.Event;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.validation.FieldValidators;
import com.casm.acled.entities.validation.FrontendValidators;
import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

@Component
public class Article_v1 extends Article {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
//            .business()
//            .maybeHistorical()
            .add(builder(DATE, "Date", LocalDate.class)
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .build())

            .add(builder(SOURCE_ID, "Source", Integer.class)
                    .displayType("source")
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .build())

            .add(builder(URL, "URL",String.class)
                    .validators(FieldValidators.ofFrontend(
                            FrontendValidators.Field.URL,
                            FrontendValidators.Field.CHECK_ARTICLE_SOURCE_DOMAIN,
                            FrontendValidators.Field.REQUIRED))
                    .build())

            .add(builder(TITLE, "Title", String.class)
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .build())

            .add(builder(TEXT, "Text", String.class)
                    .displayType("textarea")
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .build())

            .add(EVENT_STUB_IDS, "Event Highlights", String.class, "highlightable")

            .add(builder(NOTES, "Notes", String.class)
                    .displayType("textarea")
                    .build())

            .add(builder(CRAWL_DATE, "Date Collected", LocalDate.class)
                    .putMeta("hidden", true)
                    .build())
            .add(builder(CRAWL_DEPTH, "Found at depth", Integer.class)
                    .putMeta("hidden", true)
                    .build())
            .add(builder(SCRAPE_DATE, "Raw Article Date", String.class)
                    .putMeta("hidden", true)
                    .build())
            .add(builder(SCRAPE_KEYWORD_HIGHLIGHT, "Keyword Highlight", String.class)
                    .displayType("html")
                    .build())
            .add(builder(SCRAPE_RAW_HTML, "Raw markup of page", String.class)
                    .hide(Process.ALL)
                    .displayType("html")
                    .build())
    ;

    public Article_v1(){
        super(SPECIFICATION,
                "v1",
                ImmutableMap.of(),
                null);
    }

    public Article_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }

    public Article_v1(Map<String, Object> data, Integer id, List<Event> events, Source source) {
        super(SPECIFICATION, "v1", data, id, events, source);
    }
}
