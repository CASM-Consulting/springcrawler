package uk.ac.susx.tag.norconex;

import com.casm.acled.crawler.DateFilter;
import com.casm.acled.crawler.utils.Util;
import org.junit.Test;

public class TestDateFilter {

    @Test
    public void testCleanDate() {

        String date = "fl  10/06/2019 wjmm";
//        DateFilter df = new DateFilter();
        System.out.println(Util.getDate(date));

    }

    @Test
    public void testCleanDate2() {

        String date = "09/02/2019";
//        DateFilter df = new DateFilter();
        System.out.println(Util.getDate(date));

    }
}
