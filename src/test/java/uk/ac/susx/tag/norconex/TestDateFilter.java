package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.DateFilter;
import org.junit.Test;

public class TestDateFilter {

    @Test
    public void testCleanDate() {

        String date = "fl  10/06/2019 wjmm";
        DateFilter df = new DateFilter();
        System.out.println(df.parseDate(date));

    }


}
