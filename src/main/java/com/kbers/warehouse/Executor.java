package com.kbers.warehouse;

import com.amzass.model.submit.Order;
import com.amzass.service.common.ApplicationContext;
import com.amzass.utils.common.Tools;

import com.google.inject.Inject;
import com.mailman.model.common.Sheets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/12/17 20:04
 */
class Executor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);
    @Inject
    private AppScript appScript;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    @Inject
    WarehouseDate  Time;

    void execute(final boolean isJapan, final boolean isWholeDatabase)  {
        SpreadSheetDownloading nj = ApplicationContext.getBean(SpreadSheetDownloading.class);
        int length = nj.downloadSidLinkSpreadId(isJapan).length;
        final CountDownLatch countDownLatch = new CountDownLatch(length);
        for (final String spreadIdAccEmail : nj.downloadSidLinkSpreadId(isJapan)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        readSpreadIdAccEmail(spreadIdAccEmail, isJapan, isWholeDatabase);
                    } catch (Exception e) {
                        LOGGER.error("读取Spreadsheet {}异常:", spreadIdAccEmail, e);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
        }
        Tools.await(countDownLatch);
    }

    void readSpreadIdAccEmail(String spreadIdAccEmail, boolean isJapan, boolean isWholeDatabase) {
        String spreadIdAccEmailSplit [] = new String[0];
        spreadIdAccEmailSplit = spreadIdAccEmail.split("[|]");
        System.out.println("length:" + spreadIdAccEmailSplit.length);
        String acc = "";
        String spreadId = "";
        String email ="";
        if(spreadIdAccEmailSplit.length == 3) {
            acc = spreadIdAccEmail.split("[|]")[0];
            spreadId = spreadIdAccEmail.split("[|]")[1];
            email = spreadIdAccEmail.split("[|]")[2];
        } else if(spreadIdAccEmailSplit.length == 2){
            acc = spreadIdAccEmail.split("[|]")[0];
            spreadId = spreadIdAccEmail.split("[|]")[1];
        }

        Sheets sheets = appScript.getSheets(spreadId);
        List<String> results = this.filter(sheets, isWholeDatabase);
        for (String sheetName : results) {
            processSingleSheet(acc, spreadId, email, sheets, sheetName, isJapan, isWholeDatabase);
        }
    }

    List<String> filter(Sheets sheets, boolean isWholeDatabase) {
        List<String> results = new ArrayList<>();
        int index = 0;
        int singleRange;
        if (isWholeDatabase)
            singleRange = Constants.allSingleRange;
        else
            singleRange = Constants.tempSingleRange;

        for (String sheetName : sheets.getSheetNames()) {
                index++;

                if (index <= 3 || index > singleRange) {
                    continue;
                }
                try {
                    if (sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX)) {
                        String frontSheetName = sheetName.split("/")[0];
                        String backSheetName = sheetName.split("/")[1];
                        if (Time.yesterday() >= Time.dateRange) {
                            if (Integer.parseInt(frontSheetName) == Time.month() && Integer.parseInt
                                    (backSheetName) <= Time.yesterday() && Integer.parseInt
                                    (backSheetName)  >= Time.farDate()) {
                                results.add(sheetName);
                            }
                        } else  if(Time.yesterday() < Time.dateRange){
                            if ((Integer.parseInt(frontSheetName) == Time.month() && Integer.parseInt
                                    (backSheetName) <= Time.yesterday()) || (Integer.parseInt(frontSheetName) == Time.lastMonth() && Integer.parseInt(backSheetName) >= Time.farDate())) {
                                results.add(sheetName);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("filter failed {}:", sheetName, e);
                }

        }
        return results;
    }

    private void processSingleSheet(String acc, String spreadId, String email, Sheets sheets, String sheetName, boolean isJapan,
                                    boolean isWholeDatabase) {
        List<Order> orders = appScript.readOrders(sheets.getSpreadName(), sheets.getSpreadId(), sheetName);

        for (Order order : orders) {
            processSingleOrder(acc, order, email, isJapan, isWholeDatabase);
        }
        long start = System.currentTimeMillis();
        LOGGER.info("{} -> {}下面{}条订单已经全部保存, 耗时{}", spreadId, sheetName, orders.size(), Tools.formatCostTime(start));
    }

    void processSingleOrder(String acc, Order order, String email, boolean isJapan, boolean isWholeDatabase) {

        String skuAG = order.ship_country;
        double skuAC = 0;

        if(order.cost.matches(Constants.COST_DOUBLE_REGEX)||order.cost.matches(Constants.INTEGER_REGX))  //contentEquals("") && !order.cost.contains(" "))
            skuAC = Double.parseDouble(order.cost);

        double value = 0;
        String skuAB = order.ship_phone_number;
        String skuAA = order.ship_zip;


        if (skuAG.contentEquals("Germany") && skuAC < 50)
            value = skuAC;
        else if (skuAG.contentEquals("Germany") && skuAC >= 50)
            value = 50;
        else if (order.cost.contentEquals("") && skuAG.contentEquals("Germany"))
            value = 12;
        else if (!skuAG.contentEquals("Germany") && skuAC < 14.9)
            value = skuAC;
        else if (!skuAG.contentEquals("Germany") && skuAC >= 14.9)
            value = 14.9;
        else if (order.cost.contentEquals("") && !skuAG.contentEquals("Germany"))
            value = 12;


        if (skuAB.contentEquals(""))
            skuAB = "201-777-1289";

        if (skuAG.contentEquals("Argentina") || skuAG.contentEquals("Australia") || skuAG.contentEquals("Austria") ||
                skuAG.contentEquals("Belgium") || skuAG.contentEquals("Cyprus") || skuAG.contentEquals("Denmark") ||
                skuAG.contentEquals("Liechtenstein") || skuAG.contentEquals("Luxembourg") || skuAG.contentEquals
                ("Norway") || skuAG.contentEquals("New Zealand") || skuAG.contentEquals("Switzerland(CH)")) {
            skuAA = StringUtils.leftPad(skuAA, 4, '0');
        } else {
            skuAA = StringUtils.leftPad(skuAA, 5, '0');
        }
        String stats = order.fulfilled()? "1" :"0" ;

        //TODO 扩展性未来
        // Warehouse: Nj/Jp/DE, ALL/TMP
        // order_{1}_{2}
        // dbManager.save(pojo, tableName);
        if (!isJapan && (!isWholeDatabase)) {
            Nj nj = ApplicationContext.getBean(Nj.class);
            if (order.sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
                nj.dbManager.save(new Nj.NJRecord(Tools.generateUUID(), acc, order.sheetName, stats, order.order_id,
                                order.recipient_name, order.url, order.isbn,
                                order.quantity_purchased, order.remark, order.item_name, order.ship_address_1, order.ship_address_2,
                                order.ship_city, order.ship_state, skuAA, order.ship_country, skuAB,
                                order.purchase_date, order.sku_address, order.sku,
                                order.price, order.shipping_fee, order.isbn_address, order.seller, order.seller_id,
                                order.seller_price, order.condition, order.character, order.reference,
                                order.code, order.profit, order.cost, order.order_number, order.account,
                                order.last_code, value, email,order.salesChanel), Nj.NJRecord.class);
        } else if (!isJapan && isWholeDatabase) {
            Nj nj = ApplicationContext.getBean(Nj.class);
            if (order.sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
                nj.dbManager.save(new Nj.ALLRecord(Tools.generateUUID(), acc, order.sheetName, stats, order.order_id,
                                order.recipient_name, order.url, order.isbn,
                                order.quantity_purchased, order.remark, order.item_name, order.ship_address_1, order.ship_address_2,
                                order.ship_city, order.ship_state, skuAA, order.ship_country, skuAB,
                                order.purchase_date, order.sku_address, order.sku,
                                order.price, order.shipping_fee, order.isbn_address, order.seller, order.seller_id,
                                order.seller_price, order.condition, order.character, order.reference,
                                order.code, order.profit, order.cost, order.order_number, order.account,
                                order.last_code, value, email,order.salesChanel), Nj.ALLRecord.class);
        } else if (isJapan && (!isWholeDatabase)) {
            Jp nj = ApplicationContext.getBean(Jp.class);
            if (order.sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
                nj.dbManager.save(new Jp.JPRecord(Tools.generateUUID(), acc, order.sheetName, stats, order.order_id,
                                order.recipient_name, order.url, order.isbn,
                                order.quantity_purchased, order.remark, order.item_name, order.ship_address_1, order.ship_address_2,
                                order.ship_city, order.ship_state, skuAA, order.ship_country, skuAB,
                                order.purchase_date, order.sku_address, order.sku,
                                order.price, order.shipping_fee, order.isbn_address, order.seller, order.seller_id,
                                order.seller_price, order.condition, order.character, order.reference,
                                order.code, order.profit, order.cost, order.order_number, order.account,
                                order.last_code, value, email,order.salesChanel), Jp.JPRecord.class);
        } else if (isJapan && isWholeDatabase) {
            Jp nj = ApplicationContext.getBean(Jp.class);
            if (order.sheetName.matches(Constants.ORDER_UPDATE_SHEET_NAME_REGEX))
                nj.dbManager.save(new Jp.JPALLRecord(Tools.generateUUID(), acc, order.sheetName, stats, order.order_id, order.recipient_name, order.url, order.isbn,
                                order.quantity_purchased, order.remark, order.item_name, order.ship_address_1, order.ship_address_2,
                                order.ship_city, order.ship_state, skuAA, order.ship_country, skuAB, order.purchase_date,
                                order.sku_address, order.sku, order.price, order.shipping_fee, order.isbn_address, order.seller, order.seller_id,
                                order.seller_price, order.condition, order.character, order.reference, order.code, order.profit, order.cost, order.order_number, order.account,
                                order.last_code, value, email,order.salesChanel), Jp.JPALLRecord.class);
        }


    }
}
