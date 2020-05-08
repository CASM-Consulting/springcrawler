package com.casm.acled.crawler.scraper.dates;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;

public class DateFormatParserTest {

    @Test
    public void dateTimeTest2() {

        DateFormatParser dfp = new DateFormatParser("/dd 'de' MMMM 'de' yyyy 'a las' HH:mm'h'/es_ES");

        System.out.println(dfp.formatter().format(new Date()));

        Optional<LocalDateTime> date = dfp.parse("17 de marzo de 2017 a las 11:03h");
        System.out.println(date.get());

    }


    @Test
    public void dateTimeTest() {

        NumberFormat nf = NumberFormat.getIntegerInstance(new ULocale("zh"));
//        System.out.println(new DateFormatParser("dd-MMM-yyyy:en,ORD").parse("10th-Oct-2019"));
//        String txt = new DateFormatParser("EEEE/MMMM/yyyy:zh").formatter().format(new Date(120, 3, 1, 0, 0,0));

//        Optional<LocalDateTime> date = new DateFormatParser("/dd MMM yyyy HH:mm a z/en_GB/BST/RE.*Updated: (.*)").parse("Published: 26 Nov 2019 01:43 AM BdST Updated: 26 Nov 2019 01:44 AM BdST");
        Optional<LocalDateTime> date = new DateFormatParser("/dd MMM yyyy/", ULocale.forLanguageTag("en_GB")).parse("26 Nov 2019");
        System.out.println(date.get());

//        Date d = new SimpleDateFormat("yyyy-MM-DD  HH:mm:ss").parse("1999-05-01 00:00:00");
//        System.out.println(d);
//
//        System.out.println(new DateFormatParser("MMMM d, yyyy, HH:mm a:en").parse("October 20, 2019, 10:20 pm"));
//        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM yyyy, HH:mm:ss", ULocale.forLanguageTag("en"));
//
//        System.out.println(simpleDateFormat.parse("1 March 2020, 10:10:10"));
//        String txt = of("MMMM d, yyyy, h:mm a:np,AMPM").format(new Date(119, 11, 2, 16, 57,0));
//        System.out.println(txt);
//        String test = "diciembre 2, 2019, 4:57 pm";

//        System.out.println(of("MMMM d, yyyy, h:mm a:es_ES").parse(of("MMMM d, yyyy, h:mm a:es_ES").format(new Date(119, 11, 2, 16, 57,0))));
//        System.out.println(of("MMMM d, yyyy, h:mm a:es_ES,AMPM").f(test));

    }

}