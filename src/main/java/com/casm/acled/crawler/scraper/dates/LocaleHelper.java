package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LocaleHelper {

    protected static final Logger logger = LoggerFactory.getLogger(LocaleHelper.class);

    private final Map<String, Set<TimeZone>> timeZonesByCountry;
    private final Map<String, Set<ULocale>> localesByCountry;

    private static final Map<String,String> countryMap = ImmutableMap.of(
            "Palestine", "Palestinian Territories"
    );

    public LocaleHelper() {
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

    public Optional<ULocale> determineLocale(Source source) {

        Optional<ULocale> maybeLocale = Optional.empty();

        String country = getCountry(source);

        if(localesByCountry.containsKey(country)) {

            Set<ULocale> possibleLocales = localesByCountry.get(country);

            if(possibleLocales.size()==1) {
                ULocale locale = ImmutableList.copyOf(possibleLocales).get(0);
                maybeLocale = Optional.of(locale);
            } else {
//                System.out.println(country);
//                System.out.println(possibleLocales);

                logger.error("Time zone not determined: {}/{} - {} : n = {}", source.get(Source.NAME), source.id(),  country, possibleLocales.size());
            }
        } else {
            logger.error("Country not found: {}/{} - {}", source.get(Source.NAME), source.id(), country);
        }


        return maybeLocale;

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
                    logger.error("Time zone not determined: {}/{} - {} : n = {}", source.id(), source.get(Source.NAME),  country, possibleZones.size());
                }
            }
        } else {
            logger.error("Country not found: {}/{} - {}", source.id(), source.get(Source.NAME), country);
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

            for (String id :
                    com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode))
            {
                // Add timezone to result map

                timezones.add(TimeZone.getTimeZone(id));
            }
        }
        return availableTimezones;
    }

    public void determineTimeZones(List<Source> sources) {

        LocaleHelper dph = new LocaleHelper();
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
}
