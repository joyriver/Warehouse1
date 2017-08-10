//package com.kbers.warehouse;
//
//import com.amazonaws.services.simpleemail.model.Destination;
//import com.amzass.aop.Repeat;
//import com.amzass.database.DBManager;
//import com.amzass.enums.common.DateFormat;
//import com.amzass.model.submit.OrderEnums;
//import com.amzass.service.common.ApplicationContext;
//import com.amzass.service.common.EmailSender;
//import com.amzass.utils.PageLoadHelper;
//import com.amzass.utils.common.HttpUtils;
//import com.amzass.utils.common.PageUtils;
//import com.amzass.utils.common.Tools;
//import com.google.inject.Inject;
//import com.mailman.utils.ServiceConstants;
//import org.apache.commons.lang3.StringUtils;
//import org.nutz.dao.Cnd;
//import org.nutz.dao.Condition;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
///**
// * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/11/28 16:17
// */
////TODO 流程重构
//class JpComparison {
//    private static final Logger LOGGER = LoggerFactory.getLogger(JpComparison.class);
//    @Inject
//    WarehouseDate  TimeJP;
//    @Inject
//    DBManager dbManager;
//
//    void scan() {
//        String orderId;
//        String sheetName;
//        String ACC;
//        Condition cnd = Cnd.orderBy();
//        List<Jp.JPRecord> JP_recordList;
//        JP_recordList = dbManager.query(Jp.JPRecord.class, cnd);
//        System.out.println("NJ_recordList.size():" + JP_recordList.size());
//
//        if (JP_recordList.size() >= 1) {
//            for (int size = 0; size < JP_recordList.size(); size++) {
//                orderId = JP_recordList.get(size).orderId;
//                sheetName = JP_recordList.get(size).sheetName;
//                ACC = JP_recordList.get(size).ACC;
//                LOGGER.info("orderId:{} , sheetName:{} , ACC:{}" ,orderId,sheetName,ACC);
//                System.out.println("orderId:" + orderId + " , sheetName:" + sheetName + " , ACC:" + ACC);
//
//                if (sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX)) {
//                    String frontSheetName = sheetName.split("/")[0];
//                    String backSheetName = sheetName.split("/")[1];
//                    System.out.println("frontSheetName:" + frontSheetName);
//                    System.out.println("backSheetName:" + backSheetName);
//
//                    if (TimeJP.yesterday() >= TimeJP.dateRange) {
//                        if (Integer.parseInt(frontSheetName) == TimeJP.month() && Integer.parseInt
//                                (backSheetName) <= TimeJP.yesterday() && Integer.parseInt
//                                (backSheetName)  >= TimeJP.farDate())
//                            search(JP_recordList, size, TimeJP.today());
//                    } else if(TimeJP.yesterday() < TimeJP.dateRange){
//                        if ((Integer.parseInt(frontSheetName) == TimeJP.month() && Integer.parseInt
//                                (backSheetName) <= TimeJP.yesterday()) || (Integer.parseInt(frontSheetName) == TimeJP.lastMonth() && Integer.parseInt(backSheetName) >= TimeJP.farDate()))
//                            search(JP_recordList, size, TimeJP.today());
//                    }
//                }
//
//
//            }
//        }
//        System.out.println("All Jp Tasks have been done!");
//        LOGGER.info("All Jp Tasks have been done!");
//    }
//
//
//    public void search(List<Jp.JPRecord> JP_recordList, int sequenceNumber, String date1) {
//
//        String price;
//        String sheetName = JP_recordList.get(sequenceNumber).sheetName;
//        String ACC = JP_recordList.get(sequenceNumber).ACC;
//        String status = JP_recordList.get(sequenceNumber).status;
//        String orderId = JP_recordList.get(sequenceNumber).orderId;
//        String recipientName = JP_recordList.get(sequenceNumber).recipientName;
//        String URL = JP_recordList.get(sequenceNumber).URL;
//        String isbn = JP_recordList.get(sequenceNumber).isbn;
//        String quantityPurchased = JP_recordList.get(sequenceNumber).quantityPurchased;
//        String remark = JP_recordList.get(sequenceNumber).Remark;
//        String itemName = JP_recordList.get(sequenceNumber).itemName;
//        String shipAddress1 = JP_recordList.get(sequenceNumber).shipAddress1;
//        String shipAddress2 = JP_recordList.get(sequenceNumber).shipAddress2;
//
//        String shipCity = JP_recordList.get(sequenceNumber).shipCity;
//        String shipState = JP_recordList.get(sequenceNumber).shipState;
//        String shipZip = JP_recordList.get(sequenceNumber).shipZip;
//        String shipCountry = JP_recordList.get(sequenceNumber).shipCountry;
//        String shipPhoneNumber = JP_recordList.get(sequenceNumber).shipPhoneNumber;
//        String purchaseDate = JP_recordList.get(sequenceNumber).purchaseDate;
//        String skuAddress = JP_recordList.get(sequenceNumber).skuAddress;
//        String sku = JP_recordList.get(sequenceNumber).sku;
//        price = JP_recordList.get(sequenceNumber).price;
//        String shippingFee = JP_recordList.get(sequenceNumber).shippingFee;
//
//        String isbnAddress = JP_recordList.get(sequenceNumber).isbnAddress;
//        String SELLER = JP_recordList.get(sequenceNumber).SELLER;
//        String sellerID = JP_recordList.get(sequenceNumber).sellerID;
//        String sellerPrice = JP_recordList.get(sequenceNumber).sellerPrice;
//        String condition = JP_recordList.get(sequenceNumber).condition;
//        String character = JP_recordList.get(sequenceNumber).character;
//        String reference = JP_recordList.get(sequenceNumber).reference;
//        String CODE = JP_recordList.get(sequenceNumber).CODE;
//        String LR = JP_recordList.get(sequenceNumber).LR;
//        String COST = JP_recordList.get(sequenceNumber).COST;
//
//        String orderNumber = JP_recordList.get(sequenceNumber).OrderNumber;
//        String ACCOUNT = JP_recordList.get(sequenceNumber).ACCOUNT;
//        String lastCode = JP_recordList.get(sequenceNumber).LastCode;
//        Double value = JP_recordList.get(sequenceNumber).value;
//        String email = JP_recordList.get(sequenceNumber).email;
//
//
//        if (!URL.contentEquals("")) {
//            Condition cnd = Cnd.where("orderId", "like", orderId).and("sheetName", "like", sheetName).and("URL",
//                    "like", URL).and("status", "like", status);
//            List<Jp.JPALLRecord> JP_All_recordList;
//            JP_All_recordList = dbManager.query(Jp.JPALLRecord.class, cnd);
//
//            if (JP_All_recordList.size() >= 1 && status.contentEquals("1")) {
//                LOGGER.info("orderId:{} has existed in general database!" ,orderId);
//                outputSheet(date1, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
//                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
//                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
//                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
//                        value, email);
//            } else if (JP_All_recordList.size() == 0 && status.contentEquals("1")) {
//                LOGGER.info("orderId:{} is new!" ,orderId);
//                outputSheet(date1, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
//                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
//                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
//                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
//                        value, email);
//                outputGeneral(status, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
//                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
//                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
//                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
//                        value, email);
//            }
//
//        } else if (URL.contentEquals(""))
//            LOGGER.info("orderId:{} ,of URL is null!" ,orderId);
//
//    }
//
//
//    public void outputGeneral(String status, String orderId, String sheetName, String URL, String price, String ACC,
//                              String recipientName, String isbn, String quantityPurchased, String remark, String
//                                      itemName, String shipAddress1, String shipAddress2, String shipCity, String
//                                      shipState, String shipZip, String shipCountry, String shipPhoneNumber, String
//                                      purchaseDate, String skuAddress, String sku, String shippingFee, String
//                                      isbnAddress, String SELLER, String sellerID, String sellerPrice, String
//                                      condition, String character, String reference, String CODE, String LR, String
//                                      COST, String orderNumber, String ACCOUNT, String lastCode, Double value, String email) {
//
//        if (sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
//            dbManager.save(new Jp.JPALLRecord(Tools.generateUUID(), ACC, sheetName, status, orderId,
//                    recipientName, URL, isbn, quantityPurchased, remark, itemName, shipAddress1, shipAddress2,
//                    shipCity, shipState, shipZip, shipCountry, shipPhoneNumber, purchaseDate, skuAddress, sku,
//                    price, shippingFee, isbnAddress, SELLER, sellerID, sellerPrice, condition, character, reference,
//                    CODE, LR, COST, orderNumber, ACCOUNT, lastCode, value, email), Jp.JPALLRecord.class);
//    }
//
//    private Map<String, Long> notifyTimestamps = new HashMap<>();
//
//    @Repeat(times = 8, sleepTime = 10)
//    public boolean outputSheet(String date1, String orderId, String sheetName, String URL, String price, String ACC,
//                               String recipientName, String isbn, String quantityPurchased, String remark, String
//                                       itemName, String shipAddress1, String shipAddress2, String shipCity,
//                               String shipState, String shipZip, String shipCountry, String shipPhoneNumber, String
//                                       purchaseDate, String skuAddress, String sku, String shippingFee, String
//                                       isbnAddress, String SELLER, String sellerID, String sellerPrice, String
//                                       condition, String character, String reference, String CODE, String LR,
//                               String COST, String orderNumber, String ACCOUNT, String lastCode, Double value, String email) {
//        shipAddress1= shipAddress1.replace("|",",");
//        shipAddress2= shipAddress2.replace("|",",");
//        itemName = itemName.replace("|",",");
//        String orderSummary = sheetName + "|" + date1 + "|" + ACC + "|" + orderId + "|" + "" + "|" + recipientName +
//                "|" + URL + "|" + isbn + "|" + "" + "|" + "" + "|" + quantityPurchased + "|" + remark + "|" +
//                itemName + "|" + shipAddress1
//                + "|" + shipAddress2 + "|" + shipCity + "|" + shipState + "|" + shipZip + "|" + shipCountry + "|" +
//                shipPhoneNumber + "|" + purchaseDate + "|" + skuAddress + "|" + sku + "|" + price + "|" + shippingFee
//                + "|" + isbnAddress + "|" + SELLER + "|" + sellerID + "|" + sellerPrice + "|" + condition + "|" +
//                character + "|" + reference + "|" + CODE + "|" + LR + "|" + COST + "|" + orderNumber + "|" + ACCOUNT
//                + "|" + lastCode + "|" + value + "|" + "" + "|" + email;
//
//
//        long start = System.currentTimeMillis();
//
//        String url1 = String.format(Constants.ascript2Parameter, "WarehouseJPMonitor", PageUtils.encodeParamValue(orderSummary));
//        String result;
//
//        result = PageUtils.processResult(HttpUtils.getTextThriceIfFail(url1));
//        if (OrderEnums.ReturnCode.Success.name().equals(result)) {
////                notifyTimestamps.remove("WarehouseMonitor");
//            LOGGER.info("成功将搜索重复指标数据{}返回{}, 耗时{}", orderId, "WarehouseJPMonitor", Tools.formatCostTime(start));
//            return true;
//        }
//
//        LOGGER.warn("将搜索重复指标数据{}返回{}失败: {}. 尝试重复操作.", orderId, "WarehouseJPMonitor", result);
//
//        if (Tools.contains(result, "above the limit of 2000000 cells")) {
//            // 不重复发送
//            Long timestamp = notifyTimestamps.get("WarehouseJPMonitor");
//            System.out.print(System.currentTimeMillis() + "\n");
//            System.out.print("timestamp:" + timestamp + "\n");
//            System.out.print("toMillis:" + TimeUnit.HOURS.toMillis(3) + "\n");
//            if (timestamp != null && (System.currentTimeMillis() - timestamp < TimeUnit.HOURS.toMillis(3))) {
//                LOGGER.info("{}对应表格清理通知邮件已在{}发送过了, 短期内无需重复发送", "WarehouseJPMonitor", DateFormat.DATE_TIME.format
//                        (timestamp));
//                return true;
//            }
//
//            // 数据总量超过上限, 需及时清理对应表格, 发送通知邮件  ServiceConstants.MAIL_MAN_RND_EMAIL
//            try {
//                String subject = String.format("Spreadsheet of %s needs cleanse and backup ASAP!!",
//                        "WarehouseJPMonitor");
//                String content = subject + StringUtils.LF + StringUtils.defaultString(result);
//                ApplicationContext.getBean(EmailSender.class).sendGmail(subject, content, EmailSender
//                                .EmailContentType.PlainText,
//                        ServiceConstants.MAIL_MAN, "Cleanse Notifier", new Destination().withToAddresses
//                                ("joyriver7@gmail.com, jackwen777@gmail.com"));
//
//                notifyTimestamps.put("WarehouseJPMonitor", System.currentTimeMillis());
//            } catch (Exception e) {
//                // -> Ignore
//            }
//            return true;
//        }
//        PageLoadHelper.WaitTime.Shorter.execute();
//        return false;
//    }
//
//    void clearJP() {
//        dbManager.clearAll(Jp.JPRecord.class);
//        System.out.print("Clear Jp successfully!");
//
//    }
//
//
//}
