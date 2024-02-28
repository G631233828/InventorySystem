package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import zhongchiedu.framework.pojo.GeneralBean;

import java.util.List;

/**
 * 项目管理
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Pname extends GeneralBean<Pname> {


    private String name;//名称
    private String itemid;//项目编号

    private String  assistant;//项目助理

    private String pm;//项目经理

    @DBRef
    private List<NewCustomer> customers;//多个客户
}
