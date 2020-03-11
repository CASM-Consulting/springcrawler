package com.casm.acled.crawler.utils;


import com.casm.acled.camunda.BusinessKeys;
import com.casm.acled.configuration.ObjectMapperConfiguration;
import com.casm.acled.crawler.DateFilter;
import com.casm.acled.dao.entities.ArticleDAO;
import com.casm.acled.dao.entities.SourceDAO;
import com.casm.acled.dao.entities.SourceListDAO;
import com.casm.acled.entities.article.Article;
import com.casm.acled.entities.source.Source;
import com.casm.acled.entities.sourcelist.SourceList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import com.mdimension.jchronic.utils.Span;
import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.spring.boot.starter.rest.CamundaBpmRestJerseyAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;


// We have to exclude these classes, because they only work in a web context.
@EnableAutoConfiguration(exclude={CamundaBpmAutoConfiguration.class, CamundaBpmRestJerseyAutoConfiguration.class})
// We need the special object mapper, though.
@Import(ObjectMapperConfiguration.class)
// And we also need the DAOs.
@ComponentScan(basePackages={"com.casm.acled.dao"})
public class Util implements CommandLineRunner {
    protected static final Logger logger = LoggerFactory.getLogger(Util.class);

    @Autowired
    private ArticleDAO articleDAO;

    @Autowired
    private SourceListDAO sourceListDAO;

    @Autowired
    private SourceDAO sourceDAO;

    private static Options opts = new Options(Pointer.PointerType.PAST);
    private static PrettyTimeParser prettyParser = new PrettyTimeParser();
    private static PrettyTimeParser nonamericanPrettyParser = new PrettyTimeParser();

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

            Date d = DateUtil.stringToDate(date);
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

        List<LocalDate> dates = guesses.stream()
                .filter( Optional::isPresent )
                .map( Optional::get )
                .filter( d -> d.isBefore(LocalDate.now()) )
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

    public static LocalDate getDate(String date) {

        date = normaliseDate(date);

        Optional<LocalDate> jchronicGuess = getJChronic(date);
        Optional<LocalDate> prettyGuess = getPretty(date);
        Optional<LocalDate> nattyGuess = getNatty(date);
        Optional<LocalDate> defaultGuess = getDefault(date);

        Optional<LocalDate> bestGuess = resolveBestGuess(ImmutableList.of(jchronicGuess, prettyGuess, nattyGuess, defaultGuess));

        return bestGuess.orElseGet(()->null);
    }

//    public LocalDate simplDateFallback(String dateString) {
//
//    }

    public void recoverArticleDates() {

//        Parser parser = new Parser();



        for(Article article : articleDAO.getAll() ) {


            String[] text = ((String)article.get(Article.TEXT)).split("\n");

            if(text.length <= 1) {
                continue;
            }

            LocalDate localDate = null;
            try {

                localDate = getDate(text[1]);
            } catch (RuntimeException e) {
                continue;
            }

            if(localDate == null) {

                articleDAO.delete(article);
                continue;
            }

            article = article.put(Article.DATE, localDate);

            if(article.hasValue(Article.SOURCE_ID)) {

                int sourceId = article.get(Article.SOURCE_ID);

                List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                for(SourceList sourceList : sourceLists) {

                    article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                }

            }

            articleDAO.update(article);
        }
    }

    private static final String matchingKeywords = "\\b(?:kill|massacre|death|died|dead|bomb|bombed|bombing|rebel|attack|attacked|riot|battle|protest|clash|demonstration|strike|wound|injure|casualty|displace|unrest|casualties|vigilante|torture|march|rape)\\b";

    private void deleteNonMatchingArticles() {
        Pattern pattern = Pattern.compile(matchingKeywords);
        for (Article article : articleDAO.getAll()) {
            String text = article.get(Article.TEXT);

            if(!pattern.matcher(text).find()) {
                System.out.println("REMOVE " + text);
                articleDAO.delete(article);
            } else {

                //System.out.println("KEEP " + text);
            }
        }
    }

    private void linkExisting() {
        Map<String, Source> sources = sourceDAO.getAll().stream().filter(s->s.get(Source.LINK)!=null).collect(Collectors.toMap(s->s.get(Source.LINK), s -> s, (o, o2) -> o));

        for(Article article : articleDAO.getAll()) {
            String url = article.get(Article.URL);
            if(url == null) {
                continue;
            }
            for (Map.Entry<String, Source> e : sources.entrySet()) {

                if (url.contains(e.getKey())) {
                    int sourceId = e.getValue().id();
                    article = article.put(Article.SOURCE_ID, sourceId);

                    List<SourceList> sourceLists = sourceListDAO.bySource(sourceId);

                    LocalDate localDate = article.get(Article.DATE);

                    for(SourceList sourceList : sourceLists) {

                        article = article.businessKey(BusinessKeys.generate(sourceList.get(SourceList.LIST_NAME), localDate));
                    }

                    articleDAO.update(article);
//                    System.out.println("updating " + article.get(Article.URL));
                    break;
                }
            }

        }
    }

    public void run(String... args) throws Exception {
        deleteNonMatchingArticles();
        recoverArticleDates();
        linkExisting();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Util.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}