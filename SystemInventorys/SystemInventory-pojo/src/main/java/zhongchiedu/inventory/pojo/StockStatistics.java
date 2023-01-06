package zhongchiedu.inventory.pojo;

import java.util.Date;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import zhongchiedu.framework.pojo.GeneralBean;
import zhongchiedu.general.pojo.User;


/**
 * 库存统计
 * @author fliay
 *
 */
//@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
//@ToString
public class StockStatistics extends GeneralBean<StockStatistics> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8545694298499828935L;
	
	
	@DBRef
	private Stock stock; //绑定库存商品
	private String storageTime;//入库时间
	private String depotTime;//出库时间
	private long num;//出库、入库数量
	private long newNum;//当前库存
	@DBRef
	private Companys company;//绑定企业
	@DBRef
	private User user;//操作人
	private boolean revoke; //是否可撤销
	private boolean inOrOut;//入库还是出库
	private long revokeNum;//撤销数量，默认为全部，撤销数量不能大于入库或者出库数量
	
	private String personInCharge;//责任人
	private String projectName;   //项目名
	private String customer;//客户
	
	//预入库方式  新增
	private boolean preStock = false;//预入库
	
	@DBRef
	private Area area;
	
	//添加代理商品标记
	private  boolean agent;//是否代理商品 false 非代理商品  true代理商品
	
	//出库单号
	private String outboundOrder;//出库单
	
	private String sign;//

	@DBRef
	private QrCode qrCode;//出库登记二维码
	
	private String openId;

	private String pickupTime;//取货时间
	
	@DBRef
	private Sign mysign;//微信签名
	
	//20221118 添加销售发票号
	private String sailesInvoiceNo;//销售发票号
	
	//20221123 添加出库入库总价
	private Double inprice;
	//private double outprice;
	private String purchaseInvoiceNo;//采购发票号
	private String receiptNo;//银行收款单号
	private String paymentOrderNo;//银行付款单号
	
	//20221201
	private String editFinanceTime;//修改 上面几项信息的时间
	@DBRef
	private User financeUser;
	//20221205 添加销售发票开具日期
	private String sailesInvoiceDate;
	
	
	
	
	

}
