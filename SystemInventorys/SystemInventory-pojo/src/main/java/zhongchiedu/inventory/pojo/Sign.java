package zhongchiedu.inventory.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;


/**
 * 签名数据
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Sign extends GeneralBean<Sign>{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1353909445845705516L;
	private String sign;
	
	
	
}
