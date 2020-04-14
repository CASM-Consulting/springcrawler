package com.casm.acled.crawler.scraper.dates;

import com.casm.acled.crawler.spring.CrawlerService;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import org.docx4j.wml.U;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

class DateFormatParser implements DateParser {

    protected static final Logger logger = LoggerFactory.getLogger(DateFormatParser.class);

    private final SimpleDateFormat formatter;

    public DateFormatParser(SimpleDateFormat formatter) {
        this.formatter = formatter;
    }

    @Override
    public Optional<LocalDateTime> parse(String date) {
        Optional<LocalDateTime> attempt = Optional.empty();
        try {
            Date d = formatter.parse(date);
            LocalDateTime parsed = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
            attempt = Optional.of(parsed);
        } catch (ParseException e) {

            DateUtil.logger.warn(e.getMessage());
        }
        return attempt;
    }

    private static SimpleDateFormat fixAMPM(SimpleDateFormat formatter){

        DateFormatSymbols dfs = formatter.getDateFormatSymbols();

        dfs.setAmPmStrings(new String[]{"AM", "PM"});

        formatter.setDateFormatSymbols(dfs);

        return formatter;
    }

    private static SimpleDateFormat processFlag(SimpleDateFormat formatter, String flag) {
        switch (flag) {
            case "AMPM":
                formatter = fixAMPM(formatter);
                break;
            default:
                logger.warn("Unrecognised DateFormatParser flag : " + flag);
                break;
        }

        return formatter;
    }

    public static SimpleDateFormat of(String formatSpec) {
        int n = formatSpec.lastIndexOf(":");
        String[] localeFlags = formatSpec.substring(n+1).split(",");

        ULocale locale = new ULocale(localeFlags[0]);
        // set up the generator
        DateTimePatternGenerator generator
                = DateTimePatternGenerator.getInstance(locale);

//        String pattern = generator.getBestPattern(formatSpec.substring(0, n));
        String pattern = formatSpec.substring(0, n);

        // get a pattern for an abbreviated month and day
        SimpleDateFormat formatter = new SimpleDateFormat(pattern,locale);

//        formatter.setNumberFormat();


        for(int i = 1; i < localeFlags.length; ++i) {
            formatter = processFlag(formatter, localeFlags[i]);
        }

        return formatter;
    }

    public static void main(String[] args) throws Exception {


//        Date d = new SimpleDateFormat("yyyy-MM-DD  HH:mm:ss").parse("1999-05-01 00:00:00");
//        System.out.println(d);
//
//        System.out.println(of("MMMM d, yyyy:en").parse("January 18, 2019"));
//        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm:ss", ULocale.forLanguageTag("en"));
//
//        System.out.println(simpleDateFormat.parse("1 March 2020, 10:10:10"));
        String txt = of("MMMM d, yyyy, h:mm a:np,AMPM").format(new Date(119, 11, 2, 16, 57,0));
        System.out.println(txt);
//        String test = "diciembre 2, 2019, 4:57 pm";

//        System.out.println(of("MMMM d, yyyy, h:mm a:es_ES").parse(of("MMMM d, yyyy, h:mm a:es_ES").format(new Date(119, 11, 2, 16, 57,0))));
//        System.out.println(of("MMMM d, yyyy, h:mm a:es_ES,AMPM").f(test));

    }
}
