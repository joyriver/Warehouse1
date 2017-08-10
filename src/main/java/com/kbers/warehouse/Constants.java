package com.kbers.warehouse;

import java.util.Calendar;
import java.util.Date;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/12/20 18:21
 */
abstract class Constants {

    static final String ORDER_UPDATE_SHEET_NAME_REGEX = "^[0-9]{1,2}/[0-9]{1,2}$";
    static final String COST_DOUBLE_REGEX ="^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$";
    static final String INTEGER_REGX = "^\\d+$";
    final static String ascript1Parameter = "https://script.google" +
            ".com/macros/s/AKfycbxVXEjIRPpeLxi2LvGfdJciN2yuVvIvu9wFMbAm1_62H26mnX4/exec?m=%s";
    final static String ascript2Parameter = "https://script.google" +
            ".com/macros/s/AKfycbxVXEjIRPpeLxi2LvGfdJciN2yuVvIvu9wFMbAm1_62H26mnX4/exec?m=%s&h=%s";

    final static int tempSingleRange = 50;
    final static int allSingleRange  = 50;


    public static int getWeekDay(){
        Calendar c1 = Calendar.getInstance();
        Date myDate = new Date();
        c1.setTime(myDate);
        int weekDay = c1.get(Calendar.DAY_OF_WEEK) -1;
        return weekDay;
    }
}
