package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DateParsers {

    protected static final Logger logger = LoggerFactory.getLogger(DateParsers.class);

    public static final DateParser dp1 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy/en"
    ));

    public static final DateParser dp2 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, HH:mm a/en"
    ));

    public static final DateParser dp3 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy/en"
    ));

    public static final DateParser dp4 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy/en"
    ));

    public static final DateParser dp5 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy/en"
    ));

    public static final DateParser dp6 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy/en/ORD"
    ));

    public static final DateParser dp7 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy HH:mm a z/en_GB/BST/RE.*Updated: (.*)" //Published: 26 Nov 2019 01:43 AM BdST Updated: 26 Nov 2019 01:44 AM BdST
    ));

    public static final DateParser dp8 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d MMM YYYY hh:mm/en"  //Sunday 27 October 2019 23:56
    ));

    public static final DateParser dp9 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYYY/en/ORD"  //Monday, 3 February 2020
    ));

    public static final DateParser dp10 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d YYYY/en/ORD"  //Monday December 2 2019
    ));

    public static final DateParser dp11 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d YYYY/es" // Noviembre 29 2019
    ));

    public static final DateParser dp12 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY, hh:mm a/en/RE.*Update Date - (.*)"  //Update Date - November 03, 2019, 09:08 PM
    ));

    public static final DateParser dp13 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm Z|en"
    ));

    public static final DateParser dp14 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm|en"
    ));

    // too ambiguous?
    public static final DateParser dp15 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy|en"
    ));

    public static final DateParser dp16 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy/es" //22 septembre 2019
    ));

    public static final DateParser dp17 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d, yyyy/en/ORD" // Monday Sep 30, 2019
    ));

    public static final DateParser dp18 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, HH:mm/ru" // 15 Ноября 2019, 16:15
    ));

    public static final DateParser dp19 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy h:mm:ss a/en/RE.*: (.*)" // Published: September 17, 2019 8:32:39 PM
    ));

    public static final DateParser dp20 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY/fr"
    ));

    public static final DateParser dp21 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY 'г.', HH:MM/ru" // 17 ноября 2019 г., 23:01
    ));

    public static final DateParser dp22 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/fr" // octobre 6, 2019
    ));

    public static final DateParser dp23 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYYY HH:MM/en/ORD" // Tuesday, 04 February 2020 14:15'
    ));

    public static final DateParser dp24 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm dd.MM.YYYY/en" // '16:49 28.01.2020'
    ));

    public static final DateParser dp25 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/en/RE.*updated(?: on)?:? (.+)" // Last updated Sep 21, 2019'
    ));

    public static final DateParser dp26 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a MMM d, YYYY/en/ORD/RE.+ublished(?: at)|(?: on):? (.+)" // 'Published at 06:11 pm November 4th, 2019'
    ));

    public static final DateParser dp27 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY hh:mm/en/ORD" // Nov 17, 2019 22:28
    ));

    public static final DateParser dp28 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY hh:mm/en/ORD/RE.+pdated: (.+)" // Updated: Oct 11, 2019 21:51 IST
    ));

    public static final DateParser dp29 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/en/ORD/RE(.+) [Pp]osted by.*" //sept 11, 2019 Posted by Redaction Economie 0
    ));

    public static final DateParser dp30 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY '|' HH:mm a/en/ORD" // October 27, 2019 | 11:04 AM
    ));

    public static final DateParser dp31 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY hh:mm z/en/ORD/" //October 11, 2019 10:15PM EDT
    ));

    public static final DateParser dp32 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d.m.yy, HH:mm/en" //25.11.2019, 15:20
    ));

    public static final DateParser dp33 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, YYYY/es" //29 septiembre, 2019'
    ));

    public static final DateParser dp34 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|EEE, dd/MM/YYYY, HH:mm|en" //Monday, 11/18/2019, 11:12
    ));

    public static final DateParser dp35 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/YYYY-MM-dd HH:mm:ss/en" //2019-09-18 18:03:20
    ));

    public static final DateParser dp36 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY, h:mma/en/ORD" //29 October 2019, 3:24pm
    ));

    public static final DateParser dp37 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, MMM d, YYYY/en/ORD" //Monday, December 02, 2019
    ));

    public static final DateParser dp38 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY HH:mm/en/ORD" //29 September 2019 16:32
    ));

    public static final DateParser dp39 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d - YYYY/en/ORD" //October 21 - 2019
    ));

    public static final DateParser dp40 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY HH:mm zzz/en/ORD/RE.+dated: (.+)" //
    ));

    public static final DateParser dp41 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY, HH:mm/en/ORD" //October 1, 2019, 17:53
    ));

    public static final DateParser dp42 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/YYYY H'h'mm|en|ORD" //28/09/2019 15h49
    ));

    public static final DateParser dp43 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY, HH:mm z/en/ORD" //7 February 2020, 14:33 UTC
    ));

    public static final DateParser dp44 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM YYYY, HH:mm z/en/ORD"
    ));

    public static final DateParser dp45 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/en/ORD/RE^[-] (.+)" // "- January 30, 2020"
    ));

    public static final DateParser dp46 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYYY h:mm a/en/RE.+date: (.+[AP]M).*" //Thursday, 30 January 2020 5:17 PM  [ Last Update: Thursday, 30 January 2020 5:24 PM ]
    ));

    public static final DateParser dp47 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d MMM YYYY 'at' HH:mm/en/ORD"  //Tue 04 Feb 2020 at 19:42
    ));

    public static final DateParser dp48 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, MMM d,YYYY/en/RE.+dated: (.+)"  //Updated: 01:20, Feb 06,2020'
    ));

    // TODO: why no parse? Regex extracting fine.
    public static final DateParser dp49 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/YYYY hh:mm:ss a z|en|RE.+ate: (.+)"  //'JUBA, May 7 (Agencies) | Publish Date: 5/7/2019 12:04:09 PM IST'
    ));

    public static final DateParser dp50 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/YYYY 'a las' HH:mm|es|REActualizado el:? (.+)"  //'Actualizado el 03/02/2020 a las 11:03'
    ));

    public static final DateParser dp51 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/h:mm a '|' MMM d, YYYY/en/ORD"  //'6:52 PM | August 19, 2018'
    ));

    public static final DateParser dp52 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY '-' HH:mm/en/ORD"  //29 Jan 2020 - 15:54
    ));

    public static final DateParser dp53 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, YYYY '|' h:mm a/en/ORD"  //'04 Feb, 2020 | 9:57 am'
    ));

    public static final DateParser dp54 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/en/RE.*dated (.+)\\)"  // 'by Dr XXX XXX XXXXX , (Last Updated November 9, 2019)'
    ));

    public static final DateParser dp55 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' YYYY/pt/"  // '17 de Junho de 2019'
    ));

    public static final DateParser dp56 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/E, d.M.YYYY. HH:mm/sr/"  // Čet, 13.02.2020. 16:15
    ));

    public static final DateParser dp57 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/YYYY, d MMM, EEE, 'Bakı vaxtı' HH:mm/az/"  // '2020, 25 Fevral, çərşənbə axşamı, Bakı vaxtı 00:34' (Azerbaijani)
    ));

    public static final DateParser dp58 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|EEE, d / MMM / YYYY|es"  // Miércoles, 18 / Oct / 2017
    ));

    public static final DateParser dp59 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§d/M/YYYY HH:mm§en§RE^[|•]\\s+(.+)" // '|   24/02/2020 00:00'
    ));

    public static final DateParser dp60 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' YYYY HH:mm/en/" // '5 de February de 2014 21:46'
    ));

    public static final DateParser dp61 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' YYYY HH:mm/es/" // '5 de febrero de 2014 21:46'
    ));

    public static final DateParser dp62 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§d/M/YYYY '|' H:mm§en§STRIP[0-9]:[0-9][0-9]\\s+Atualizado\\s+" // '21/02/2020 | 8:21'
    ));

    public static final DateParser dp63 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§HH:mm dd.mm.YYYY§el§RE.+обновлено:\\s+(.+)[)]" // '09:58 13.07.2013 (обновлено: 10:02 13.07.2013)'
    ));

    public static final DateParser dp64 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d HH:mm YYYY/en/ORD" // 'November 22 17:36 2018'
    ));

    public static final DateParser dp65 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY hh:mm a zzz/en/ORD" // '20 Feb 2020 11:42 AM GMT'
    ));

    public static final DateParser dp66 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/'Published at :' MMM dd, YYYY/en/ORD" // 'Published at : February 12, 2020
    ));

    public static final DateParser dp67 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY '|' H:mm 'h'/es/ORD" // '26 Dic 2019 | 4:45 h'
    ));

    public static final DateParser dp68 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, d MMM YYYY/ru/ORD" //08:32, 16 января 2020
    ));

    public static final DateParser dp69 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYYY/ru/ORD" //'6 декабря 2019'
    ));

    public static final DateParser dp70 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY/es/ORD" //'marzo 16, 2020'
    ));

    public static final DateParser dp71 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' YYYY '-' HH:mm/es/ORD/RE^[|]\\s+(.+)" //'| 22 de Febrero de 2020 - 00:00'
    ));

    public static final DateParser dp72 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, YYYY HH:mm/en/ORD/RE^Published:\\s+(.+\\d)$" //'Published:  February 20, 2020 11:41'
    ));

    public static final List<DateParser> ALL = ImmutableList.of(
            dp1,
            dp2,
            dp3,
            dp4,
            dp5,
            dp6,
            dp7,
            dp8,
            dp9,
            dp10,
            dp11,
            dp12,
            dp13,
            dp14,
            dp15,
            dp16,
            dp17,
            dp18,
            dp19,
            dp20,
            dp21,
            dp22,
            dp23,
            dp24,
            dp25,
            dp26,
            dp27,
            dp28,
            dp29,
            dp30,
            dp31,
            dp32,
            dp33,
            dp34,
            dp35,
            dp36,
            dp37,
            dp38,
            dp39,
            dp40,
            dp41,
            dp42,
            dp43,
            dp44,
            dp45,
            dp46,
            dp47,
            dp48,
            dp49, // check
            dp50,
            dp51,
            dp52,
            dp53,
            dp54,
            dp55,
            dp56,
            dp57,
            dp58,
            dp59,
            dp60,
            dp61,
            dp62,
            dp63,
            dp64,
            dp65,
            dp66,
            dp67,
            dp68,
            dp69,
            dp70,
            dp71,
            dp72
    );
}
