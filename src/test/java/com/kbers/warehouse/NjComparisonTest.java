package com.kbers.warehouse;

import com.amzass.service.common.ApplicationContext;
import org.testng.annotations.Test;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/11/28 16:27
 */
class NjComparisonTest {
    NjComparison nj = ApplicationContext.getBean(NjComparison.class);

    @Test
    public void TestscanNJ() {
//   nj.search("702-7174589-7160232");
    }

    @Test
    public void scan() {
        nj.scan();
    }


    @Test
    public void TestclearNJ() {
        nj.clearNJ();
    }
}