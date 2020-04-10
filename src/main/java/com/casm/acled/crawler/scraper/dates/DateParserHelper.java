package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.entities.source.Source;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

public class DateParserHelper {

    private final Map<String, Set<TimeZone>> countryTimeZones;

    public DateParserHelper() {
        countryTimeZones = DateUtil.getAvailableTimeZones();
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

    private Optional<TimeZone> collapseIfPossible(Set<TimeZone> possibleTimeZones) {
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

    public Optional<TimeZone> determineTimeZone(Source source) {

        Optional<TimeZone> maybeTimezone = Optional.empty();

        String country = source.get(Source.COUNTRY);
        if(countryTimeZones.containsKey(country)) {

            Set<TimeZone> possibleZones = countryTimeZones.get(country);

            if(possibleZones.size()==1) {
                TimeZone timeZone = ImmutableList.copyOf(possibleZones).get(0);
                maybeTimezone = Optional.of(timeZone);
            } else {
                maybeTimezone = collapseIfPossible(possibleZones);

                if(!maybeTimezone.isPresent()) {
                    System.out.println(country);
                    System.out.println(possibleZones);
                }
            }
        }


        return maybeTimezone;
    }


}
