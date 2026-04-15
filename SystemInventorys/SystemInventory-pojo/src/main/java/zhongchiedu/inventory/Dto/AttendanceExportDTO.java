package zhongchiedu.inventory.Dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class AttendanceExportDTO {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("签到学校")
    private String schoolName;
    
    @ExcelProperty("签到教室")
    private String classRoom;

    @ExcelProperty("签到时间")
    private String signTime;

    @ExcelProperty("现场问题反馈")
    private String problems;
    
    @ExcelProperty("定位")
    private String address;

}