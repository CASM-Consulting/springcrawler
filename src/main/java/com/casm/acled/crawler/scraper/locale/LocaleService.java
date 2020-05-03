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
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LocaleService {

    protected static final Logger logger = LoggerFactory.getLogger(LocaleService.class);

    private final Map<String, Set<TimeZone>> timeZonesByCountry;
    private final Map<String, Set<ULocale>> localesByCountry;

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

    public String getCountry(Source  source){
        String country = source.get(Source.COUNTRY);
        if(countryMap.containsKey(country)) {
            country = countryMap.get(country);
        }
        return country;
    }


    public Optional<ULocale> determineLocale(String country) {
        Optional<ULocale> maybeLocale = Optional.empty();

        if(localesByCountry.containsKey(country)) {

            Set<ULocale> possibleLocales = localesByCountry.get(country);

            if(possibleLocales.size()==1) {
                ULocale locale = ImmutableList.copyOf(possibleLocales).get(0);
                maybeLocale = Optional.of(locale);
            } else {
//                System.out.println(country);
//                System.out.println(possibleLocales);

                throw new ReportingException(Report.of(Event.LOCALE_NOT_FOUND).message("country %s - possible %s", country, possibleLocales.size()));
            }
        } else {
            reporting.report(Report.of(Event.COUNTRY_NOT_FOUND).message("country %s", country));
        }

        return maybeLocale;
    }

    public Optional<ULocale> determineLocale(Source source) {

        String country = getCountry(source);
        try {

            return determineLocale(country);
        } catch (ReportingException e) {
            reporting.report(e.get().id(source.id()).type(Source.class.getName()));
            return Optional.empty();
        }
    }

    public Optional<TimeZone> determineTimeZone(Source source) {

        Optional<TimeZone> maybeTimezone = Optional.empty();

        String country = getCountry(source);

        if(timeZonesByCountry.containsKey(country)) {

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
        return availableTimezones;
    }

    public void allSourcesDetermineLocalesAndTimeZones(List<Source> sources) {

        LocaleService dph = new LocaleService();
        for(Source source : sources) {
            Optional<TimeZone> maybeTimeZone = dph.determineTimeZone(source);
            Optional<ULocale> maybeLocale = dph.determineLocale(source);

            if(!maybeTimeZone.isPresent()) {
//                System.out.println(source);
            }

            if(!maybeLocale.isPresent()) {
//                System.out.println(source);
            }
        }
    }

    private void determineSourceLocalesAndListTimeZones(String listName) {

        SourceList sourceList = sourceListDAO.getBy(SourceList.LIST_NAME, listName).get(0);
        List<Source> sources = sourceDAO.byList(sourceList);
        LocaleService dph = new LocaleService();

        dph.allSourcesDetermineLocalesAndTimeZones(sources);
    }

    public void allSourcesDetermineLocalesAndTimeZones() {
        for(Desk desk : deskDAO.getAll()) {

            List<SourceList> lists = sourceListDAO.byDesk(desk.id());

            for(SourceList list : lists) {

                determineSourceLocalesAndListTimeZones(list.get(SourceList.LIST_NAME));
            }
        }
    }
}
