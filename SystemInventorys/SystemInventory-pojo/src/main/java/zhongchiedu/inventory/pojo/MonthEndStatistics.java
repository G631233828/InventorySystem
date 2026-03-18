package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 月度库存统计
 * @author gjb
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MonthEndStatistics   extends GeneralBean<MonthEndStatistics>{/**
	 * 
	 */
	private static final long serialVersionUID = 610368787474380776L;
	
	@DBRef 
	private Stock stock;
	
	private Double monthEndStockNum;//月末库存数量
	
	private String stockName;//设备名称
	
	private String stockModel;//设备型号
	
	private String stockSuppier;//设备供应商
	
	private String stockArea;//设备区域
	
	private String date;//日期
	
	private String price;//记录当月库存设备的平均价格
	
	private String inprice;//记录每月单价
	

}
