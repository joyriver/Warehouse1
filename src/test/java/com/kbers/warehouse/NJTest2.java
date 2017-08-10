package com.kbers.warehouse;

import com.mailman.model.common.Sheets;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/12/14 18:13
 */
public class NJTest2 {

    @Test
    public void filter() {
        Sheets sheets = new Sheets("ID", "Name", Arrays.asList("S1", "SE", "12/14", "12/13", "12/10", "12/09",
                "12/07", "11/29"));
        assertEquals(new Executor().filter(sheets, false).toString(), Arrays.asList("12/13", "12/10", "12/09",
                "12/07").toString());

        sheets = new Sheets("ID", "Name", Arrays.asList("S1", "SE", "12/14", "12/13 ", "12/10", "12/9", "12/07",
                "11/29"));
        assertEquals(new Executor().filter(sheets, false).toString(), Arrays.asList("12/13 ", "12/10", "12/9",
                "12/07").toString());
    }

}