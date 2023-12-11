package zhongchiedu.inventory.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Optional;

@Data
public class RequestBo {

    private String name;
    private String model;
    private String supplier;
    private String entryName;
    private String itemNo;
    private String purchaseInvoiceNo;
    private String purchaseInvoiceDate;
    private String paymentOrderNo;

    private String start;//开始

    private String end;//结束

    private String type;//进出

    private String confirm;//核对


    private String revoke;//正常 ，撤销

    private String searchArea;//区域选择

    private String  ssC;//分类

    private String id;//

    private String userId;

    private String projectName;//用于项目

    private String customer;//客户



    public  boolean isNotEmpty(Object s) {
        if (null == s || "".equals(s) || "".equals(String.valueOf(s).trim())
                || "null".equalsIgnoreCase(String.valueOf(s))) {
            return false;
        } else {
            return true;
        }
    }
    public boolean isEmpty(){
        return  !(isNotEmpty(name) || isNotEmpty(model) || isNotEmpty(supplier) || isNotEmpty(entryName) || isNotEmpty(itemNo) || isNotEmpty(purchaseInvoiceNo)
                || isNotEmpty(purchaseInvoiceDate) || isNotEmpty(paymentOrderNo) || isNotEmpty(start) || isNotEmpty(end) || isNotEmpty(projectName) || isNotEmpty(customer)
                || isNotEmpty(confirm)  || isNotEmpty(searchArea) || isNotEmpty(ssC) || isNotEmpty(id) || isNotEmpty(userId));
    }



}
