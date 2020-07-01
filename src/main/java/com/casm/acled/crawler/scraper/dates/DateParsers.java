package com.casm.acled.crawler.scraper.dates;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DateParsers {

    protected static final Logger logger = LoggerFactory.getLogger(DateParsers.class);

    public static final DateParser dp1 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy/"
    ));

    public static final DateParser dp2 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, HH:mm a/"
    ));

    public static final DateParser dp3 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy/"
    ));

    public static final DateParser dp4 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy/"
    ));

    public static final DateParser dp5 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy/"
    ));

    public static final DateParser dp6 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/ddF-MMM-yyyy//ORD"
    ));

    public static final DateParser dp7 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd MMM yyyy HH:mm a z//BST/RE.*Updated: (.*)" //Published: 26 Nov 2019 01:43 AM BdST Updated: 26 Nov 2019 01:44 AM BdST
    ));

    public static final DateParser dp8 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d MMM yyyy hh:mm/"  //Sunday 27 October 2019 23:56
    ));

    public static final DateParser dp9 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy//ORD"  //Monday, 3 February 2020
    ));

    public static final DateParser dp10 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d yyyy//ORD"  //Monday December 2 2019
    ));

    public static final DateParser dp11 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d yyyy/" // Noviembre 29 2019
    ));

    public static final DateParser dp12 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, hh:mm a//RE.*Update Date - (.*)"  //Update Date - November 03, 2019, 09:08 PM
    ));

    public static final DateParser dp13 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm Z|"
    ));

    public static final DateParser dp14 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy hh:mm|"
    ));

    // too ambiguous?
    public static final DateParser dp15 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy|"
    ));

    public static final DateParser dp16 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy/" //22 septembre 2019
    ));

    public static final DateParser dp17 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE MMM d, yyyy//ORD" // Monday Sep 30, 2019
    ));

    public static final DateParser dp18 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, HH:mm/" // 15 Ноября 2019, 16:15
    ));

    public static final DateParser dp19 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy h:mm:ss a//RE.*: (.*)" // Published: September 17, 2019 8:32:39 PM
    ));

    public static final DateParser dp20 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy/"
    ));

    public static final DateParser dp21 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy 'г.', HH:MM/" // 17 ноября 2019 г., 23:01
    ));

    public static final DateParser dp22 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy/" // octobre 6, 2019
    ));

    public static final DateParser dp23 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy HH:MM//ORD" // Tuesday, 04 February 2020 14:15'
    ));

    public static final DateParser dp24 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm dd.MM.yyyy/" // '16:49 28.01.2020'
    ));

    public static final DateParser dp25 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//RE.*updated(?: on)?:? (.+)" // Last updated Sep 21, 2019'
    ));

    public static final DateParser dp26 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a MMM d, yyyy//ORD/RE.+ublished(?: at)|(?: on):? (.+)" // 'Published at 06:11 pm November 4th, 2019'
    ));

    public static final DateParser dp27 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy hh:mm//ORD" // Nov 17, 2019 22:28
    ));

    public static final DateParser dp28 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy hh:mm//ORD/RE.+pdated: (.+)" // Updated: Oct 11, 2019 21:51 IST
    ));

    public static final DateParser dp29 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//ORD/RE(.+) [Pp]osted by.*" //sept 11, 2019 Posted by Redaction Economie 0
    ));

    public static final DateParser dp30 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy '|' HH:mm a//ORD" // October 27, 2019 | 11:04 AM
    ));

    public static final DateParser dp31 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy hh:mm z//ORD/" //October 11, 2019 10:15PM EDT
    ));

    public static final DateParser dp32 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy HH:mm/" //25.11.2019, 15:20
    ));

    public static final DateParser dp33 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy/" //29 septiembre, 2019'
    ));

    public static final DateParser dp34 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|EEE, dd/MM/yyyy, HH:mm|" //Monday, 11/18/2019, 11:12
    ));

    public static final DateParser dp35 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/yyyy-MM-dd HH:mm:ss/" //2019-09-18 18:03:20
    ));

    public static final DateParser dp36 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, h:mma//ORD" //29 October 2019, 3:24pm
    ));

    public static final DateParser dp37 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, MMM d, yyyy//ORD" //Monday, December 02, 2019
    ));

    public static final DateParser dp38 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy HH:mm//ORD" //29 September 2019 16:32
    ));

    public static final DateParser dp39 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d - yyyy//ORD" //October 21 - 2019
    ));

    public static final DateParser dp40 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy HH:mm zzz//ORD/RE.+dated: (.+)" //
    ));

    public static final DateParser dp41 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, HH:mm//ORD" //October 1, 2019, 17:53
    ));

    public static final DateParser dp42 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd/MM/yyyy H'h'mm||ORD" //28/09/2019 15h49
    ));

    public static final DateParser dp43 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, HH:mm z//ORD" //7 February 2020, 14:33 UTC
    ));

    public static final DateParser dp44 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM yyyy, HH:mm z//ORD"
    ));

    public static final DateParser dp45 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//ORD/RE^[-] (.+)" // "- January 30, 2020"
    ));

    public static final DateParser dp46 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy h:mm a//RE.+date: (.+[AP]M).*" //Thursday, 30 January 2020 5:17 PM  [ Last Update: Thursday, 30 January 2020 5:24 PM ]
    ));

    public static final DateParser dp47 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d MMM yyyy 'at' HH:mm//ORD"  //Tue 04 Feb 2020 at 19:42
    ));

    public static final DateParser dp48 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, MMM d,yyyy//RE.+dated: (.+)"  //Updated: 01:20, Feb 06,2020'
    ));

    // TODO: why no parse? Regex extracting fine.
    public static final DateParser dp49 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy hh:mm:ss a z||RE.+ate: (.+)"  //'JUBA, May 7 (Agencies) | Publish Date: 5/7/2019 12:04:09 PM IST'
    ));

    public static final DateParser dp50 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy 'a las' HH:mm||REActualizado el:? (.+)"  //'Actualizado el 03/02/2020 a las 11:03'
    ));

    public static final DateParser dp51 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/h:mm a '|' MMM d, yyyy//ORD"  //'6:52 PM | August 19, 2018'
    ));

    public static final DateParser dp52 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy '-' HH:mm//ORD"  //29 Jan 2020 - 15:54
    ));

    public static final DateParser dp53 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy '|' h:mm a//ORD"  //'04 Feb, 2020 | 9:57 am'
    ));

    public static final DateParser dp54 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//RE.*dated (.+)\\)"  // 'by Dr XXX XXX XXXXX , (Last Updated November 9, 2019)'
    ));

    public static final DateParser dp55 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' yyyy//"  // '17 de Junho de 2019'
    ));

    public static final DateParser dp56 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/E, d.M.yyyy. HH:mm//"  // Čet, 13.02.2020. 16:15
    ));

    public static final DateParser dp57 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/yyyy, d MMM, EEE, 'Bakı vaxtı' HH:mm//"  // '2020, 25 Fevral, çərşənbə axşamı, Bakı vaxtı 00:34' (Azerbaijani)
    ));

    public static final DateParser dp58 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|EEE, d / MMM / yyyy|"  // Miércoles, 18 / Oct / 2017
    ));

    public static final DateParser dp59 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§d/M/yyyy HH:mm§§RE^[|•]\\s+(.+)" // '|   24/02/2020 00:00'
    ));

    public static final DateParser dp60 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' yyyy HH:mm//" // '5 de February de 2014 21:46'
    ));

    public static final DateParser dp61 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' yyyy HH:mm//" // '5 de febrero de 2014 21:46'
    ));

    public static final DateParser dp62 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§d/M/yyyy '|' H:mm§§STRIP[0-9]:[0-9][0-9]\\s+Atualizado\\s+" // '21/02/2020 | 8:21'
    ));

    public static final DateParser dp63 = CompositeDateParser.of(ImmutableList.of(
            "ISO:§HH:mm dd.MM.yyyy§§RE.+обновлено:\\s+(.+)[)]" // '09:58 13.07.2013 (обновлено: 10:02 13.07.2013)'
    ));

    public static final DateParser dp64 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d HH:mm yyyy//ORD" // 'November 22 17:36 2018'
    ));

    public static final DateParser dp65 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy hh:mm a zzz//ORD" // '20 Feb 2020 11:42 AM GMT'
    ));

    public static final DateParser dp66 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/'Published at :' MMM dd, yyyy//ORD" // 'Published at : February 12, 2020
    ));

    public static final DateParser dp67 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy '|' H:mm 'h'//ORD" // '26 Dic 2019 | 4:45 h'
    ));

    public static final DateParser dp68 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, d MMM yyyy//ORD" //08:32, 16 января 2020
    ));

    public static final DateParser dp69 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy//ORD" //'6 декабря 2019'
    ));

    public static final DateParser dp70 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//ORD" //'marzo 16, 2020'
    ));

    public static final DateParser dp71 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' yyyy '-' HH:mm//ORD/RE^[|]\\s+(.+)" //'| 22 de Febrero de 2020 - 00:00'
    ));

    public static final DateParser dp72 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy HH:mm//ORD/RE^Published:\\s+(.+\\d)$" //'Published:  February 20, 2020 11:41'
    ));

//    public static final DateParser dp73 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:|d/MM '-' HH:mm|" //'12/02 - 14:53'
//    ));

    public static final DateParser dp74 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d.M.yyyy HH:mm:ss||RE[^0-9]*(.+)" // 'Torreón, Coahuila / 03.04.2020 12:13:52'
    ));

    public static final DateParser dp75 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d MMM yyyy|" // '2 Temmuz 2019'
    ));

    public static final DateParser dp76 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'de' yyyy/" //23 de febrero de 2020
    ));

    public static final DateParser dp77 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE d 'de' MMM 'de' yyyy/" //Martes 17 de febrero de 2009
    ));

    public static final DateParser dp78 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy HH:mm||RE(.+)\\s[/].+" //14/06/2019 15:50 / Ciudad de México
    ));

    public static final DateParser dp79 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy//RE.*Atualizado a (.+)" //15 Agosto 2016 - Atualizado a 3 Novembro 2016
    ));

    public static final DateParser dp80 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy h:mm a//ORD/RE^[\\w\\h]+[\\W\\s]+(.+)/STRIP[–]" //Published on – December 7, 2019 – 8:57 pm
    ));

    public static final DateParser dp81 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd.MM.yyyy / HH:mm||ORD" //26.04.2019 / 10:20
    ));

    public static final DateParser dp82 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d MMM yyyy hh:mm a||RE^Actualiza.+,\\s(.+)|STRIP/" //'Actualización: Mié, 19 / Jun / 2019 12:09 pm'
    ));

    public static final DateParser dp83 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy hh:mm:ss a//RE.*MODIFIED:\\s(.+)" //'10 December, 2019 00:00 00 AM / LAST MODIFIED: 18 February, 2020 01:32:50 AM'
    ));

    public static final DateParser dp84 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM YYY HH:mm//STRIPà" //07 Fév 2019 à 17:45
    ));

    public static final DateParser dp85 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM YYY HH:mm zzz//STRIP–" //Rabu, 05 Februari 2020 – 03:59 WIB
    ));

    public static final DateParser dp86 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy//RE(.+) 00:00 00 AM" //18 February, 2020 00:00 00 AM
    ));

    public static final DateParser dp87 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy/" //'2 Februari 2020'
    ));

    // TODO: why fails finding IST with zzz?
    public static final DateParser dp88 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM yyyy, hh:mm a//RE.+Updated:\\s(.+) IST" //8 min read . Updated: 28 Jul 2019, 07:00 PM IST
    ));

    public static final DateParser dp89 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a MMM d, yyyy//ORD/RE^Published at (.+)" //Published at 11:14 pm March 7th, 2019
    ));

    public static final DateParser dp90 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a MMM d, yyyy//ORD/RE.+updated at (.+)" //Last updated at 08:37 pm March 2nd, 2017
    ));

    // TODO: why fails finding ist timezone with zzz?
    public static final DateParser dp91 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d yyyy, hh:mma//ORD/RE.+updated: (.+) ist" //Last updated at 08:37 pm March 2nd, 2017
    ));

    public static final DateParser dp92 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/hh:mm a, MMM d, yyyy//RE.+MODIFIED: (.+)" //'12:00 AM, February 07, 2020 / LAST MODIFIED: 01:15 AM, February 07, 2020'
    ));

    public static final DateParser dp93 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//ORD/RE.+\\d{4}(\\w.+)" //'January 30, 2020January 30, 2020'
    ));

    public static final DateParser dp94 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.YY H'h'mm/" //05.02.20 7h15
    ));

    public static final DateParser dp95 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, d.MM.yyyy/" //15:00, 31.01.2020
    ));

    public static final DateParser dp96 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy '-' h:mm a//" //Feb 4, 2020 - 2:41 PM
    ));

    // TODO: why fails finding IST timezone with zzz?
    public static final DateParser dp97 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy HH:mm//RE.+dated: (.+) IST" //Updated: Feb 15, 2020 09:11 IST
    ));

    public static final DateParser dp98 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy//ORD/RE.+ate\\s?: (.+)$" //Publishing Date : 14 January, 2020
    ));

    public static final DateParser dp99 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy//ORD/RE(.+)\\s[|]\\D+$" //Thursday, 16 January 2020 | XXX XXX XXX ...'
    ));

    public static final DateParser dp100 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy, hh:mm a//ORD/RE.+ed\\s?: (.+) IST$" //Published : Feb 18, 2020, 12:01 am IST
    ));

    public static final DateParser dp101 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//ORD/RE.*On (.+)$" //On Feb 13, 2020
    ));

    public static final DateParser dp102 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|M/d/yyyy hh:mm:ss a||ORD|RE.*Date: (.+) IST$" //Publish Date: 8/24/2019 12:16:40 PM IST
    ));

    public static final DateParser dp103 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d.M.yyyy. HH:mm/" //Čet, 19.12.2019. 09:44
    ));

    public static final DateParser dp104 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM, yyyy//ORD/RE.+\\d{4}(\\d.+)" //'28 agosto, 20186 octubre, 2018'
    ));

    public static final DateParser dp105 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy HH:mm||RE^Publicada em (.+)$|STRIPàs" //'Publicada em 06/01/2020 às 09:17'
    ));

    public static final DateParser dp106 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy '-' HH:mm||RE.+Atualizado em (.+)$" //'27/11/2019 - 17:48 / Atualizado em 27/11/2019 - 18:23'
    ));

    public static final DateParser dp107 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|HH:mm d/M/yyyy|" //12:10 26/12/2019
    ));

    public static final DateParser dp108 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy HH:mm'h'||STRIPàs" //03/02/2020 às 11:07h
    ));

    public static final DateParser dp109 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy//RE^(.+) [|] [\\w\\s]+ [|] [\\w\\s]+$" //Sunday, 16 February 2020 | XXX | XXX XXX
    ));

    public static final DateParser dp110 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd 'de' MMM 'de' yyyy 'a las' HH:mm'h'//ORD" //'17 de marzo de 2017 a las 11:03h'
    ));

    public static final DateParser dp111 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy HH:mm:ss//ORD/RE.+dated: (.+)$" //'Published: November 18, 2017 13:24:22 | Updated: November 18, 2017 16:58:34'
    ));

    public static final DateParser dp112 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, d MMM yyyy hh:mm a//ORD" //Tue, 19 Nov 2019 04:13 PM
    ));

    public static final DateParser dp113 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy HH:mm:ss//RE.*Published: (.+)$" //'XXX XXX | Published: September 02, 2019 22:09:48'
    ));

    public static final DateParser dp114 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, MMM d,yyyy//RE.*Published: (.+)$" //'XXX XXX | Published: 00:55, Feb 05,2020'
    ));

    public static final DateParser dp115 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMM d, yyyy//RE^Published on: (.+)$" //Published on: January 29, 2020
    ));

    public static final DateParser dp116 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEE, MMM d, YYY, hh:mm a//RE^: (.+) IST$" //: Thursday, February 13, 2020, 6:45 AM IST
    ));

    public static final DateParser dp117 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d/M/yyyy HH:mm||RE^Issued on: (.+)$|STRIP-" //Issued on: 16/02/2018 - 10:30
    ));

    /* Conflicting names from MERGE conflict, TODO: add back in */
//    public static final DateParser dp118 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d MMM yyyy HH:mm//STRIPгода," //9 ноября 2016 года, 20:11
//    ));
//
//    public static final DateParser dp119 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/EEE d MMM yyyy HH:mm//STRIPà" //Mercredi 05 Fevrier 2020 à 09:21
//    ));
//
//    public static final DateParser dp120 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d.M.yyyy//RE^Publicado en:? (.+)$" //Publicado en 28.06.2013
//    ));
//
//    public static final DateParser dp121 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d MMM, yyyy//RE^On:? (.+)$" //'On 24 September, 2018'
//    ));
//
//    public static final DateParser dp122 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/EEE d 'de' MMM 'de' yyyy, 'a las' HH:mm/es" //Miércoles 05 de febrero de 2020, a las 17:34
//    ));
//
//    public static final DateParser dp123 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:|d/M/yyyy HH:mm||STRIP-" //'13/06/2019 - 04:29'
//    ));
//
//    // TODO: timezones again..?
//    public static final DateParser dp124 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/MMM d, yyyy HH:mm z//STRIP-" //January 30, 2020 - 13:32 AMT
//    ));
//
//    public static final DateParser dp125 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/EEE d MMM yyyy hh:mm a//STRIP[|]" //'Mié 20 Mar 2019 | 01:48 pm'
//    ));
//
//    public static final DateParser dp126 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d 'de' MMM 'de' YYYY HH:mm//STRIP[•]" //'1 de mayo de 2019   • 00:54'
//    ));
//
//    public static final DateParser dp127 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d MMM yyyy/" //'29  İyl 2019' //uz lang
//            //https://manpages.debian.org/unstable/libdatetime-locale-perl/DateTime::Locale::uz_Latn_UZ.3pm.en.html
//    ));
//
//    public static final DateParser dp128 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d 'de' MMM yyyy hh:mm a//AMPM/STRIP[,.]" //30 de junio 2019 , 09:58 a.m. //es lang
//    ));
//
//    public static final DateParser dp129 = CompositeDateParser.of(ImmutableList.of(
//            "ISO:/d MMM yyyy//STRIP[,]" // ar lang
//            // '6 أبريل, 2016'
//    ));

    //January 30, 2020 - 13:32 AMT
    //Tue, 19 Nov 2019 04:13 PM
    //Feb 4, 2020 - 2:41 PM
    //Published at 11:14 pm March 7th, 2019
    //07 Fév 2019 à 17:45
    //Last updated at 08:37 pm March 2nd, 2017
    //Thursday, 16 January 2020 | markandey katju Janhvi Prakash'
    //17 de marzo de 2017 a las 11:03h
    //понедељак, 02.03.2020. у 21:33
    //28. marta 2020. 15:59
    public static final DateParser dp118 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy HH:mm/en/RE.*(\\d{2}\\.\\d{2}\\.\\d{4})\\.? [уu] (\\d{2}:\\d{2}).*"
    ));

    public static final DateParser dp119 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM, yyyy/" //14 de octubre, 2019
    ));

    public static final DateParser dp120 = CompositeDateParser.of(ImmutableList.of(
            "ISO:-yyyy MM  dd'T'HH:mm" // 2020-05-12T07:03
    ));

    public static final DateParser dp121 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d MMM',' yyyy',' HH:mm" // 30 Mar, 2020, 14:30
    ));

    public static final DateParser dp122 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE, MMM d, yyyy HH:mm a//AMPM" // jueves, diciembre 1, 2016 9:08 pm
    ));

    public static final DateParser dp123 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE d 'de' MMM 'de' yyyy, 'a las' HH:mm" // 'Miércoles 28 de agosto de 2019, a las 16:35'
    ));

    public static final DateParser dp124 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE d, MMM yyyy HH:mm" // 'Viernes 08, mayo 2020 12:39'
    ));

    public static final DateParser dp125 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'DE' MMM, yyyy//REPublicado: (\\d{2} DE [a-zA-Z]+, \\d{4})" // 'Publicado: 09 DE MAYO, 2020'
    ));

    public static final DateParser dp126 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE d 'de' MMM 'de' yyyy//RE.*([a-zA-Z]+ \\d{2} de [a-zA-z]+ de \\d{4}).*" // '  / lunes 11 de mayo de 2020'
    ));

    public static final DateParser dp127 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/yyyy MM d'T'HH:mm:ssxxx//" // 2013-11-28T23:07:36-05:00
    ));

    public static final DateParser dp128 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE, d MMM, yyyy HH:mm a//AMPM" // lunes, 11 mayo, 2020 07:26 AM
    ));

    public static final DateParser dp129 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/d 'de' MMM 'del' yyyy 'a las' HH:mm//RE.*Por:.*(\\d{2}.*\\d{4}.*\\d{2}:\\d{2})" // Por: Tito Reséndez Treviño El Día Lunes 11 de Mayo del 2020 a las 17:26
    ));

    public static final DateParser dp130 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE, d 'de' MMM 'de' yyyy/es/RE.*-\\s+(.*)\\s+-.*" // ENSENADA, BC - jueves, 1 de agosto de 2019 - AFN
    ));

    public static final DateParser dp131 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/EEEE, dd.MM.yyyy. 'u' HH:mm" // ENSENADA, BC - jueves, 1 de agosto de 2019 - AFN
    ));

    public static final DateParser dp132 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/'Objavljeno' d. MMM, yyyy" // ENSENADA, BC - jueves, 1 de agosto de 2019 - AFN
    ));

    public static final DateParser dp133 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd. MM. yyyy." // ENSENADA, BC - jueves, 1 de agosto de 2019 - AFN
    ));

    public static final DateParser dp134 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd. MMM yyyy, HH:mm" // ENSENADA, BC - jueves, 1 de agosto de 2019 - AFN
    ));

    public static final DateParser dp135 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.MM.yyyy HH:mm//RE.*\\|\\s(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}).*" // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp136 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/HH:mm, d. M. yyyy." // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp137 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd.M.yyyy - HH:mm" // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp138 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/MMMM dd, yyyy - HH:mm" // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp139 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|HH:mm / dd.MM.yyyy" // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp140 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/LLLL d, yyyy/" // "BETAVIDEO | 30.04.2020 22:05 > 30.04 22:05"
    ));

    public static final DateParser dp141 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd. MMM, yyyy.||RE(?:Objavljeno)? ?(.*)" // "Objavljeno 30. siječnja, 2020."
    ));

    public static final DateParser dp142 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|mm:ss E, dd MMM yyyy|sq|RE(.*) / E (\\w{3}).*, (.*)" // "07:41 / E Diele, 22 Maj 2020"
    ));

    public static final DateParser dp143 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|dd.M.yyyy. 'u' HH:mm" // "07:41 / E Diele, 22 Maj 2020"
    ));

    public static final DateParser dp144 = CompositeDateParser.of(ImmutableList.of(
            "ISO:|d. LLLL yyyy." // "2. April 2020."
    ));

    public static final DateParser dp145 = CompositeDateParser.of(ImmutableList.of(
            "ISO:/dd. MMM yyyy. HH:mm/sr/RE.*\\| (.*?) [>|].*" //  "Б. Борисављевић | 24. мај 2020. 09:43 | Коментара: 0"
    ));

    public static final DateParser nl1 = CompositeDateParser.of(ImmutableList.of(
            "NL:/prije/"
    ));

    public static final DateParser nl2 = CompositeDateParser.of(ImmutableList.of(
            "NL:/(\\d+d|\\d+h|\\d+min)/"
    ));

    public static final DateParser generic = CompositeDateParser.of(ImmutableList.of(
            "NL:/.*/"
    ));

    public static void main(String... args) {
        String date = "4. marta 2015. 11:37";
        DateParser dp = CompositeDateParser.of(ImmutableList.of(
                "ISO:/dd. MMM yyyy. HH:mm/sr/RE.*\\| (.*?) [>|].*"
        ));

//        dp = dp140;
        dp = generic;

        System.out.println(dp.parse(date).toString());
    }

    public static final List<DateParser> ONE = ImmutableList.of(dp130);

    public static final List<DateParser> ALL = ImmutableList.of(
            generic,
            dp145,
            dp144,
            dp143,
            dp142,
            dp141,
            dp140,
            dp139,
            dp138,
            dp137,
            dp136,
            dp135,
            dp134,
            dp133,
            dp132,
            dp131,
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
            dp72,
//            dp73,
            dp74,
            dp75,
            dp76,
            dp77,
            dp78,
            dp79,
            dp80,
            dp81,
            dp82,
            dp83,
            dp84,
            dp85,
            dp86,
            dp87,
            dp88,
            dp89,
            dp90,
            dp91,
            dp92,
            dp93,
            dp94,
            dp95,
            dp96,
            dp97,
            dp98,
            dp99,
            dp100,
            dp101,
            dp102,
            dp103,
            dp104,
            dp105,
            dp106,
            dp107,
            dp108,
            dp109,
            dp110,
            dp111,
            dp112,
            dp113,
            dp114,
            dp115,
            dp116,
            dp117,
            dp118,
            dp119,
            dp120,
            dp121,
            dp122,
            dp123,
            dp124,
            dp125,
            dp126,
            dp127,
            dp128,
            dp129,
            dp130
            ,nl1
            ,nl2
    );
}
