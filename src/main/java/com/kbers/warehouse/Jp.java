package com.kbers.warehouse;

import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.kber.commons.model.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;

/**
 * <a href="mailto:joyriver7@gmail.com">Jinxi Hong</a> 2016/11/23 15:52
 */
public class Jp {
    @Inject
    DBManager dbManager;

    @Inject
    public void init() {
        dbManager.getDao().create(Jp.JPRecord.class, false);
        dbManager.getDao().create(Jp.JPALLRecord.class, false);
    }

    @Table(value = "jp_record")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JPRecord extends PrimaryKey {
        @Name private String id;
        @Column public String ACC;
        @Column public String sheetName;
        @Column public String status;
        @Column public String orderId;
        @Column public String recipientName;
        @Column public String URL;
        @Column public String isbn;
        @Column public String quantityPurchased;
        @Column public String Remark;
        @Column public String itemName;
        @Column public String shipAddress1;
        @Column public String shipAddress2;
        @Column public String shipCity;
        @Column public String shipState;
        @Column public String shipZip;
        @Column public String shipCountry;
        @Column public String shipPhoneNumber;
        @Column public String purchaseDate;
        @Column public String skuAddress;
        @Column public String sku;
        @Column public String price;
        @Column public String shippingFee;
        @Column public String isbnAddress;
        @Column public String SELLER;
        @Column public String sellerID;
        @Column public String sellerPrice;
        @Column public String condition;
        @Column public String character;
        @Column public String reference;
        @Column public String CODE;
        @Column public String LR;
        @Column public String COST;
        @Column public String OrderNumber;
        @Column public String ACCOUNT;
        @Column public String LastCode;
        @Column public double value;
        @Column public String email;
        @Column public String SalesChannel;

        @Override
        public String getPK() {
            return id;
        }
    }

    //TODO 研究动态表名，一个model映射过去
    @Table(value = "jp_all_record")
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JPALLRecord extends JPRecord {
        @Name private String id;
        @Column public String ACC;
        @Column public String sheetName;
        @Column public String status;
        @Column public String orderId;
        @Column public String recipientName;
        @Column public String URL;
        @Column public String isbn;
        @Column public String quantityPurchased;
        @Column public String Remark;
        @Column public String itemName;
        @Column public String shipAddress1;
        @Column public String shipAddress2;
        @Column public String shipCity;
        @Column public String shipState;
        @Column public String shipZip;
        @Column public String shipCountry;
        @Column public String shipPhoneNumber;
        @Column public String purchaseDate;
        @Column public String skuAddress;
        @Column public String sku;
        @Column public String price;
        @Column public String shippingFee;
        @Column public String isbnAddress;
        @Column public String SELLER;
        @Column public String sellerID;
        @Column public String sellerPrice;
        @Column public String condition;
        @Column public String character;
        @Column public String reference;
        @Column public String CODE;
        @Column public String LR;
        @Column public String COST;
        @Column public String OrderNumber;
        @Column public String ACCOUNT;
        @Column public String LastCode;
        @Column public double value;
        @Column public String email;
        @Column public String SalesChannel;

        @Override
        public String getPK() {
            return id;
        }
    }


}
