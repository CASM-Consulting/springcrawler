package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.text.*;
import com.ibm.icu.util.ULocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DateFormatParser implements DateParser {

    protected static final Logger logger = LoggerFactory.getLogger(DateFormatParser.class);

    private final SimpleDateFormat formatter;

    private final String formatSpec;

    private boolean removeOrdinals;
    private boolean fixBST;
    private final Pattern ordinalPattern = Pattern.compile("(\\d+)(?:st|nd|rd|th)");
    private final Pattern bstPattern = Pattern.compile("(?i)bdst");
    private Pattern extractPattern;

    public DateFormatParser(String formatSpec) {
        this.formatSpec = formatSpec;
        removeOrdinals = false;
        fixBST = false;
        extractPattern = null;
        formatter = buildSimpleDateFormat(formatSpec);
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        Optional<LocalDateTime> attempt = Optional.empty();
        date = preProcessDate(date);
        try {
            Date d = formatter.parse(date);
            LocalDateTime parsed = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
            attempt = Optional.of(parsed);
        } catch (ParseException e) {

            logger.warn(e.getMessage());
        }
        return attempt;
    }

    public SimpleDateFormat formatter() {
        return formatter;
    }

    private String preProcessDate(String date) {
        if(removeOrdinals) {
            date = remoteOrdinals(date);
        }
        if(extractPattern != null) {
            Matcher m = extractPattern.matcher(date);
            if(m.matches()) {
                date = m.group(1);
            }
        }
        if(fixBST) {
            date = bstPattern.matcher(date).replaceAll("BST");
        }
        return date;
    }

    private String remoteOrdinals(String date) {
        String replacement = ordinalPattern.matcher(date).replaceAll("$1");
        return replacement;
    }

    private static SimpleDateFormat fixAMPM(SimpleDateFormat formatter){

        DateFormatSymbols dfs = formatter.getDateFormatSymbols();

        dfs.setAmPmStrings(new String[]{"AM", "PM"});

        formatter.setDateFormatSymbols(dfs);

        return formatter;
    }

    private SimpleDateFormat processFlag(SimpleDateFormat formatter, String flag) {
        if(flag.equals("AMPM")) {
            formatter = fixAMPM(formatter);
        } else if(flag.equals("ORD")) {
            removeOrdinals = true;
        } else if(flag.startsWith("RE")) {
            extractPattern = Pattern.compile(flag.substring(2));
        } else if(flag.startsWith("BST")) {
            fixBST = true;
        } else {
            logger.warn("Unrecognised DateFormatParser flag : " + flag);
        }

        return formatter;
    }

    @Override
    public List<String> getFormatSpec() {
        return ImmutableList.of(formatSpec);
    }

    public SimpleDateFormat buildSimpleDateFormat(String formatSpec) {
        String delim = formatSpec.substring(0,1);

        String[] parts = formatSpec.split(delim);
        String pattern = parts[1];
        ULocale locale = new ULocale(parts[2]);

        // set up the generator
        DateTimePatternGenerator generator
                = DateTimePatternGenerator.getInstance(locale);

        // get a pattern for an abbreviated month and day
        SimpleDateFormat formatter = new SimpleDateFormat(pattern,locale);

        for(int i = 3; i < parts.length; ++i) {
            formatter = processFlag(formatter, parts[i]);
        }

        return formatter;
    }


}
