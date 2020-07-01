package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class DateUtil {

    protected static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String[] timeFormats = {"HH:mm:ss", "HH:mm"};
    private static final String[] dateSeparators = {"/", "-", " "};

    private static final String DMY_FORMAT = "dd{sep}MM{sep}yyyy";
    private static final String YMD_FORMAT = "yyyy{sep}MM{sep}dd";
    private static final String SEP = "{sep}";

    private static final String ymd_template = "\\d{4}{sep}\\d{2}{sep}\\d{2}.*";
    private static final String dmy_template = "\\d{2}{sep}\\d{2}{sep}\\d{4}.*";
    private static Options opts = new Options(Pointer.PointerType.PAST);
    private static PrettyTimeParser prettyParser = new PrettyTimeParser();
    private static Parser nattyParser = new Parser();
    private static Map<String, String> months = ImmutableMap.<String,String>builder()
            .put("كَانُون ٱلثَّانِي",  "January")
            .put("شُبَاط", "February")
            .put("آذَار" ,"March")
            .put("نَيْسَان" , "April")
            .put("أَيَّار"	, "May")
            .put("حَزِيرَان" , "June")
            .put("تَمُّوز"	, "July")
            .put("آب" , "August")
            .put("أَيْلُول" , "September")
            .put("تِشْرِين ٱلْأَوَّل" , "October")
            .put("تِشْرِين ٱلثَّانِي" , "November")
            .put("كَانُون ٱلْأَوَّل" , "December")

            .put("يناير" , "January")
            .put("فبراير" , "February")
            .put("مارس" , "March")
            .put("أبريل" , "April")
            .put("إبريل" , "April")
            .put("مايو" , "May")
            .put("يونيو" , "June")
            .put("يونية", "June")
            .put("يوليو" , "July")
            .put("يولية" , "July")
            .put("أغسطس", "August")
            .put("سبتمبر", "September")
            .put("أكتوبر" , "October")
            .put("نوفمبر" , "November")
            .put("ديسمبر" , "December")
            .build();
    private static Map<String, String> russianMonths = ImmutableMap.<String,String>builder()
            .put("января",  "January")
            .put("февраля", "February")
            .put("март" ,"March")
            .put("апреля" , "April")
            .put("май"	, "May")
            .put("июня" , "June")
            .put("июля"	, "July")
            .put("августа" , "August")
            .put("сентября" , "September")
            .put("октября" , "October")
            .put("ноября" , "November")
            .put("декабря" , "December")
            .build();
    private static Map<String, String> frenchMonths = ImmutableMap.<String,String>builder()
            .put("janvier",  "January")
            .put("février", "February")
            .put("mars" ,"March")
            .put("avril" , "April")
            .put("mai"	, "May")
            .put("juin" , "June")
            .put("juillet"	, "July")
            .put("août" , "August")
            .put("septembre" , "September")
            .put("octobre" , "October")
            .put("novembre" , "November")
            .put("décembre" , "December")
            .build();
    private static Map<String, String> spanishMonths = ImmutableMap.<String,String>builder()
            .put("enero",  "January")
            .put("febrero", "February")
            .put("marzo" ,"March")
            .put("abril" , "April")
            .put("mayo"	, "May")
            .put("junio" , "June")
            .put("julio"	, "July")
            .put("agosto" , "August")
            .put("septiembre" , "September")
            .put("octubre" , "October")
            .put("noviembre" , "November")
            .put("diciembre" , "December")
            .build();
    private static Map<String, String> germanMonths = ImmutableMap.<String,String>builder()
            .put("januar",  "January")
            .put("februar", "February")
            .put("märz" ,"March")
            .put("april" , "April")
            .put("mai"	, "May")
            .put("juni" , "June")
            .put("juli"	, "July")
            .put("august" , "August")
            .put("september" , "September")
            .put("oktober" , "October")
            .put("november" , "November")
            .put("dezember" , "December")
            .build();

    public static Date stringToDate(String input) {
        Date date = null;
        String dateFormat = getDateFormat(input);
        if (dateFormat == null) {
            input = removeAlpha(input);
            dateFormat = getDateFormat(input);
            if(dateFormat == null) {
                throw new IllegalArgumentException("Date is not in an accepted format " + input);
            }
        }

        for (String sep : dateSeparators) {
            String actualDateFormat = dateFormat.replace(SEP, sep);
            //try first with the time
            for (String time : timeFormats) {
                date = tryParse(input, actualDateFormat + " " + time);
                if (date != null) {
                    return date;
                }
            }
            //didn't work, try without the time formats
            date = tryParse(input, actualDateFormat);
            if (date != null) {
                return date;
            }
        }


        return date;
    }

    private static String getDateFormat(String date) {
        for (String sep : dateSeparators) {
            String ymdPattern = ymd_template.replace(SEP, sep);
            String dmyPattern = dmy_template.replace(SEP, sep);
            if (date.matches(ymdPattern)) {
                return YMD_FORMAT;
            }
            if (date.matches(dmyPattern)) {
                return DMY_FORMAT;
            }
        }
        return null;
    }

    public static String removeAlpha(String messyDate) {
        return messyDate.replaceAll("[a-zA-Z]","");
    }

    public static String toDateString(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_DATE);
    }

    public static LocalDate fromDateString(String date) {
        return LocalDate.from(DateTimeFormatter.ISO_DATE.parse(date));
    }

    private static Date tryParse(String input, String pattern) {
        try {
            return new SimpleDateFormat(pattern).parse(input);
        } catch (ParseException e) {
        }
        return null;
    }

    private static String replaceMonths(Map<String, String> months, String dateStr) {

        for(Map.Entry<String, String> entry : months.entrySet()) {
            dateStr = dateStr.replace(entry.getKey(), entry.getValue());
        }

        return dateStr;
    }

    private static Optional<LocalDate> getJChronic (String date) {
        Span span = Chronic.parse(date, opts);   // Year is treated as 1938 because of PointerType.PAST.
        Optional<LocalDate> result = Optional.empty();
        if (span != null) {
            LocalDate localDate = Instant.ofEpochMilli(span.getBegin()*1000).atZone(ZoneId.of("GMT")).toLocalDate();
            result = Optional.of(localDate);
        }
        return result;
    }

    private static Optional<LocalDate> getPretty (String date) {
        List<Date> dates = prettyParser.parse(date);
        Optional<LocalDate> result = Optional.empty();

        if(!dates.isEmpty()) {
            LocalDate localDate = dates.get(dates.size()-1).toInstant().atZone(ZoneId.of("GMT")).toLocalDate();

            if(localDate.isAfter(LocalDate.now())) {
                localDate = dates.get(0).toInstant().atZone(ZoneId.of("GMT")).toLocalDate();
            }
            result = Optional.of(localDate);
        }
        return result;
    }

    private static Optional<LocalDate> getNatty (String date) {

        Optional<LocalDate> result = Optional.empty();

        List<DateGroup> dateGroups = nattyParser.parse(date);
        if(dateGroups.size() > 0) {
            DateGroup dateGroup = dateGroups.get(dateGroups.size() - 1);
            List<Date> ds = dateGroup.getDates();
            Date d = ds.get(ds.size() - 1);
            LocalDate localDate = d.toInstant().atZone(ZoneId.of("GMT")).toLocalDate();
            result = Optional.of(localDate);

        }

        return result;
    }

    private static Optional<LocalDate> getDefault(String date) {
        try {

            Date d = stringToDate(date);
            LocalDate localDate = d.toInstant().atZone(ZoneId.of("GMT")).toLocalDate();

            return Optional.of(localDate);
        } catch (IllegalArgumentException e) {

            return Optional.empty();
        }
    }

    public static String normaliseDate(String date) {

        date = date.replaceAll("\\p{javaSpaceChar}+", " ").trim();

        date = date.replaceAll("UTC\\+[0-9]+:[0-9]+","");

        date = date.replaceAll("\\.","/");

        date = date.replaceAll("-(?!\\p{Digit})", " ")
                .replaceAll("(?<!\\p{Digit})-", " ");

        date = date.replaceAll("(?<=\\p{Alpha}{2,})(?=\\p{Digit})", " ");
        date = date.replace(",", " ");

        date = replaceMonths(months,date);
        date = replaceMonths(russianMonths,date.toLowerCase());
        date = replaceMonths(spanishMonths,date.toLowerCase());
        date = replaceMonths(germanMonths,date.toLowerCase());
        date = replaceMonths(frenchMonths,date.toLowerCase());

        return date;
    }

    private static Optional<LocalDate> resolveBestGuess(List<Optional<LocalDate>> guesses) {

        LocalDate now = LocalDate.now();

        List<LocalDate> dates = guesses.stream()
                .filter( Optional::isPresent )
                .map( Optional::get )
                .filter( d -> d.isBefore(now) || d.equals(now) )
                .collect(Collectors.toList());

        Optional<LocalDate> result = Optional.empty();

        if(dates.size()==1) {
            result = Optional.of(dates.get(0));
        } else if (dates.size() > 1) {
            Map<LocalDate, Long> counts = dates.stream().collect(
                    groupingBy(Function.identity(), counting())
            );

            Optional<Map.Entry<LocalDate, Long>> favourite = counts.entrySet().stream().max(Map.Entry.comparingByValue());

            if(favourite.isPresent()) {
                result = Optional.of(favourite.get().getKey());
            }
        }

        return result;
    }



    public static Optional<LocalDate> getDate(String date) {

        date = normaliseDate(date);

        Optional<LocalDate> jchronicGuess = getJChronic(date);
        Optional<LocalDate> prettyGuess = getPretty(date);
//        Optional<LocalDate> nattyGuess = getNatty(date);
//        Optional<LocalDate> defaultGuess = getDefault(date);

        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess, defaultGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess, nattyGuess, defaultGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, defaultGuess));

        return bestGuess;
    }

    public static Optional<LocalDate> getDateWithoutNormalisation(String date) {

        Optional<LocalDate> jchronicGuess = getJChronic(date);
        Optional<LocalDate> prettyGuess = getPretty(date);
//        Optional<LocalDate> nattyGuess = getNatty(date);
//        Optional<LocalDate> defaultGuess = getDefault(date);

        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess, defaultGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess, nattyGuess, defaultGuess));
//        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, defaultGuess));

        return bestGuess;
    }

    public static Optional<String> extractDateFromLastScrapeJson(Path path) {
        Path lastScrapePath = path.resolve("last_scrape.json");
        Optional<String> date = Optional.empty();
        if(Files.exists(lastScrapePath)) {
            try {
                String name = path.getFileName().toString();
                Map data = new Gson().fromJson(Files.newBufferedReader(lastScrapePath), Map.class);
                date = Optional.ofNullable((String)data.get("field.name/date_0"));
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
        return date;
    }

    public static Map<String, String> extractDatesFromLastScrapeJson(Path scrapersPath) {

        final Map<String, String> dates = new HashMap<>();
        try {
            Files.walk(scrapersPath, 1).forEach(path -> {

                Optional<String> maybeDate = extractDateFromLastScrapeJson(path);
                if(maybeDate.isPresent()) {

                    String name = path.getFileName().toString();
                    dates.put(name, maybeDate.get());
                }

            });
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

        return dates;
    }



    public static void main(String[] args) {
        for(Map.Entry<String,String> date : extractDatesFromLastScrapeJson(Paths.get("allscrapers")).entrySet()) {
//            System.out.println(date.getKey() + "\t\t" + date.getValue());
            System.out.println(date.getValue());
        }
    }
}
