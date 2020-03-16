package uk.ac.susx.tag.norconex;
import com.casm.acled.crawler.utils.DateUtil;
import com.joestelmach.natty.Parser;
import org.junit.Test;

import java.time.LocalDate;

public class TestDataParser {


    // Should be trying to extract dates further from the text
    private static Parser nattyParser = new Parser();

    // problem 1. Reformat the date if it is in an american format


    @Test
    public void TestDataParser() {
        // Ciudad de México a 10 de febrero de 2017.-
        // 06.02.2020 Perşembe 16:25  - Son Güncelleme: 06.02.2020 Perşembe 16:48

        String date = " Our Correspondent | Published: March 01, 2018 01:17:42 | Updated: March 01, 2018 11:10:53";
        LocalDate parsedD = DateUtil.getDate(date);
        System.out.println("Original: " + date);
        System.out.println("Parsed: " + parsedD);

        System.out.println();
//        try {
//            Date dateS = new SimpleDateFormat("dd/MM/yyyy").parse(date);
//
//            System.out.println(dateS);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
    }

}
