package com.kbers.warehouse;

import com.alibaba.fastjson.JSON;
import com.amzass.service.common.ApplicationContext;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.HttpUtils;
import com.amzass.utils.common.PageUtils;
import com.kber.aop.Repeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SpreadSheetDownloading {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpreadSheetDownloading.class);

    @Repeat(times = 8, sleepTime = 10)
     String[] downloadSidLinkSpreadId(boolean isJapan) {
        String method = isJapan ? "methodLoadAccountJP" : "methodLoadAccount";
        String url = String.format(Constants.ascript1Parameter, method);
        String sidLinkSpreadId[] = new String[0];
        try {
            String range = PageUtils.processResult(HttpUtils.getTextThriceIfFail(url));
            System.out.println(range);
            String[][] sidAndIndicator = JSON.parseObject(range, String[][].class);
            System.out.println(sidAndIndicator);
            System.out.println("length:"+sidAndIndicator.length);
            sidLinkSpreadId = new String[sidAndIndicator.length];
            for (int i = 0; i  < sidAndIndicator.length; i++) {
                sidLinkSpreadId[i] = sidAndIndicator[i][0] + "|" + sidAndIndicator[i][1] + "|" + sidAndIndicator[i][2] ;
                System.out.println(sidLinkSpreadId[i]);
            }
        } catch (BusinessException e) {
            LOGGER.error("AppScript URL {}:", url, e);
        }
        return sidLinkSpreadId;
    }

    public static void main(String[] args) {
        SpreadSheetDownloading SpreadLoad = ApplicationContext.getBean(SpreadSheetDownloading.class);
        SpreadLoad.downloadSidLinkSpreadId(true);
    }
}
