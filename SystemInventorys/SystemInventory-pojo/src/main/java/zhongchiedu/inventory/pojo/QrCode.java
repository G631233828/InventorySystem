package zhongchiedu.inventory.pojo;

import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.MultiMedia;


/**
 * 库存商品二维码管理
 * @author fliay
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QrCode extends GeneralBean<QrCode>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3784611543983310865L;

	private String path;//二维码访问地址
	
	@DBRef
	private MultiMedia qrcode;
	
	private String name;
	
	private String type;
	
//	@DBRef
//	private Stock stock;
	
	
}
