package zhongchiedu.inventory.Dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.Date;

@Data
public class WxRepairExportDTO {
    @ExcelProperty("工单号")
    @ColumnWidth(20)
    private String workOrderNumber;

    @ExcelProperty("报修人")
    @ColumnWidth(10)
    private String userName;
    
    @ExcelProperty("报修人电话")
    @ColumnWidth(20)
    private String contactNumber;
    
    @ExcelProperty("报修学校")
    @ColumnWidth(25)
    private String schoolName;

    @ExcelProperty("报修校区")
    @ColumnWidth(15)
    private String campus;

    @ExcelProperty("报修地址")
    @ColumnWidth(30)
    private String schoolAddress;

    @ExcelProperty("报修教室")
    @ColumnWidth(20)
    private String reportClassroomRepair;

    @ExcelProperty("报修设备")
    @ColumnWidth(20)
    private String equipmentRepair;
    
    
    @ExcelProperty("故障信息")
    @ColumnWidth(20)
    private String faultInformation;

    @ExcelProperty("紧急程度")
    @ColumnWidth(12)
    private String urgencyLevel;

    @ExcelProperty("报修日期")
    @ColumnWidth(20)
    private String createTime;

    @ExcelProperty("期望完成时间")
    @ColumnWidth(20)
    private String expectedVisitTime;

    @ExcelProperty("维修状态")
    @ColumnWidth(12)
    private String statusDesc;

    @ExcelProperty("施工队人员")
    @ColumnWidth(20)
    private String workerName;
    
    @ExcelProperty("第一次打开时间")
    @ColumnWidth(20)
    private String findReadTime;//第一次打开时间
    
    

    @ExcelProperty("维修内容")
    @ColumnWidth(30)
    private String repairContent;
    
    @ExcelProperty("维修完成时间")
    @ColumnWidth(20)
    private String completeTime;
    
    @ExcelProperty("工单状态")
    @ColumnWidth(20)
    private String workDept;
    
    @ExcelProperty("维修单描述")
    @ColumnWidth(20)
    private String description;
    
    @ExcelProperty("用时")
    @ColumnWidth(20)
    private String useTime;
    
    
    
    

    
    
    
}