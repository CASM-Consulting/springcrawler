package com.casm.acled.crawler.scraper.locale;

import com.casm.acled.crawler.reporting.Event;
import com.casm.acled.crawler.reporting.Report;
import com.casm.acled.crawler.reporting.Reporter;
import com.casm.acled.crawler.reporting.ReportingException;
import com.casm.acled.dao.entities.DeskDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.desk.Desk;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocaleService {

    protected static final Logger logger = LoggerFactory.getLogger(LocaleService.class);

    private final Map<String, Set<TimeZone>> timeZonesByCountry;
    private final Map<String, Set<ULocale>> localesByCountry;
    private final Map<String, Set<ULocale>> localesByLanguage;

    @Autowired
    private DeskDAO deskDAO;
    @Autowired
    private SourceListDAO sourceListDAO;
    @Autowired
    private SourceDAO sourceDAO;
    @Autowired
    private Reporter reporting;

    private static final Map<String,String> countryMap = ImmutableMap.of(
            "Palestine", "Palestinian Territories"
    );

    public LocaleService() {
        timeZonesByCountry = timeZonesByCountry();
        localesByCountry = localesByCountry();
        localesByLanguage = localesByLanguage();
    }

    private static class ComparableTimeZone {

        private final TimeZone timeZone;
        public ComparableTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComparableTimeZone that = (ComparableTimeZone) o;
            return new EqualsBuilder()
                    .append(timeZone.getRawOffset(), that.timeZone.getRawOffset())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(timeZone.getRawOffset())
                    .toHashCode();
        }

        public TimeZone get() {
            return timeZone;
        }
    }

    private Optional<TimeZone> collapseTimeZonesIfPossible(Set<TimeZone> possibleTimeZones) {
        Set<ComparableTimeZone> collapsed = new HashSet<>();

        for(TimeZone timeZone : possibleTimeZones) {
            collapsed.add(new ComparableTimeZone(timeZone));
        }

        Optional<TimeZone> maybeTimeZone = Optional.empty();

        if(collapsed.size()==1) {
            maybeTimeZone = Optional.of(ImmutableList.copyOf(collapsed).get(0).get());
        }

        return maybeTimeZone;
    }

    public String getCountry(Source source){
        String country = source.get(Source.COUNTRY);
        if(country == null) {
            return country;
        }
        if(countryMap.containsKey(country)) {
            country = countryMap.get(country);
        }

        country = country.replaceAll("and", "&");

        return country;
    }


    public Set<ULocale> determineLocaleByCountry(String country) {
        Set<ULocale> locales = new HashSet<>();

        if(localesByCountry.containsKey(country)) {

            locales = localesByCountry.get(country);

            if(locales.isEmpty()) {
                throw new ReportingException(Report.of(Event.LOCALE_NOT_FOUND).message("country %s - possible %s", country, locales.size()));
            }
        } else {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND).message("country %s", country));
        }

        return locales;
    }

    public Set<ULocale> determineLocaleByLanguage(String language) {
        Set<ULocale> locales = new HashSet<>();

        if(localesByLanguage.containsKey(language)) {

            locales = localesByLanguage.get(language);

            if(locales.isEmpty()) {
                throw new ReportingException(Report.of(Event.LOCALE_NOT_FOUND).message("language %s - possible %s", language, locales.size()));
            }
        } else {
            throw new ReportingException(Report.of(Event.LANGUAGE_NOT_FOUND).message(language));
        }

        return locales;
    }

    public Set<ULocale> candidateLocales(Source source) {
        Set<ULocale> locales = new HashSet<>();
        String country = getCountry(source);
        List<String> languages = source.get(Source.LANGUAGES);

        if(languages != null) {
            for(String language : languages) {
                if(language.equalsIgnoreCase("english")) {
                    locales.add(new ULocale("en"));
                } else {
                    try {
                        locales.addAll(determineLocaleByLanguage(language));
                    } catch (ReportingException e) {
                        reporting.report(e.get().id(source.id()).type(Source.class.getName()));
                    }
                }
            }
        }

        if(locales.isEmpty() && country != null) {
            try {
                locales = determineLocaleByCountry(country);
            } catch (ReportingException e) {
                reporting.report(e.get().id(source.id()).type(Source.class.getName()));
            }
        }

        return locales;
    }

    public Set<TimeZone> candidateTimeZones(Source source) {

        Set<TimeZone> possibleZones = new HashSet<>();

        String country = getCountry(source);
        if(country == null) {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND,  source.id(), "Source","%s - %s", source.get(Source.NAME), source.get(Source.COUNTRY)));
        } else if(timeZonesByCountry.containsKey(country)) {

            possibleZones = timeZonesByCountry.get(country);


        } else {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND,  source.id(), "Source","%s - %s", source.get(Source.NAME), country));
        }

        return possibleZones;
    }

    public Optional<TimeZone> determineTimeZone(Source source) {

        Optional<TimeZone> maybeTimezone = Optional.empty();

        String country = getCountry(source);
        if(country == null) {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND,  source.id(), "Source","%s - %s", source.get(Source.NAME), source.get(Source.COUNTRY)));
        } else if(timeZonesByCountry.containsKey(country)) {

            Set<TimeZone> possibleZones = timeZonesByCountry.get(country);

            if(possibleZones.size()==1) {
                TimeZone timeZone = ImmutableList.copyOf(possibleZones).get(0);
                maybeTimezone = Optional.of(timeZone);
            } else {
                maybeTimezone = this.collapseTimeZonesIfPossible(possibleZones);

                if(!maybeTimezone.isPresent()) {
//                    System.out.println(country);
//                    System.out.println(possibleZones);
                    reporting.report(Report.of(Event.TIMEZONE_NOT_FOUND,  source.id(), "Source", "%s - %s - possible %s", source.get(Source.NAME), country, possibleZones.size()));

                }
            }
        } else {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND,  source.id(), "Source","%s - %s", source.get(Source.NAME), country));
        }

        return maybeTimezone;
    }

    public static Map<String, Set<ULocale>> localesByLanguage() {
        Map<String, Set<ULocale>> availableLocales = new HashMap<>();

        for (ULocale locale : ULocale.getAvailableLocales()) {
            final String language = locale.getDisplayLanguage();

            Set<ULocale> locales = availableLocales.get(language);

            if(locales == null) {
                locales = new HashSet<>();
                availableLocales.put(language, locales);
            }

            locales.add(locale);
        }
        return availableLocales;
    }

    public static Map<String, Set<ULocale>> localesByCountry() {
        Map<String, Set<ULocale>> availableLocales = new HashMap<>();

        for (ULocale locale : ULocale.getAvailableLocales()) {
            final String countryName = locale.getDisplayCountry();

            Set<ULocale> locales = availableLocales.get(countryName);

            if(locales == null) {
                locales = new HashSet<>();
                availableLocales.put(countryName, locales);
            }

            locales.add(locale);
        }
        return availableLocales;
    }

    public static Map<String, Set<TimeZone>> timeZonesByCountry()
    {
        Map<String, Set<TimeZone>> availableTimezones =
                new HashMap<String, Set<TimeZone>>();

        // Loop through all available locales

        for (ULocale locale : ULocale.getAvailableLocales())
        {
            final String countryCode = locale.getCountry();
            final String countryName = locale.getDisplayCountry();

            // Locate the timezones added for this country so far
            // (This can be moved to inside the loop if depending
            // on whether country with no available timezones should
            // be in the result map with an empty set,
            // or not included at all)

            Set<TimeZone> timezones = availableTimezones.get(countryName);
            if (timezones==null)
            {
                timezones = new HashSet<>();
                availableTimezones.put(countryName, timezones);
            }

            // Find all timezones for that country (code) using ICU4J

            for (String id : com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode))
            {
                // Add timezone to result map

                timezones.add(TimeZone.getTimeZone(id));
            }
        }

        availableTimezones.put("Kosovo", ImmutableSet.of(TimeZone.getTimeZone("CET")));

        return availableTimezones;
    }


    public void autoAssignLocalesAndTimeZones(Source source) {

        Optional<TimeZone> maybeTimeZone = determineTimeZone(source);
        Set<ULocale> locales = candidateLocales(source);

        if(maybeTimeZone.isPresent()) {
            source = source.put(Source.TIMEZONE, maybeTimeZone.get().getID());
            sourceDAO.upsert(source);
        } else {
            reporting.report(Report.of(Event.TIMEZONE_NOT_FOUND, source.id(), Source.class.getName()).message(getCountry(source)));
        }

        if(!locales.isEmpty()) {
            source = source.put(Source.LOCALES, locales.stream().map(ULocale::getName).collect(Collectors.toList()));
            sourceDAO.upsert(source);
//                System.out.println(source);
        } else {
            reporting.report(Report.of(Event.LOCALE_NOT_FOUND, source.id(), Source.class.getName()).message(getCountry(source)));
        }

    }

    public void assignLocale(Source source, ULocale locale) {
        source = source.put(Source.LOCALES, Lists.newArrayList(locale.getName()));
        sourceDAO.upsert(source);
    }


    public void assignTimezone(Source source, ZoneId zoneId) {
        source = source.put(Source.TIMEZONE, zoneId.getId());
        sourceDAO.upsert(source);
    }

    public void autoAssignLocalesAndTimeZones(List<Source> sources) {

        for(Source source : sources) {
            autoAssignLocalesAndTimeZones(source);
        }
    }

    public void determineSourceLocalesAndListTimeZones(String listName) {

        SourceList sourceList = sourceListDAO.getBy(SourceList.LIST_NAME, listName).get(0);
        List<Source> sources = sourceDAO.byList(sourceList);

        autoAssignLocalesAndTimeZones(sources);
    }

    public void autoAssignLocalesAndTimeZones() {
        for(Desk desk : deskDAO.getAll()) {

            List<SourceList> lists = sourceListDAO.byDesk(desk.id());

            for(SourceList list : lists) {

                determineSourceLocalesAndListTimeZones(list.get(SourceList.LIST_NAME));
            }
        }
    }

    public static void main(String[] args) {

        System.out.println(ZoneId.of("Mexico/General"));
    }
}
