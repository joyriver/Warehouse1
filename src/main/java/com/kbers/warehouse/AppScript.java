package com.kbers.warehouse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.amzass.common.Application;
import com.amzass.enums.common.Customize;
import com.amzass.model.distribution.Version;
import com.amzass.model.sheet.Sheet;
import com.amzass.model.submit.Order;
import com.amzass.model.submit.OrderEnums.OrderColor;
import com.amzass.model.submit.OrderEnums.OrderColumn;
import com.amzass.model.submit.OrderEnums.ReturnCode;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.distribution.VersionManager;
import com.amzass.service.sheet.OrderHelper;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.common.*;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Exceptions.OrderNotFoundException;
import com.amzass.utils.common.RegexUtils.Regex;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kber.aop.Profile;
import com.kber.aop.Repeat;
import com.kber.commons.DBManager;
import com.mailman.model.common.BaseStatisticSummary;
import com.mailman.model.common.Settings;
import com.mailman.model.common.Sheets;
import com.mailman.model.common.TransferTrackingData;
import com.mailman.model.feedback.FeedbackSendStatistic;
import com.mailman.model.feedback.MarketWebServiceIdentity;
import com.mailman.model.feedback.SimpleEmailServiceIdentity;
import com.mailman.model.trackingcrawl.TrackingCrawlStatistic;
import com.mailman.service.common.DataSource.DataSourceCategory;
import com.mailman.utils.ServiceConstants;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * 基于Google App Script读取、操作Google Sheet，Deploy之后在Java端基于Jsoup或者HttpClient访问
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Nov 29, 2014 11:41:26 AM
 */
@Singleton
public class AppScript {
    private final Logger logger = LoggerFactory.getLogger(AppScript.class);
    private final String appScriptUrl = "https://script.google.com/macros/s/AKfycby3oR8IH5Z7uaTi-i_GPUfWeeFV96ejxfyo3dlq8tXZivpW51F0/exec";
    private static final String SUCCESS = ReturnCode.Success.name();
    private static final String FAIL = ReturnCode.Fail.name();
    private static final String SKIP = ReturnCode.Skip.name();
    private static final String PARAM_SHEET_NAME = "sn";
    private static final String PARAM_SPREAD_ID = "s";
    private static final String PARAM_METHOD = "method";
    private static final String PARAM_ROW = "row";
    private static final String PARAM_VALUE = "val";

    /**
     * 基于App Script所要执行的方法，与OrderMan.gs中定义的方法一致
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Dec 8, 2014 1:19:21 PM
     */
    public enum Method {
        /** 按照给定的范围读取订单集合 */
        READ,
        /** 刷新读取当前订单 */
        REFRESHLYREAD,
        /** 为某一行标上指定的颜色 */
        BGCOLOR,
        /** 读取给定Spread Sheet中所有的页签名称 */
        GETSHEETNAMES,
        /** 读取给定Spread Sheet的元数据信息, {@link #GETSHEETNAMES}的升级版本 */
        GETSPREADMETADATA,
        /** Read tracking# in UK Forwarded Order Sheets */
        UKTRACKNO,
        /** Read tracking# in US Forwarded Order Sheets */
        USTRACKNO,
        /** Read tracking# in NJ WareHouse Export Trasnfer Order Sheets */
        NJEXPORTTRACKNO,
        /** Write Buyer Cancel Log in '备忘' sheet of latest google sheet */
        BUYERCANCELLOG,
        /** Clear seller data and mark the row gray color */
        CLEARSELLERANDMARKGRAY,
        /** Mark 'Buyer Cancel' in column AD */
        MARKBUYERCANCEL,
        /** 更新一行订单的AD列数据 */
        UpdateColumnAD,
        /** Mark 'Seller Cancel' in column AD and perform other appendix operations */
        MARKSELLERCANCEL,
        /** Find row number by order id */
        FINDROWBYORDERID,
        /** Find row number by order number and id */
        FINDROWBYORDERNOANDID,
        /** Read SKU of Gray Orders */
        GRAYORDERSKU,
        /** Clear Seller Information and Mark 'Seller Cancel' */
        CLEARSELLERINFO,
        /** 获取Google Spreadsheet的最后修改时间 */
        GETLASTUPDATE,
        /** 上传客服助手配置关键信息 */
        UploadMailManConfig
    }

    public List<String> getSheetNames(String spreadId) {
        String result = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_SPREAD_ID, spreadId);
            params.put(PARAM_METHOD, Method.GETSHEETNAMES.name());
            try {
                result = this.processResult(this.get(params));
                return JSON.parseArray(result, String.class);
            } catch (BusinessException | JSONException e) {
                logger.error("返回结果{}不包含有效的Sheet名称数组，尝试重复操作", result);
            }
        }

        String msg = String.format("Failed to read sheet names of Google Spreadsheet(ID: %s) after %s attempts: %s",
                spreadId, com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(result));
        throw new BusinessException(msg);
    }

    /**
     * <pre>
     * 将给定的Google Sheet某一行指定(多)列标上指定的颜色。
     * 场景：订单失效或不可做时。程序找到Seller将数据写入时。将某些订单标上高优先级时等等。
     * 如果一次不能成功，尝试重复执行一次
     * </pre>
     * @param sheetUrl		当前google sheet的url
     * @param sheetName		当前google sheet页签名称
     * @param row			要标灰的行
     * @param notation		要标灰的区域，形如A2:B2等等(也即只将若干列标灰)
     */
    private String markColor(String sheetUrl, String sheetName, int row, String notation, OrderColor color) {
        Map<String, String> params = new HashMap<>();
        String spreadId = Tools.getSpreadId(sheetUrl);
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_ROW, String.valueOf(row));
        if (StringUtils.isNotBlank(notation)) {
            params.put("r", notation);
        }
        params.put(PARAM_METHOD, Method.BGCOLOR.name());
        params.put("code", color.code());

        String result = this.get(params);
        logger.debug("将第{}行订单标上颜色{}(代码:{})返回结果:{}", row, color, color.code(), result);
        if (!SUCCESS.equals(result)) {
            return this.get(params);
        }
        return result;
    }

    /**
     * 将给定的Google Sheet某一行标上指定的颜色
     * @see #markColor(String, String, int, String, OrderColor)
     */
    String markColor(String sheetUrl, String sheetName, int row, OrderColor color) {
        return this.markColor(sheetUrl, sheetName, row, StringUtils.EMPTY, color);
    }

    /**
     * 对访问App Script WebService的结果进行处理，比如某些情况下返回HTML，只提取其中的有效内容
     */
    String processResult(String result) {
        return PageUtils.processResult(result);
    }

    /**
     * 根据App Script返回的结果，解析一条订单的信息，该结果格式形如['finish', 'Joker Yang', ...]，最后一列为当前行数
     */
    private Order parseSingleOrder(String result) {
        String[] array = JSONArray.parseObject(result, String[].class);
        Order order = new Order();
        for (int j = OrderColumn.STATUS.number(); j <= OrderColumn.SHIP_COUNTRY.number(); j++) {
            OrderHelper.setColumnValue(j, array[j - 1], order);
        }
        order.row = NumberUtils.toInt(array[OrderColumn.SHIP_COUNTRY.number()]);
        OrderHelper.autoCorrect(order);
        return order;
    }

    /**
     * 根据给定的程序运行参数设置读取订单集合
     * @return	原始订单集合(没有任何过滤和筛选)
     */
    public List<Order> readOrders(@NotNull Sheet sheet) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, sheet.spreadId);
        params.put(PARAM_SHEET_NAME, sheet.name);
        params.put(PARAM_METHOD, Method.READ.name());
        params.put("r", sheet.notation.value());

        JSONException ex = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            try {
                long start = System.currentTimeMillis();
                String json = this.processResult(this.get(params));
                List<Order> orders = this.parse(json, sheet);
                logger.debug("基于AppScript读取{}下面{}条订单完毕, 耗时: {}", sheet.abbrev(), orders.size(), Tools.formatCostTime(start));
                return orders;
            } catch (JSONException e) {
                ex = e;
                logger.error("第{}次读取{}订单数据失败: {}", i + 1, sheet.abbrev(), e.getMessage());
                if (i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES - 1) {
                    PageLoadHelper.WaitTime.Short.execute();
                }
            }
        }
        throw ex;
    }

    public List<Order> readOrders(String spreadTitle, String spreadId, String sheetName) {
        return this.readOrders(new Sheet(spreadId, spreadTitle, sheetName));
    }

    List<Order> readOrders(String sheetUrl, String sheetName) {
        String spreadId = Tools.getSpreadId(sheetUrl);
        return this.readOrders(new Sheet(spreadId, null, sheetName));
    }

    /**
     * 读取订单列表的结果集，对应Google App Script返回的JSON
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Dec 8, 2014 1:07:17 PM
     */
    @Data
    private static class ReadResult {
        public boolean valid() {
            return !(startRow == 0 && endRow == 0 && multiRows == null && (orders == null || orders.length == 0));
        }

        private int startRow;
        private int endRow;
        private String multiRows;
        private String[][] orders;
        private String[][] colors;
    }

    public boolean validateOrderId(String orderId) {
        return Regex.AMAZON_ORDER_NUMBER.isMatched(orderId) ||
                RegexUtils.match(orderId, "#[A-Za-z0-9]*-[0-9]*") || RegexUtils.match(orderId, "#[0-9]*");
    }

    /**
     * 解析App Script返回的JSON为订单集合
     * @param json		App Script返回的JSON字符串，对应{@link ReadResult}
     */
    private List<Order> parse(String json, @NotNull Sheet sheet) {
        ReadResult result;
        try {
            result = JSONArray.parseObject(json, ReadResult.class);
        } catch(JSONException e) {
            throw new JSONException(String.format("Failed to parse order data in AppScript response result: %s", json));
        }

        if (result == null || !result.valid()) {
            throw new JSONException(String.format("There is no order data in AppScript response result: %s", json));
        }

        List<Order> orders = new ArrayList<>();
        int startRow = result.startRow, endRow = result.endRow;
        if (startRow <= 0 && endRow <= 0 && StringUtils.isBlank(result.multiRows)) {
            return orders;
        }

        String[] multiRows = StringUtils.split(StringUtils.defaultString(result.multiRows), com.amzass.utils.common.Constants.COMMA);
        String[][] array = result.orders;
        String[][] colors = result.colors;
        for (int i = 0; i < array.length; i++) {
            Order order = new Order();
            if (startRow == -1) {
                order.row = NumberUtils.toInt(multiRows[i]);
            } else {
                order.row = startRow + i;
            }

            String[] color = colors[i];
            order.color = color[0];

            String[] columns = array[i];
            int maxCol = columns.length >= OrderColumn.TRACKING_NUMBER.number() ? OrderColumn.TRACKING_NUMBER.number() : columns.length;
            for (int j = OrderColumn.STATUS.number(); j <= maxCol; j++) {
                OrderHelper.setColumnValue(j, columns[j - 1], order);
            }

            if (!this.validateOrderId(order.order_id)) {
                continue;
            }

            order.spreadTitle = sheet.spreadTitle;
            order.sheetName = sheet.name;
            order.spreadId = sheet.spreadId;
            try {
                OrderHelper.autoCorrect(order);
            } catch (RuntimeException e) {
                logger.warn("{}数据自动修正过程中出现异常:{}", order.id(), Tools.getExceptionMsg(e));
                continue;
            }
            orders.add(order);
        }
        return orders;
    }

    /**
     * 访问Google App Script: OrderMan部署好的Web Service，获取返回结果字符串
     * @param params	参数
     * @return Server端的App Script处理、返回的结果
     */
    public String get(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        String url = appScriptUrl + params4Url;
        return HttpUtils.getText(url);
    }

    /**
     * 将参数键值对拼接到url中
     * @param params	参数
     */
    String params2Url(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            sb.append(com.amzass.utils.common.Constants.QUESTION_MARK);
            int i = 0;
            for (Entry<String, String> entry : params.entrySet()) {
                if (i++ > 0) {
                    sb.append(com.amzass.utils.common.Constants.AND);
                }
                sb.append(entry.getKey()).append(com.amzass.utils.common.Constants.EQUALS);
                String value = PageUtils.encodeParamValue(entry.getValue());
                sb.append(value);
            }
        }
        return sb.toString();
    }

    /**
     * 刷新、读取当前订单在google sheet中的状态
     * @param sheetUrl		当前google sheet的url
     * @param sheetName		当前google sheet的名称
     * @param order			当前订单
     */
    Order readRefreshedOrder(String sheetUrl, String sheetName, Order order) {
        Map<String, String> params = new HashMap<>();
        String sheetId = Tools.getSpreadId(sheetUrl);
        params.put(PARAM_SPREAD_ID, sheetId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_METHOD, Method.REFRESHLYREAD.name());

        this.setBaseOrderParams(order, params);

        String errorMsg = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            String json = this.processResult(this.get(params));
            try {
                return this.parseSingleOrder(json);
            } catch (JSONException e) {
                errorMsg = String.format("Failed to read latest data of %s via parsing AppScript response result %s", order.id(), json);
            }
        }

        throw new BusinessException(StringUtils.defaultString(errorMsg));
    }

    private void setBaseOrderParams(Order order, Map<String, String> params) {
        params.put(PARAM_ROW, String.valueOf(order.row));
        params.put("order_id", order.order_id);
        params.put("isbn", order.isbn);
        params.put("seller", order.seller);
        params.put("seller_id", order.seller_id);
        params.put("recipient_name", order.recipient_name);
    }

    private <T> T readCertainColumns(String spreadId, String sheetName, Method method, Class<T> clazz) {
        String methodName = method.name();
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_METHOD, methodName);

        String json = this.processResult(this.get(params));
        try {
            return JSONArray.parseObject(json, clazz);
        } catch (JSONException e) {
            throw new BusinessException(String.format("Failed to read %s-%s, %s in AppScript response result %s", spreadId, sheetName, methodName, json));
        }
    }

    private String[][] readTrackNos(String spreadId, String sheetName, DataSourceCategory category) {
        if (category == DataSourceCategory.UKForward) {
            return this.readCertainColumns(spreadId, sheetName, Method.UKTRACKNO, String[][].class);
        } else if (category == DataSourceCategory.USForward) {
            return this.readCertainColumns(spreadId, sheetName, Method.USTRACKNO, String[][].class);
        } else if (category == DataSourceCategory.NJExport) {
            return this.readCertainColumns(spreadId, sheetName, Method.NJEXPORTTRACKNO, String[][].class);
        }
        throw new IllegalArgumentException("Current DataSource category is " + category + ", however only LAExport, NJExport, UKForward and USForward are accepted.");
    }

    public List<TransferTrackingData> readTrackingData(String spreadId, String sheetName, DataSourceCategory category) {
        String[][] data = this.readTrackNos(spreadId, sheetName, category);
        List<TransferTrackingData> list = new ArrayList<>(data.length);
        for (String[] array : data) {
            TransferTrackingData trackingData = new TransferTrackingData(spreadId, sheetName, array[0], array[1], array[2]);
            if (array.length >= 9) {
                trackingData.color = array[3];
                trackingData.forwardedCode = array[4];
                trackingData.reference = array[5];
                trackingData.orderNumber = array[6];
                trackingData.shipToCustomerDate = array[7];
                trackingData.trAtColumnF = array[8];
            }
            list.add(trackingData);
        }
        return list;
    }

    public String[] readGrayOrderSkus(String spreadId, String sheetName) {
        return this.readCertainColumns(spreadId, sheetName, Method.GRAYORDERSKU, String[].class);
    }

    public void writeBuyerCancelLog(String spreadId, String sheetName, String orderId) {
        String today = DateHelper.today();
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put("order_id", orderId);
        params.put(PARAM_METHOD, Method.BUYERCANCELLOG.name());

        boolean success = false;
        String error = StringUtils.EMPTY;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            String json = this.processResult(this.get(params));
            if (SUCCESS.equals(json)) {
                success = true;
                break;
            } else {
                error = json;
                logger.warn("第{}次尝试写入备注页签失败，详情:{}", i + 1, json);
            }

            Tools.sleep(PageUtils.SIMPLE_WAIT_MS);
        }

        String data = today + com.amzass.utils.common.Constants.TAB + orderId + com.amzass.utils.common.Constants.TAB + Remark.BUYER_CANCELLED;
        if (!success) {
            throw new BusinessException(String.format("Failed to write memo of buyer cancel log after %s attempts: %s. Please paste it manually: %s.", com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, error, data));
        }
    }

    public void clearSellerAndMarkGray(int row, String sheetName, String spreadId) {
        this.simpleOperation(row, sheetName, spreadId, Method.CLEARSELLERANDMARKGRAY);
    }

    private void simpleOperation(int row, String sheetName, String spreadId, Method method, String...values) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put(PARAM_ROW, String.valueOf(row));
        params.put(PARAM_METHOD, method.name());
        if (ArrayUtils.isNotEmpty(values)) {
            params.put(PARAM_VALUE, StringUtils.join(values, com.amzass.utils.common.Constants.COMMA));
        }

        boolean success = false;
        String json = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            json = this.processResult(this.get(params));
            if (SUCCESS.equals(json)) {
                success = true;
                break;
            }
            Tools.sleep(PageUtils.SIMPLE_WAIT_MS);
        }
        logger.info("{}->{}第{}行订单{}操作完成, 结果: {}, {}", spreadId, sheetName, row, method, success, json);
        if (!success) {
            throw new BusinessException(String.format("Failed to perform %s for order at row %s in google sheet %s after %s attempts: %s. Please try to do it manually.", method.name(), row, sheetName, com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(json)));
        }
    }

    public int findRowByOrderId(String orderId, String sheetName, String spreadId) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put("orderId", orderId);
        params.put(PARAM_METHOD, Method.FINDROWBYORDERID.name());

        String text = this.get(params);
        int rc = NumberUtils.toInt(text);
        if (rc == -1 || rc < 2) {
            throw new OrderNotFoundException(String.format("Failed to find row number in google sheet %s according to order id %s. AppScript response result: %s", sheetName, orderId, text));
        }
        return rc;
    }

    public int findRowByOrderNoAndId(String orderNo, String orderId, String sheetName, String spreadId) {
        if (StringUtils.isBlank(orderNo) || StringUtils.isBlank(orderId)) {
            throw new IllegalArgumentException(String.format("Order id %s or supplier order number %s is empty, which is not allowed.", StringUtils.defaultString(orderId), StringUtils.defaultString(orderNo)));
        }

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, spreadId);
        params.put(PARAM_SHEET_NAME, sheetName);
        params.put("orderNo", orderNo);
        params.put("orderId", orderId);
        params.put(PARAM_METHOD, Method.FINDROWBYORDERNOANDID.name());

        String text = this.get(params);
        int rc = NumberUtils.toInt(text);
        if (rc == -1 || rc < 2) {
            throw new OrderNotFoundException(String.format("Failed to find row number in google sheet %s according to order id %s and supplier order number %s. AppScript response result: %s", sheetName, orderId, orderNo, text));
        }
        return rc;
    }

    public void markBuyerCancel(int row, String sheetName, String spreadId) {
        this.simpleOperation(row, sheetName, spreadId, Method.MARKBUYERCANCEL);
    }

    public void markRefunded(int row, String sheetName, String spreadId) {
        this.simpleOperation(row, sheetName, spreadId, Method.UpdateColumnAD, "Fully refunded and notification letter sent");
    }

    public String markSellerCancel(int row, String sheetName, String spreadId) {
        return this.markSellerCancel(row, sheetName, spreadId, null, null, false, false);
    }

    public String markSellerCancel(int row, String sheetName, String spreadId,
                                   String cancelSheetName, String cancelSpreadId, boolean copy2CancelSheet, boolean ignoreExistence) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, StringUtils.defaultString(spreadId));
        params.put(PARAM_SHEET_NAME, StringUtils.defaultString(sheetName));
        params.put("cs", StringUtils.defaultString(cancelSpreadId));
        params.put("cn", StringUtils.defaultString(cancelSheetName));
        params.put(PARAM_ROW, String.valueOf(row));
        params.put("copy", String.valueOf(copy2CancelSheet));
        params.put("ignore", String.valueOf(ignoreExistence));
        params.put(PARAM_METHOD, Method.MARKSELLERCANCEL.name());

        String json = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            json = this.processResult(this.get(params));
            if (SUCCESS.equals(json) || SKIP.equals(json)) {
                return json;
            }

            Tools.sleep(PageUtils.SIMPLE_WAIT_MS);
        }

        throw new BusinessException(String.format("Failed to perform %s for order at row %s in google sheet %s after %s attempts: %s. Please try to do it manually.", Method.MARKSELLERCANCEL.name(), row, sheetName, com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(json)));
    }

    public void clearSellerInfo(int row, String sheetName, String spreadId) {
        this.simpleOperation(row, sheetName, spreadId, Method.CLEARSELLERINFO);
    }

    public long getLastUpdateTime(String spreadId) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_SPREAD_ID, StringUtils.defaultString(spreadId));
        params.put(PARAM_METHOD, Method.GETLASTUPDATE.name());
        String json;
        long start = System.currentTimeMillis();
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            try {
                json = this.processResult(this.get(params));
                long timestamp = NumberUtils.toLong(json, ServiceConstants.DUMMY_LAST_UPDATE_TIME);
                if (timestamp == ServiceConstants.DUMMY_LAST_UPDATE_TIME) {
                    logger.error("无法获取Google Spreadsheet {}的最后修改日期: {}", spreadId, json);
                    Tools.sleep(PageUtils.SIMPLE_WAIT_MS);
                    continue;
                }
                logger.debug("成功获取Spreadsheet {}的最后修改时间:{}，耗时{}", spreadId, DateHelper.toDateTime(new Date(timestamp)), Tools.formatCostTime(start));
                return timestamp;
            } catch (BusinessException e) {
                logger.error("第{}次读取{}最后修改时间过程中出现异常:", i + 1, spreadId, e);
            }
        }

        logger.error("经过{}次尝试仍无法获取{}对应Spreadsheet的最后修改时间", com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, spreadId);
        return ServiceConstants.DUMMY_LAST_UPDATE_TIME;
    }

    public Sheets getSheets(String spreadId) {
        String json = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            Map<String, String> params = new HashMap<>();
            params.put(PARAM_SPREAD_ID, spreadId);
            params.put(PARAM_METHOD, Method.GETSPREADMETADATA.name());
            try {
                json = this.processResult(this.get(params));
                return JSON.parseObject(json, Sheets.class);
            } catch (JSONException e) {
                logger.warn("第{}次尝试读取Google Spreadsheet(ID: {})信息失败, 结果{}无效", i + 1, spreadId, json);
            } catch (BusinessException e) {
                logger.warn("第{}次尝试读取Google Spreadsheet(ID: {})信息过程中出现异常:", i + 1, spreadId, e);
            }
            if (i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES - 1) {
                PageLoadHelper.WaitTime.Short.execute();
            }
        }
        throw new BusinessException("Cannot get spreadsheet(ID: " + spreadId + ") metadata from response: '" + StringUtils.defaultString(json) + "'");
    }

    /**
     * 验证码出现情况统计
     */
    public String recordCaptcha(String application, String accountType, String email, String url) {
        try {
            Preconditions.checkArgument(Tools.isNotBlank(email, url));
            Map<String, String> params = new HashMap<>();
            params.put("application", application);
            params.put("accountType", accountType);
            params.put("email", email);
            params.put("time", DateHelper.now());
            params.put("url", url);
            params.put(PARAM_METHOD, "RecordCaptcha");
            return this.exec(params);
        } catch (Exception e) {
            return Tools.getExceptionMsg(e);
        }
    }

    /**
     * 记录验证码处理结果
     */
    public void recordCaptchaResolveLog(String application, String accountType, String email, String url, String handlerId, boolean success) {
        try {
            Preconditions.checkArgument(Tools.isNotBlank(email, url));
            Map<String, String> params = new HashMap<>();
            params.put("application", application);
            params.put("accountType", accountType);
            params.put("email", email);
            params.put("time", DateHelper.now());
            params.put("url", url);
            params.put("result", (success ? SUCCESS : FAIL) + com.amzass.utils.common.Constants.SLASH + handlerId);
            params.put(PARAM_METHOD, "RecordCaptchaResolveLog");
            this.exec(params);
        } catch (Exception e) {
            // -> Ignore
        }
    }

    @Inject private VersionManager versionManager;
    @Inject private DBManager dbManager;

    public String uploadMailManConfig() {
        Map<String, String> params = new HashMap<>();

        Version version = versionManager.getCurrentVersion(Application.MailMan);
        params.put("version", version.getCode().toString());

        Settings settings = Tools.loadCustomize(Customize.MailEngine);
        params.put("acc", settings.context());
        params.put("user", settings.getUserName());
        params.put("bizType", settings.getBusinessType().name());
        params.put("datasource", settings.spreads());
        params.put("seller", settings.getSeller().key());
        params.put("buyers", settings.buyerEmails());
        SimpleEmailServiceIdentity ses = settings.getSesAuth();
        params.put("ses", ses != null && ses.valid() ? ses.toString() : StringUtils.EMPTY);
        MarketWebServiceIdentity mws = settings.getMwsAuth();
        params.put("mws", mws != null && mws.valid() ? mws.toString() : StringUtils.EMPTY);

        Cnd cnd = Cnd.where("context", com.amzass.utils.common.Constants.EQUALS, settings.context())
                .and("beginTime", ">=", DateHelper.beginOfDay(new DateTime()).getMillis() - PageLoadHelper.WaitTime.Long.valInMS())
                .and("endTime", "<", DateHelper.endOfDay(new DateTime()).getMillis());
        params.put("tracking", this.abbrevStatistic(dbManager.query(TrackingCrawlStatistic.class, cnd)));
        params.put("feedback", this.abbrevStatistic(dbManager.query(FeedbackSendStatistic.class, cnd)));

        params.put(PARAM_METHOD, Method.UploadMailManConfig.name());
        return this.exec(params);
    }

    private <T extends BaseStatisticSummary> String abbrevStatistic(List<T> statistics) {
        if (CollectionUtils.isEmpty(statistics)) {
            return com.amzass.utils.common.Constants.HYPHEN;
        }

        int total = 0, success = 0, fail = 0, skip = 0;
        for (BaseStatisticSummary statistic : statistics) {
            total += statistic.getTotal();
            success += statistic.getSuccess();
            fail += statistic.getFail();
            skip += statistic.getSkip();
        }
        // 如果失败总数在400以上，或者失败率在30%以上，报警查看
        String warning = fail >= BaseStatisticSummary.FAIL_THRESHOLD_COUNT || (total > 0 && fail * 100.0f / total >= BaseStatisticSummary.FAIL_THRESHOLD_RATE) ? ("!!WARNING!!" + StringUtils.LF) : StringUtils.EMPTY;
        return String.format("%sTotal: %s%nSuccess: %s%nFail: %s%nSkip: %s%nFailure Rate: %s", warning, total, success, fail, skip, (total > 0 ? com.amzass.utils.common.Constants.DOUBLE_FORMAT.format(fail * 100.0f / total) + com.amzass.utils.common.Constants.PERCENTAGE : com.amzass.utils.common.Constants.HYPHEN));
    }

    public String commitDHLClaimMemo(String urlParams) {
        return this.exec(urlParams);
    }

    private String exec(Map<String, String> params) {
        String params4Url = this.params2Url(params);
        return exec(params4Url);
    }

    @Profile
    @Repeat(sleepTime = 1, expectedException = BusinessException.class)
    public String getDHLClaimMetadata(String spreadId) {
        return this.processResult(HttpUtils.getText(appScriptUrl + "?method=GetDHLClaimMetadata&spreadId=" +spreadId));
    }

    /**
     * 执行一个AppScript请求, 适用于返回结果Success标识正确的场景
     * @param params4Url 请求参数, 形如:?method=CommitDHLClaimMemo&sid=EBEU&orderId=111-2222222-3333333...
     * @throws BusinessException 如果执行结果不成功抛出此异常
     */
    private String exec(String params4Url) {
        String url = appScriptUrl + params4Url;
        String json = null;
        for (int i = 0; i < com.amzass.utils.common.Constants.MAX_REPEAT_TIMES; i++) {
            try {
                json = this.processResult(HttpUtils.getText(url));
            } catch (BusinessException e) {
                json = e.getMessage();
                Tools.sleep(PageUtils.SIMPLE_WAIT_MS);
            }
            if (SUCCESS.equals(json) || Tools.startWithAny(json, SUCCESS)) {
                logger.debug("{}已成功执行, 结果:{}", PageUtils.decodeUrl(params4Url), StringUtils.abbreviate(json, com.amzass.utils.common.Constants.MAX_MSG_LENGTH * 2));
                return json;
            }
        }
        throw new BusinessException(String.format("Failed to invoke '%s' after %s attempts. Error detail:%n%s.", StringUtils.abbreviate(params4Url, com.amzass.utils.common.Constants.MAX_URL_LENGTH), com.amzass.utils.common.Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(json)));
    }

    public static void main(String[] args) {
        if (ArrayUtils.isEmpty(args)) {
            System.exit(4);
        }

        AppScript appScript = ApplicationContext.getBean(AppScript.class);
        if ("ReadOrder".equalsIgnoreCase(args[0])) {
            String spreadId = args[1];
            String sheetName = args[2];
            List<Order> orders = appScript.readOrders(spreadId, sheetName);
            if (CollectionUtils.isNotEmpty(orders)) {
                System.out.println(String.format("There are %d orders in sheet %s.", orders.size(), sheetName));
                System.out.println(JSON.toJSONString(orders.get(0), true));
            } else {
                System.err.println("No order is found in sheet " + sheetName);
            }
        }

        System.exit(0);
    }
}