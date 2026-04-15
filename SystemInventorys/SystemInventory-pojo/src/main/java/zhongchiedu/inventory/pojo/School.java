package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class School extends GeneralBean<School> {

    private static final long serialVersionUID = 1L;

    private String schoolName;    // 学校名称
    private String address;       // 学校地址
    private String contact;       // 联系人（可选）
    private String phone;         // 联系电话（可选）
    private String remark;        // 备注
}