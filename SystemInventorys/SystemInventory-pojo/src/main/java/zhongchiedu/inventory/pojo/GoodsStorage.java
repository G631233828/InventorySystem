package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GoodsStorage extends GeneralBean<GoodsStorage> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 438213240034833925L;
	private String address;
	private String shelfNumber;
	private String selflevel;
	@DBRef
	private Companys companys;

}
