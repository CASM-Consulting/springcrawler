package uk.ac.susx.tag.norconex;
import com.casm.acled.crawler.utils.Util;
//import com.casm.acled.dao.util.Util;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class TestDataParser {

    @Test
    public void TestDataParser() {
        String date = "15/10/2019 - 04:30 / Atualizado em 17/10/2019 - 08:20";
        LocalDate parsedD = Util.getDate(date);
        System.out.println("Original: " + date);
        System.out.println("Parsed: " + parsedD);
//        try {
//            Date dateS = new SimpleDateFormat("dd/MM/yyyy").parse(date);
//
//            System.out.println(dateS);
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
    }

}
