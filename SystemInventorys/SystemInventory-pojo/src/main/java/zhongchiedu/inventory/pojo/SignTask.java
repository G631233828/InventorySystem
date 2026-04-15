package zhongchiedu.inventory.pojo;

import lombok.Data;
import zhongchiedu.framework.pojo.GeneralBean;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "sign_task")
public class SignTask  extends GeneralBean<SignTask> {

    private String taskName;        // 签到任务名称（如：日常签到、维修签到、月度签到）
    private String startTime;       // 签到开始时间  格式：yyyy-MM-dd HH:mm
    private String endTime;         // 签到结束时间  格式：yyyy-MM-dd HH:mm
}