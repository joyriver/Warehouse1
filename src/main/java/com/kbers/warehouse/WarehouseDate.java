package com.kbers.warehouse;

import com.amzass.service.common.ApplicationContext;

import java.util.Calendar;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/12/20 16:43
 */
//TODO 命名/单元测试
class WarehouseDate {

    int farDate() {
        Calendar c1 = Calendar.getInstance();
        int m1 = c1.get(Calendar.MONTH) + 1;
        int date = c1.get(Calendar.DATE);
        int d1 = date - 1;

        int nd = 0;

        if (m1 == 2)
            nd = 28;
        else if (m1 == 4 || m1 == 6 || m1 == 9 || m1 == 11)
            nd = 30;
        else if (m1 == 1 || m1 == 3 || m1 == 5 || m1 == 7 || m1 == 8 || m1 == 10 || m1 == 12)
            nd = 31;

        if (m1 == 1 && d1 < dateRange) {
            d1 = d1 + nd - dateRange + 1;
        } else if (m1 == 1 && d1 >= dateRange) {
            d1 = d1 - dateRange + 1;
        } else if (m1 > 1 && d1 < dateRange) {
            d1 = d1 + nd - dateRange + 1;
        } else if (m1 > 1 && d1 >= dateRange) {
            d1 = d1 - dateRange + 1;
        }
        return d1;
    }

    int yesterday() {
        Calendar c1 = Calendar.getInstance();
        return c1.get(Calendar.DATE) - 1;
    }

    int month() {
        Calendar c1 = Calendar.getInstance();
        return c1.get(Calendar.MONTH) + 1;
    }

    String today() {
        Calendar c1 = Calendar.getInstance();
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH);
        int date = c1.get(Calendar.DATE);
        return year + "-" + (month + 1) + "-" + date;
    }

    int dateRange = 60;

    int lastMonth(){
        int lastMonth;
        if(month() == 1)
            lastMonth =12;
        else
            lastMonth = month() - 1;

        return lastMonth;
    }

    public static void main(String[] args) {
        WarehouseDate nj = ApplicationContext.getBean(WarehouseDate.class);
        Calendar c1 = Calendar.getInstance();
        System.out.println(c1.get(Calendar.MONTH));
    }

}
