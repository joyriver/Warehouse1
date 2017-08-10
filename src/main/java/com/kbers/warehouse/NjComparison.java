package com.kbers.warehouse;

import com.amazonaws.services.simpleemail.model.Destination;
import com.amzass.enums.common.ConfigEnums.AccountType;
import com.amzass.enums.common.DateFormat;
import com.amzass.model.common.Account;
import com.amzass.model.submit.OrderEnums;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.common.EmailSender;
import com.amzass.utils.common.HttpUtils;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.aop.Repeat;
import com.kber.commons.DBManager;
import com.kbers.warehouse.Nj.NJRecord;
import com.mailman.utils.ServiceConstants;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/11/28 16:17
 */
class NjComparison {
    private static final Logger LOGGER = LoggerFactory.getLogger(NjComparison.class);
    @Inject
    WarehouseDate  Time;

    @Inject
    DBManager dbManager;

    void scan() {
        Condition cnd = Cnd.orderBy();
        List<Nj.NJRecord> NJ_recordList = dbManager.query(Nj.NJRecord.class, cnd);
        System.out.println("NJ_recordList.size():" + NJ_recordList.size());


        if (NJ_recordList.size() >= 1) {
            for (int size = 0; size < NJ_recordList.size(); size++) {
                //TODO 性能优化
                NJRecord njRecord = NJ_recordList.get(size);
                String orderId = njRecord.orderId;
                String sheetName = njRecord.sheetName;
                String ACC = njRecord.ACC;
                LOGGER.info("orderId:{} , sheetName:{} , ACC:{}" ,orderId,sheetName,ACC);
//                System.out.println("orderId:" + orderId + " , sheetName:" + sheetName + " , ACC:" + ACC);

                if (sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX)) {
                    String[] array = sheetName.split("/");
                    String frontSheetName = array[0];
                    String backSheetName = array[1];
//                    System.out.println("frontSheetName:" + frontSheetName);
//                    System.out.println("backSheetName:" + backSheetName);

                    if (Time.yesterday() >= Time.dateRange) {
                        if (Integer.parseInt(frontSheetName) == Time.month() && Integer.parseInt
                                (backSheetName) <= Time.yesterday() && Integer.parseInt
                                (backSheetName)  >= Time.farDate())
                            search(NJ_recordList, size, Time.today());
                    } else if(Time.yesterday() < Time.dateRange){
                        if ((Integer.parseInt(frontSheetName) == Time.month() && Integer.parseInt
                                (backSheetName) <= Time.yesterday()) || (Integer.parseInt(frontSheetName) == Time.lastMonth() && Integer.parseInt(backSheetName) >= Time.farDate()))
                            search(NJ_recordList, size, Time.today());
                    }
                }
            }
        }
        //TODO LOGGER
        System.out.println("All Tasks have been done!");
        LOGGER.info("All Tasks have been done!");
    }


    void search(List<Nj.NJRecord> NJ_recordList, int sequenceNumber, String date1) {
        // TODO: Unused/Performance
        NJRecord njRecord = NJ_recordList.get(sequenceNumber);

        String sheetName = njRecord.sheetName;
        String ACC = njRecord.ACC;
        String status = njRecord.status;
        String orderId = njRecord.orderId;
        String recipientName = njRecord.recipientName;
        String URL = njRecord.URL;
        String isbn = njRecord.isbn;
        String quantityPurchased = njRecord.quantityPurchased;
        String remark = njRecord.Remark;
        String itemName = njRecord.itemName;
        String shipAddress1 = njRecord.shipAddress1;
        String shipAddress2 = njRecord.shipAddress2;

        String shipCity = njRecord.shipCity;
        String shipState = njRecord.shipState;
        String shipZip = njRecord.shipZip;
        String shipCountry = njRecord.shipCountry;
        String shipPhoneNumber = njRecord.shipPhoneNumber;
        String purchaseDate = njRecord.purchaseDate;
        String skuAddress = njRecord.skuAddress;
        String sku = njRecord.sku;
        String price = njRecord.price;
        String shippingFee = njRecord.shippingFee;

        String isbnAddress = njRecord.isbnAddress;
        String SELLER = njRecord.SELLER;
        String sellerID = njRecord.sellerID;
        String sellerPrice = njRecord.sellerPrice;
        String condition = njRecord.condition;
        String character = njRecord.character;
        String reference = njRecord.reference;
        String CODE = njRecord.CODE;
        String LR = njRecord.LR;
        String COST = njRecord.COST;

        String orderNumber = njRecord.OrderNumber;
        String ACCOUNT = njRecord.ACCOUNT;
        String lastCode = njRecord.LastCode;
        Double value = njRecord.value;
        String email = njRecord.email;
        String SalesChannel = njRecord.SalesChannel;

        if (!URL.contentEquals("")) {
            Condition cnd = Cnd.where("orderId", "like", orderId).and("sheetName", "like", sheetName).and("URL",
                    "like", URL).and("status", "like", status);
            List<Nj.ALLRecord> NJ_All_recordList = dbManager.query(Nj.ALLRecord.class, cnd);

            if (NJ_All_recordList.size() >= 1 && status.contentEquals("1")) {
                LOGGER.info("orderId:{} has existed in general database!" ,orderId);
                //TODO 方法参数太多，同类型参数可以考虑隔开
                outputSheet(date1, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
                        value, email,SalesChannel);
            } else if (NJ_All_recordList.size() == 0 && status.contentEquals("1")) {
                LOGGER.info("orderId:{} is new!" ,orderId);
                outputSheet(date1, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
                        value, email,SalesChannel);
                outputGeneral(status, orderId, sheetName, URL, price, ACC, recipientName, isbn, quantityPurchased,
                        remark, itemName, shipAddress1, shipAddress2, shipCity, shipState, shipZip, shipCountry,
                        shipPhoneNumber, purchaseDate, skuAddress, sku, shippingFee, isbnAddress, SELLER, sellerID,
                        sellerPrice, condition, character, reference, CODE, LR, COST, orderNumber, ACCOUNT, lastCode,
                        value, email,SalesChannel);
                    //TODO BeanUtils 了解
//                ALLRecord record = new ALLRecord();
//                BeanUtils.copyProperties(njRecord, record);
//                dbManager.save(njRecord, Nj.ALLRecord.class);
            }

        } else if (URL.contentEquals(""))
        LOGGER.info("orderId:{} of URL is null!" ,orderId);
    }

    void outputGeneral(String status, String orderId, String sheetName, String URL, String price, String ACC,
                       String recipientName, String isbn, String quantityPurchased, String remark, String
                                      itemName, String shipAddress1, String shipAddress2, String shipCity, String
                                      shipState, String shipZip, String shipCountry, String shipPhoneNumber, String
                                      purchaseDate, String skuAddress, String sku, String shippingFee, String
                                      isbnAddress, String SELLER, String sellerID, String sellerPrice, String
                                      condition, String character, String reference, String CODE, String LR, String
                                      COST, String orderNumber, String ACCOUNT, String lastCode, Double value, String email,String SalesChannel) {

        if (sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
            dbManager.save(new Nj.ALLRecord(Tools.generateUUID(), ACC, sheetName, status, orderId, recipientName,
                    URL, isbn, quantityPurchased, remark, itemName, shipAddress1, shipAddress2,
                    shipCity, shipState, shipZip, shipCountry, shipPhoneNumber, purchaseDate, skuAddress, sku,
                    price, shippingFee, isbnAddress, SELLER, sellerID, sellerPrice, condition, character, reference,
                    CODE, LR, COST, orderNumber, ACCOUNT, lastCode, value, email,SalesChannel), Nj.ALLRecord.class);
    }

    private Map<String, Long> notifyTimestamps = new HashMap<>();

    @Repeat(times = 8, sleepTime = 10)
    boolean outputSheet(String date1, String orderId, String sheetName, String URL, String price, String ACC,
                        String recipientName, String isbn, String quantityPurchased, String remark, String
                                       itemName, String shipAddress1, String shipAddress2, String shipCity,
                        String shipState, String shipZip, String shipCountry, String shipPhoneNumber, String
                                       purchaseDate, String skuAddress, String sku, String shippingFee, String
                                       isbnAddress, String SELLER, String sellerID, String sellerPrice, String
                                       condition, String character, String reference, String CODE, String LR,
                        String COST, String orderNumber, String ACCOUNT, String lastCode, Double value, String email,String SalesChannel) {
        shipAddress1= shipAddress1.replace("|",",");
        shipAddress2= shipAddress2.replace("|",",");
        itemName = itemName.replace("|",",");
        recipientName = recipientName.replace("|",",");
        String profitValue = price + "+"+ COST + "+"+ sellerPrice +"+"+ shippingFee +"+"+ sku+"+"+SalesChannel ;

        String orderSummary = sheetName + "|" + date1 + "|" + ACC + "|" + orderId + "|" + "" + "|" + recipientName +
                "|" + URL + "|" + isbn + "|" + "" + "|" + "" + "|" + quantityPurchased + "|" + remark + "|" +
                itemName + "|" + shipAddress1
                + "|" + shipAddress2 + "|" + shipCity + "|" + shipState + "|" + shipZip + "|" + shipCountry + "|" +
                shipPhoneNumber + "|" + purchaseDate + "|" + skuAddress + "|" + sku + "|" + price + "|" + shippingFee
                + "|" + isbnAddress + "|" + SELLER + "|" + sellerID + "|" + sellerPrice + "|" + condition + "|" +
                character + "|" + reference + "|" + CODE + "|" + LR + "|" + COST + "|" + orderNumber + "|" + ACCOUNT
                + "|" + lastCode + "|" + value + "|" + profitValue + "|" + email;

        long start = System.currentTimeMillis();

        String url1 = String.format(Constants.ascript2Parameter, "WarehouseMonitor", PageUtils.encodeParamValue(orderSummary));
        String result;

        result = PageUtils.processResult(HttpUtils.getTextThriceIfFail(url1));
        if (OrderEnums.ReturnCode.Success.name().equals(result)) {
//                notifyTimestamps.remove("WarehouseMonitor");
            LOGGER.info("成功将搜索重复指标数据{}返回{}, 耗时{}", orderId, "WarehouseMonitor", Tools.formatCostTime(start));
            return true;
        }

        LOGGER.warn("将搜索重复指标数据{}返回{}失败: {}. 尝试重复操作.", orderId, "WarehouseMonitor", result);

        if (Tools.contains(result, "above the limit of 2000000 cells")) {
            // 不重复发送
            Long timestamp = notifyTimestamps.get("WarehouseMonitor");
            System.out.print(System.currentTimeMillis() + "\n");
            System.out.print("timestamp:" + timestamp + "\n");
            System.out.print("toMillis:" + TimeUnit.HOURS.toMillis(3) + "\n");
            if (timestamp != null && (System.currentTimeMillis() - timestamp < TimeUnit.HOURS.toMillis(3))) {
                LOGGER.info("{}对应表格清理通知邮件已在{}发送过了, 短期内无需重复发送", "WarehouseMonitor", DateFormat.DATE_TIME.format
                        (timestamp));
                return true;
            }

            // 数据总量超过上限, 需及时清理对应表格, 发送通知邮件  ServiceConstants.MAIL_MAN_RND_EMAIL
            try {
                String subject = String.format("Spreadsheet of %s needs cleanse and backup ASAP!!", "WarehouseMonitor");
                String content = subject + StringUtils.LF + StringUtils.defaultString(result);
                ApplicationContext.getBean(EmailSender.class).sendGmail(subject, content, EmailSender
                                .EmailContentType.PlainText,
                        new Account("mailmanibport@gmail.com", "ibport7777", AccountType.EmailSender), "Cleanse Notifier", new Destination().withToAddresses
                                ("joyriver7@gmail.com,jackwen777@gmail.com"));

                notifyTimestamps.put("WarehouseMonitor", System.currentTimeMillis());
            } catch (Exception e) {
                // -> Ignore
            }
            return true;
        }
        return false;
    }

    void clearNJ() {
        dbManager.clearAll(Nj.NJRecord.class);
        dbManager.clearAll(Nj.ALLRecord.class);
        System.out.print("Clear successfully!");

    }


    public static void main(String[] args) {
        String skuAA = "32";
        String skuAB= StringUtils.leftPad(skuAA, 4, '0');
        System.out.println(skuAB);
    }
}
