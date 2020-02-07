package uk.ac.susx.tag.norconex;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import com.mdimension.jchronic.Chronic;
import org.junit.Assert;
import org.junit.Test;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class TestPrettyTime {

    @Test
    public void testPrettyTime() {
        String date = "njksnrv 20th October egbt 2019 vdrvdev";
        String date2 = "20 10 17";
        System.out.println(new PrettyTimeParser().parse(date).get(0).toString());
        System.out.println(new PrettyTimeParser().parse(date2).get(0).toString());

        new PrettyTimeParser(TimeZone.getTimeZone("gb"));
        System.out.println(Chronic.parse(date));

//        Parser parser = new Parser();
//        List<DateGroup> groups = parser.parse(date2);
//        for(DateGroup group:groups) {
//            for(Date d : group.getDates()) {
//                System.out.println(d.toString());
//            }
//        }

    }

}
