package com.casm.acled.entities.source.versions;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntitySpecification;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.casm.acled.entities.validation.FieldValidators;
import com.casm.acled.entities.validation.FrontendValidators;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.casm.acled.entities.EntityField.builder;

@Component
public class Source_v1 extends Source {

    private static final EntitySpecification SPECIFICATION = EntitySpecification.empty()
            .add(builder(NAME, "Name", String.class)
                    .displayType("suggestValue")
                    .validators(FieldValidators.ofFrontend(FrontendValidators.Field.REQUIRED))
                    .putMeta("minimisable", false)
                    .build())
            .add(builder(LINK, "Link", String.class)
                    .displayType("link")
                    .putMeta("minimisable", false)
                    .build())
            .add(NOTES, "Notes", String.class)
            .add(STANDARD_NAME, "Standard Name", String.class, "suggestValue")
            .add(REGION, "Region", String.class, "suggestValue")
            .add(COUNTRY, "Country", String.class, "suggestValue")
            .add(ADMIN1, "Admin 1", String.class, "suggestValue")
            .add(SCALE, "Scale", String.class, "suggestValue")
            .add(ALIAS, "Alias", String.class, "suggestValue")
            .add(builder(VERIFIED, "Verified", Boolean.class)
                    .hide(Process.ADD_SOURCE)
                    .edit(Process.ENTITY_REVIEW)
                    .build())
//            .add(VERIFIED, "Verified", Boolean.class, "label")
            .add(builder(EXAMPLE_URLS, "Example Article URLs", List.class )
                    .encodingException((n, oc)->{
                        String s = n.asText();
                        JsonParser p = oc.getFactory().createParser(s);
                        JsonNode node = p.getCodec().readTree(p);
                        return p.getCodec().treeToValue(node, List.class);
                    })
                    .build())
            .add(builder(SEED_URLS, "Seed URLs", List.class )
                    .encodingException((n, oc)->{
                        String s = n.asText();
                        JsonParser p = oc.getFactory().createParser(s);
                        JsonNode node = p.getCodec().readTree(p);
                        return p.getCodec().treeToValue(node, List.class);
                    })
                    .build())
            .add(builder(CRAWL_RECRAWL_PATTERN, "White List Recrawl Pattern", String.class)
                    .build())
            .add(TIMEZONE, "Timezone", String.class)
            .add(DATE_FORMAT, "Date Format", List.class)
            .add(builder(LANGUAGES, "Language", List.class)
                    .encodingException((n, oc)-> Arrays.asList(n.asText().split(";")))
                    .build())
            .add(LOCALES, List.class)
            .add(COUNTRY_CODES,"Country Code", String.class)
            .add(CRAWL_DISABLED, Boolean.class)
            .add(builder(CRAWL_SCHEDULE, "Crawl Schedule", String.class)
                    .hide(Process.ALL)
                    .build())
            .add(builder(CRAWL_JOB_ID, "Crawl Job ID", Integer.class)
                    .hide(Process.ALL)
                    .build())
            .add(builder(CRAWL_DEPTH, "Crawl Depth", Integer.class).hide(Process.ALL).build())
            .add(builder(CRAWL_OFF_DOMAIN, "Crawl off domain", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_EXCLUDE_PATTERN,"Exclude Pattern", String.class).hide(Process.ALL).build())
            .add(builder(CRAWL_IGNORE_ROBOTS, "Ignore Robots", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_IGNORE_SITEMAP, "Ignore Sitemap", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_INCLUDE_SUBDOMAINS, "Include Subdomains", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_SCRAPER_PATH, "Path to scraper", String.class).hide(Process.ALL).build())
            .add(builder(CRAWL_DISABLE_SITEMAP_DISCOVERY, "Disable Sitemap Discovery", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_DISABLE_SITEMAPS, "Disable Sitemaps", Boolean.class).hide(Process.ALL).build())
            .add(builder(CRAWL_SITEMAP_LOCATIONS, "Sitemap Locations", List.class)
                    .encodingException((n, oc)->{
                        String s = n.asText();
                        JsonParser p = oc.getFactory().createParser(s);
                        JsonNode node = p.getCodec().readTree(p);
                        return p.getCodec().treeToValue(node, List.class);
                    })
                    .hide(Process.ALL)
                    .build())

            .unique(ImmutableSet.of(STANDARD_NAME))
//            .deletable()
            .add(builder(SCRAPER_RULE_ARTICLE, "Article Rule", String.class).hide(Process.ALL).build())
            .add(builder(SCRAPER_RULE_TITLE, "Title Rule", String.class).hide(Process.ALL).build())
            .add(builder(SCRAPER_RULE_DATE, "Date Rule", String.class).hide(Process.ALL).build())

            ;

    public Source_v1(){
        super(SPECIFICATION, "v1", ImmutableMap.of(), null);
    }
    public Source_v1(Map<String, Object> data, Integer id) {
        super(SPECIFICATION, "v1", data, id);
    }
    public Source_v1(Map<String, Object> data, Integer id, List<SourceList> sourceLists) {
        super(SPECIFICATION, "v1", data, id, sourceLists);
    }

}
