package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.MatrixToImageWriter;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.pojo.User;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.Category;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.PreStock;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.QrCode;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.InventoryRoleService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.PreStockService;
import zhongchiedu.inventory.service.QrCodeService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class StockServiceImpl extends GeneralServiceImpl<Stock> implements StockService {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired SystemClassificationServiceImpl systemClassificationService;

	private @Autowired CategoryServiceImpl categoryService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired BrandServiceImpl brandService;

	private @Autowired AreaService areaService;

	private @Autowired StockStatisticsService stockStatisticsService;

	private @Autowired PreStockService preStockService;

	private @Autowired InventoryRoleService inventoryRoleService;

	private @Autowired PickUpApplicationService pickUpApplicationService;

	@Autowired
	private MultiMediaService multiMediaService;

	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private QrCodeService qrCodeServce;

	@Override
	@SystemServiceLog(description = "编辑库存信息")
	public void saveOrUpdate(Stock stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				Stock ed = this.findOneById(stock.getId(), Stock.class);
				stock.setInventory(ed.getInventory());
				stock.setUpdateTime(new Date());
				BeanUtils.copyProperties(stock, ed);
				this.save(stock);
			} else {
				// insert
				stock.setUpdateTime(new Date());
				this.insert(stock);
			}
		}
	}

	@Override
	@SystemServiceLog(description = "获取所有非禁用库存信息")
	public List<Stock> findAllStock(boolean isdisable, String areaId,String searchAgent) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		
		if(Common.isNotEmpty(searchAgent)) {
			query = query.addCriteria(Criteria.where("agent").is(Boolean.valueOf(searchAgent)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		
		List<Stock> list = this.find(query, Stock.class);
		List<Stock> rlist = new ArrayList<Stock>();
		list.forEach(s->{
			Stock stock = new Stock();
			stock.setName(s.getName());
			stock.setModel(s.getModel());
			stock.setId(s.getId());
			stock.setScope(s.getScope());
			stock.setGoodsStorage(s.getGoodsStorage());
			stock.setUnit(s.getUnit());
			stock.setPrice(s.getPrice());
			stock.setInventory(s.getInventory());
			stock.setArea(s.getArea());
			stock.setAgent(s.isAgent());
			rlist.add(stock);
		});
		
		return rlist;
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除库存信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				Stock de = this.findOneById(edid, Stock.class);
				de.setIsDelete(true);
				this.save(de);
			}
			return "success";
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
		return "error";
	}

	@Override
	@SystemServiceLog(description = "分页查询库存信息")
	public Pagination<Stock> findpagination(Integer pageNo, Integer pageSize, String search, String searchArea,String searchAgent,String ssC) {
		// 分页查询数据
		Pagination<Stock> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}

			if (Common.isNotEmpty(ssC)) {
				query = query.addCriteria(Criteria.where("systemClassification.$id").is(new ObjectId(ssC)));
			}

			if(Common.isNotEmpty(searchAgent)) {
				query = query.addCriteria(Criteria.where("agent").is(Boolean.valueOf(searchAgent)));
			}

			if (Common.isNotEmpty(search)) {
				query = this.findbySearch(search, query);
			}
			query.addCriteria(Criteria.where("isDelete").is(false));
			 query.with(new Sort(new Order(Direction.DESC, "createTime")));
//			query.with(new Sort(new Order(Direction.DESC, "updateTime")));
//			query.with(new Sort(new Order(Direction.DESC, "inventory"))); 按库存量排序
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, Stock.class);
			if (pagination == null)
				pagination = new Pagination<Stock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@SystemServiceLog(description = "查询库存信息")
	public Query findbySearch(String search, Query query) {
		if (Common.isNotEmpty(search)) {
			 List<Object> systemClassification = this.findSystemClassificationIds(search);
			 List<Object> brand = this.findBrandIds(search);
			 List<Object> goodsStorage = this.findGoodsStorageIds(search);
			 List<Object> category = this.findCategoryIds(search);
			 List<Object> suppliersId = this.findSuppliersId(search, systemClassification,
			 category, brand);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(
					  Criteria.where("goodsStorage.$id").in(goodsStorage),
					  Criteria.where("supplier.$id").in(suppliersId),
					  Criteria.where("brand.$id").in(brand),
					  Criteria.where("name").regex(search, "i"),
					Criteria.where("model").regex(search, "i"), Criteria.where("scope").regex(search),
					Criteria.where("entryName").regex(search),Criteria.where("itemNo").regex(search),
					Criteria.where("projectLeader").regex(search))
					);
		}

		return query;

	}

	/**
	 * 模糊匹配系统分类的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询系统分类信息")
	public List<Object> findSystemClassificationIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<SystemClassification> lists = this.systemClassificationService.find(query, SystemClassification.class);
		for (SystemClassification li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;

	}

	/**
	 * 模糊匹配类目的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询品牌信息")
	public List<Object> findBrandIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Brand> lists = this.brandService.find(query, Brand.class);
		for (Brand li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配货架号的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询货架信息")
	public List<Object> findGoodsStorageIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		if (search.contains("/")) {
			query.addCriteria(ca.orOperator(Criteria.where("address").regex(search),
					Criteria.where("shelfNumber").is(search.split("/")[0]),
					Criteria.where("shelflevel").is(search.split("/")[1])));
		} else {
			query.addCriteria(ca.orOperator(Criteria.where("address").regex(search),
					Criteria.where("shelfNumber").regex(search), Criteria.where("shelflevel").regex(search)));
		}

		query.addCriteria(Criteria.where("isDelete").is(false));
		List<GoodsStorage> lists = this.goodsStorageService.find(query, GoodsStorage.class);
		for (GoodsStorage li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配品牌的Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询类目信息")
	public List<Object> findCategoryIds(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		query.addCriteria(Criteria.where("name").regex(search));
		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Category> lists = this.categoryService.find(query, Category.class);
		for (Category li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	/**
	 * 模糊匹配查过供应商以及供应商设备信息Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询供应商信息")
	public List<Object> findSuppliersId(String search, List<Object> systemClassification, List<Object> categorys,
			List<Object> brands) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("price").regex(search), Criteria.where("wechat").regex(search),
				Criteria.where("qq").regex(search), Criteria.where("afterSaleService").regex(search),
				Criteria.where("introducer").regex(search), Criteria.where("name").regex(search),
				Criteria.where("address").regex(search), Criteria.where("email").regex(search),
				Criteria.where("contact").regex(search), Criteria.where("contactNumber").regex(search),
				Criteria.where("systemClassification.$id").in(systemClassification),
				Criteria.where("categorys.$id").in(categorys), Criteria.where("brand.$id").in(brands)));

		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Supplier> lists = this.supplierService.find(query, Supplier.class);
		for (Supplier li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@Override
	@SystemServiceLog(description = "根据名称查询库存信息")
	public BasicDataResult ajaxgetRepletes(String name, String areaId, String model) {
		Query query = new Query();

		if (Common.isNotEmpty(name)) {
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("name").is(name));
			if (Common.isNotEmpty(areaId)) {
				query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
			}
			if (Common.isNotEmpty(model)) {
				query.addCriteria(Criteria.where("model").is(model));
			}
			Stock stock = this.findOneByQuery(query, Stock.class);
			return stock != null ? BasicDataResult.build(206, "当前供应商信息已经存在，请检查", null) : BasicDataResult.ok();
		}
		return BasicDataResult.ok();
	}

	@Override
	@SystemServiceLog(description = "启用禁用库存信息")
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		Stock stock = this.findOneById(id, Stock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());

	}

	@SystemServiceLog(description = "批量导入库存信息")
	public String BatchImport(File file, int row, HttpSession session) {
		String error = "";
		String[][] resultexcel = null;
		try {
			resultexcel = ExcelReadUtil.readExcel(file, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int rowLength = resultexcel.length;
		ProcessInfo pri = new ProcessInfo();
		pri.allnum = rowLength;
		for (int i = 1; i < rowLength; i++) {
			Query query = new Query();
			Stock importStock = new Stock();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

				Stock stock = null; // 库存信息
				Supplier supplier = null;
				Unit unit = null;
				Brand brand = null;
				SystemClassification ssC=null;
				String areaName = resultexcel[i][j].trim();// 区域名称
				// 通过区域名称查询区域是否存在
				Area getarea = this.areaService.findByName(areaName);
				if (Common.isEmpty(getarea)) {
					error += "<span class='entypo-attention'></span>导入文件过程中，第<b>&nbsp&nbsp" + (i + 1)
							+ "行出现未添加的区域，请手动去修改该条信息或创建区域！&nbsp&nbsp</b></br>";
					return error;
				}
				importStock.setArea(getarea);
				String name = resultexcel[i][j + 1].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				
				importStock.setName(name.replaceAll("/", "^")); // 设备名称
				
				
				String model = resultexcel[i][j + 2].trim();
				if (Common.isEmpty(model)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备型号为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importStock.setModel(model);// 设备型号
				
				
				String brandname= resultexcel[i][j + 3].trim();
				
				if (Common.isNotEmpty(brandname)) {
					// 根据供应商名称查找，看供应商是否存在
					brand = this.brandService.findByName(brandname);
					if (Common.isEmpty(brand)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的品牌<b>&nbsp;&nbsp;" + brandname
								+ "&nbsp;&nbsp;</b>，<b>&nbsp&nbsp" + (i + 1)
								+ "已经自动创建该品牌，如有问题 请手动修改！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				
				importStock.setBrand(brand);
				
			
				
				//新添加了入库数量
				importStock.setStocknum(Long.valueOf(resultexcel[i][j + 4].trim()));
				
				String n = resultexcel[i][j + 5].trim();
				boolean num = Common.isInteger(n);
				if(num) {
					importStock.setPrice(n);// 价格
				}else {
					error += "<span class='entypo-attention'></span>导入文件过程中出现不合法的金额<b>&nbsp;&nbsp;" + n
							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				

				String unitName = resultexcel[i][j + 6].trim();
				if (Common.isNotEmpty(unitName)) {
					// 根据供应商名称查找，看供应商是否存在
					unit = this.unitService.findByName(unitName);
				}
				importStock.setUnit(unit);
				String entryName = resultexcel[i][j + 7].trim();
				importStock.setEntryName(entryName);// 项目名称
				importStock.setItemNo(resultexcel[i][j + 8].trim());// 项目编号
				String supplierName = resultexcel[i][j + 9].trim();// 供应商名称
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByName(supplierName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importStock.setSupplier(supplier);
				//新添加 是否代理商品
				String agent =  resultexcel[i][j + 10].trim();// 获取是否代理商品
				importStock.setAgent(agent=="是");

				//添加系统分类
				String ssCName = resultexcel[i][j + 11].trim();// 系统分类
				if (Common.isNotEmpty(ssCName)) {
					// 根据供应商名称查找，看供应商是否存在
					ssC = this.systemClassificationService.findByName(ssCName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的系统分类<b>&nbsp;&nbsp;" + ssCName
								+ "&nbsp;&nbsp;</b>，请先添加系统分类，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importStock.setSystemClassification(ssC);

				stock = this.findByName(areaName,name, model, entryName);
				
				 StockStatistics stockStatistics = new StockStatistics();//
				 stockStatistics.setNum(importStock.getStocknum());//
			        stockStatistics.setInOrOut(true);//true为入库//
				if (Common.isNotEmpty(stock)) {
					//对于已经存在设备执行入库操作
					 stockStatistics.setStock(stock);//
					// 设备已存在
					error += "<span class='entypo-attention'></span>导入文件过程中第<b>&nbsp&nbsp" + (i + 1) + "设备已经存在，设备名称<b>&nbsp;&nbsp;" + stock.getName()
							+ "&nbsp;&nbsp;</b>，入库数量为"+importStock.getStocknum()+"已经完成入库！&nbsp&nbsp</b></br>";
					//continue;
				} else {
					// 添加新设备
					importStock.setUpdateTime(new Date());
					this.insert(importStock);
//					//设备添加完成之后执行入库操作 获取入库数据如果>0
//					if(importStock.getStocknum()>0) {
//						//读取session中的用户    
//				        User user = (User) session.getAttribute(Contents.USER_SESSION);  
//				        StockStatistics stockStatistics = new StockStatistics();
//				        stockStatistics.setNum(importStock.getStocknum());
//				        stockStatistics.setInOrOut(true);//true为入库
//				        stockStatistics.setStock(importStock);
//				        this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
//					}
				        stockStatistics.setStock(importStock);//
				}
				//设备添加完成之后执行入库操作 获取入库数据如果>0
				if(importStock.getStocknum()>0) {
					//读取session中的用户    
			        User user = (User) session.getAttribute(Contents.USER_SESSION);  
			       
			        this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
				}
				
				
				
				

				// 捕捉批量导入过程中遇到的错误，记录错误行数继续执行下去
			} catch (Exception e) {
				log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
				String aa = e.getLocalizedMessage();
				String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
				error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
						+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br>";
				if ((i + 1) < rowLength) {
					continue;
				}

			}
		}
		log.info(error);
		return error;
	}
	


	/**
	 * 执行上传文件，返回错误消息
	 */
	@SystemServiceLog(description = "上传库存信息")
	public String upload(HttpServletRequest request, HttpSession session) {
		String error = "";
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			// 别名
			String upname = File.separator + "FileUpload" + File.separator + "category";

			// 可以上传的文件格式
			log.info("准备上传类目数据");
			String filetype[] = { "xls,xlsx" };
			List<Map<String, Object>> result = FileOperateUtil.upload(request, upname, filetype);
			log.info("上传文件成功");
			boolean has = (Boolean) result.get(0).get("hassuffix");

			if (has != false) {
				// 获得上传的xls文件路径
				String path = (String) result.get(0).get("savepath");
				File file = new File(path);
				// 知道导入返回导入结果
				error = this.BatchImport(file, 1, session);
			}
		} catch (Exception e) {
			return e.toString();
		}
		return error;

	}

	/**
	 * 上传进度
	 */
	@Override
	public ProcessInfo findproInfo(HttpServletRequest request) {

		return (ProcessInfo) request.getSession().getAttribute("proInfo");

	}


	/**
	 *   查找区域信息，设备名称，型号是否有冲突
	 */

	@Override
	public Stock findByName(String areaName,String name,String model,String entryName){
		Area area = this.areaService.findByName(areaName);

		Query query = new Query();
		if (Common.isNotEmpty(area.getId())) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(area.getId())));
		}


		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("model").is(model));

		if (Common.isNotEmpty(entryName)) {
			query.addCriteria(Criteria.where("entryName").is(entryName));
		}

		query.addCriteria(Criteria.where("isDelete").is(false));
		Stock stock = this.findOneByQuery(query, Stock.class);
		/*
		 * if(Common.isEmpty(stock)){ Stock ca = new Stock(); ca.setName(name);
		 * this.insert(ca); return ca; }
		 */
		return stock;
	}


	/**
	 * 根据区域，设备名称、型号，供应商
	 */
	@Override
	public Stock findByNameSupplier(String areaName,String name, String model,String supplierName) {
		//获取areaid信息
		Area area = this.areaService.findByName(areaName);
		Supplier supplier=this.supplierService.findByName(supplierName);
		Query query = new Query();
		if (Common.isNotEmpty(area.getId())) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(area.getId())));
		}
		if(Common.isNotEmpty(supplier.getId())){
			query.addCriteria(Criteria.where("supplier.$id").is(new ObjectId(supplier.getId())));
		}

		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("model").is(model));

		query.addCriteria(Criteria.where("isDelete").is(false));
		Stock stock = this.findOneByQuery(query, Stock.class);
		/*
		 * if(Common.isEmpty(stock)){ Stock ca = new Stock(); ca.setName(name);
		 * this.insert(ca); return ca; }
		 */
		return stock;
	}

	@SystemServiceLog(description = "根据id查询库存信息")
	public BasicDataResult findOneById(String id) {
		Stock getstock = new Stock();
		Stock stock = this.findOneById(id, Stock.class);
		getstock.setName(stock.getName());
		getstock.setInventory(stock.getInventory());
		getstock.setDescription(stock.getDescription());
		getstock.setId(stock.getId());
		return Common.isNotEmpty(getstock) ? BasicDataResult.build(200, "查询成功", getstock)
				: BasicDataResult.build(400, "查询失败", null);
	}

	@Override
	@SystemServiceLog(description = "导出库存信息")
	public HSSFWorkbook export(String name, String areaId,String searchAgent) {
		HSSFWorkbook wb = new HSSFWorkbook();

		// 创建sheet
		HSSFSheet sheet = wb.createSheet(name);
		HSSFCellStyle style = createStyle(wb);
		List<String> title = this.title(true);
		this.createHead(sheet, title, style, name);
		this.createTitle(sheet, title, style);
		this.createStock(sheet, title, style, areaId,searchAgent);
		sheet.setDefaultColumnWidth(12);
		sheet.autoSizeColumn(1, true);
		return wb;
	}

	@Override
	@SystemServiceLog(description = "导出库存量总和表")
	public HSSFWorkbook exportTJ(String name, String areaId,String searchAgent) {
		HSSFWorkbook wb = new HSSFWorkbook();
		// 创建sheet
		HSSFSheet sheet = wb.createSheet(name);
		HSSFCellStyle style = createStyle(wb);
		List<String> title = this.title(false);
		this.createHead(sheet, title, style, name);
		this.createTitle(sheet, title, style);
		this.createStockTJ(sheet, title, style, areaId,searchAgent);
		sheet.setDefaultColumnWidth(12);
		sheet.autoSizeColumn(1, true);
		return wb;
	}





	/**
	 * 创建样式
	 * 
	 * @param wb
	 * @return
	 */
	public HSSFCellStyle createStyle(HSSFWorkbook wb) {

		HSSFCellStyle style = wb.createCellStyle();
		// 设置边框
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		HSSFFont font = wb.createFont();
		font.setFontName("宋体");
		font.setFontHeightInPoints((short) 9);
		style.setFont(font);
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 水平布局：居
		return style;

	}

	/**
	 * 创建第一行
	 * 
	 * @param sheet
	 */
	public void createHead(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String name) {
		HSSFRow row = sheet.createRow(0);// 初始化excel第一行
		HSSFCell cell = row.createCell(0);
		cell.setCellValue(name);
		cell.setCellStyle(style);
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, title.size() - 1));
		for (int a = 1; a < title.size(); a++) {
			cell = row.createCell(a);
			cell.setCellStyle(style);
		}
	}

	/**
	 * 创建第二行 创建所有的title
	 * 
	 * @param sheet
	 */
	public void createTitle(HSSFSheet sheet, List<String> title, HSSFCellStyle style) {
		HSSFRow row = sheet.createRow(1);
		for (int a = 0; a < title.size(); a++) {
			HSSFCell cell = row.createCell(a);
			cell.setCellValue(title.get(a));
			cell.setCellStyle(style);
		}
	}

	/**
	 * 创建第三行 创建数据
	 * 
	 * @param sheet
	 */
	public void createStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String areaId,String searchAgent) {

		int j = 1;
		// 获取所有的库存
		List<Stock> list = this.findAllStock(false, areaId,searchAgent);

		for (Stock stock : list) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);

			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			if(stock.getArea()!=null) {
				cell.setCellValue(stock.getArea().getName());
			}else {
				cell.setCellValue("");
				
			}
			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getId());

			cell = row.createCell(1+1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());

			cell = row.createCell(2+1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());

			cell = row.createCell(3+1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getScope());

			cell = row.createCell(4+1);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getGoodsStorage())) {
				String level = "";
				if (Common.isEmpty(stock.getGoodsStorage().getShelflevel())) {
					level = "/" + stock.getGoodsStorage().getShelflevel();
				}
				cell.setCellValue(stock.getGoodsStorage().getShelfNumber() + level);
			} else {
				cell.setCellValue("-/-");

			}
			cell = row.createCell(5+1);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getUnit())) {
				cell.setCellValue(stock.getUnit().getName());
			} else {
				cell.setCellValue("");
			}

			cell = row.createCell(6+1);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getPrice())) {
				cell.setCellValue(stock.getPrice());
			} else {
				cell.setCellValue("");
			}

			cell = row.createCell(7+1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getInventory());

			cell = row.createCell(8+1);
			cell.setCellStyle(style);
			if (Common.isNotEmpty(stock.getInventory()) && Common.isNotEmpty(stock.getPrice())) {
				cell.setCellValue(stock.getInventory() * Double.valueOf(stock.getPrice()));
			} else {
				cell.setCellValue("");
			}

			
			cell = row.createCell(9+1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.isAgent()==true?"是":"否");

			j++;
		}

	}


	public void createStockTJ(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String areaId,String searchAgent) {

		int j = 1;
		// 获取所有的库存
		List<Stock> list = this.findAllStock(false, areaId,searchAgent);

		Map<String, List<Stock>> stocks=list.stream().collect(Collectors.groupingBy(stock -> fetchGroupKey(stock)));

		for(String key:stocks.keySet()){
			//区域，设备名称和型号相同的库存量总和
			if(Common.isNotEmpty(key)){
			Long sums=stocks.get(key).stream().mapToLong(Stock::getInventory).sum();
            String[] ss=key.split("_");
			String area=ss[0];
			String name=ss[1];
			String model=Common.isNotEmpty(ss[2])?ss[2]:"";

			String unit=Common.isNotEmpty(stocks.get(key).get(0).getUnit())?stocks.get(key).get(0).getUnit().getName():"单位空";


			HSSFRow row = sheet.createRow(j + 1);

			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			cell.setCellValue(area);

			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(name);


			cell = row.createCell(2);
			cell.setCellStyle(style);
			cell.setCellValue(model);


			cell = row.createCell(3);
			cell.setCellStyle(style);
			cell.setCellValue(unit);


			cell = row.createCell(4);
			cell.setCellStyle(style);
			cell.setCellValue(sums);

			j++;
			}
		}


	}



	/**
	 * 根据区域，设备名称和设备型号分组
	 * @param stock
	 * @return
	 */
	private String fetchGroupKey(Stock stock){
		String areaname=Common.isNotEmpty(stock.getArea().getName())?stock.getArea().getName():"区域空";
		String name=Common.isNotEmpty(stock.getName())?stock.getName():"设备名为空";
		String model=Common.isNotEmpty(stock.getModel())?stock.getModel():"设备型号为空";
		return areaname+"_"+name+"_"+model;
	}


	/**
	 * 设置title
	 * 
	 * @return
	 */
	public List<String> title(boolean a) {
		List<String> list = new ArrayList<>();
		if(a){
			list.add("区域");
			list.add("物料代码(*)");
			list.add("设备名称");
			list.add("设备型号");
			list.add("使用范围");
			list.add("货架号/层");
			list.add("计量单位 ");
			list.add("单价");
			list.add("当前库存");
			list.add("总价");
			list.add("代理商品");
		}else{
			list.add("区域");
			list.add("设备名称");
			list.add("设备型号");
			list.add("计量单位");
			list.add("当前库存");
		}
		return list;
	}



	@Override
	@SystemServiceLog(description = "查询低库存量信息")
	public List<Stock> findLowStock(int num) {

		Query query = new Query();
		if (num >= 0) {
			query.addCriteria(Criteria.where("inventory").lte(num));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.ASC, "inventory")));
			List<Stock> list = this.find(query, Stock.class);
			return list;
		}

		return null;
	}

	@Override
	public Set<String> findProjectNames() {
		Set<String> projects = new HashSet<>();

		Set<String> set = (Set<String>) this.redisTemplate.opsForValue().get("projectNames");
		if (Common.isNotEmpty(set)) {
			return set;
		}
		List<StockStatistics> findAllStockStatics = this.stockStatisticsService.findAllStockStatics();
		for (StockStatistics stocks : findAllStockStatics) {
			if (Common.isNotEmpty(stocks.getProjectName())) {
				projects.add(stocks.getProjectName());
			}
		}
		this.redisTemplate.opsForValue().set("projectNames", projects);
		return projects;
	}

	/***
	 * 预库存入库
	 * @param preStock  预库存参数
	 * @param actnum    预库存的实际已经入库的数量
	 */
	@Override
	public void preStockToStock(PreStock preStock,long actnum) {
		String areaId = preStock.getArea().getId();
		String name = preStock.getName();
		String model = preStock.getModel();
		String entryName= preStock.getEntryName();
		// 1.通过区域、设备名称、设备型号,和项目名称 判断设备是否在库存中已经存在
		StockStatistics stockStatistics = new StockStatistics();
		// 字段绑定
		if (Common.isNotEmpty(preStock)) {
			stockStatistics.setUser(preStock.getHandler());// 操作人
			stockStatistics.setPreStock(true);// 预入库方式入库
			stockStatistics.setInOrOut(true);
			stockStatistics.setPreStockId(preStock.getId());
			stockStatistics.setNum(preStock.getActualReceiptQuantity());// 设置实际入库数量

			if(Common.isNotEmpty(preStock.getInprice())){
				stockStatistics.setInprice(preStock.getInprice());//入库总金额
			}
			if(Common.isNotEmpty(preStock.getPurchaseInvoiceNo())){
				stockStatistics.setPurchaseInvoiceNo(preStock.getPurchaseInvoiceNo());//采购发票号
			}

			if(Common.isNotEmpty(preStock.getPaymentOrderNo())){
				stockStatistics.setPaymentOrderNo(preStock.getPaymentOrderNo());//银行付款日期
			}
			if(Common.isNotEmpty(preStock.getPurchaseInvoiceDate())){
				stockStatistics.setPurchaseInvoiceDate(preStock.getPurchaseInvoiceDate());//采购发票到票时间
			}
			if(Common.isNotEmpty(preStock.getItemNo())){
				stockStatistics.setNewItemNo(preStock.getItemNo());//采购付款单编码（新）
			}
			if(Common.isNotEmpty(preStock.getDescription())){
				stockStatistics.setDescription(preStock.getDescription());//备注
			}
		}

		Stock stock = this.findByAreaNameModel(areaId, name, model,entryName);

		if(Common.isNotEmpty(preStock.getSystemClassification()) && Common.isEmpty(stock.getSystemClassification())
		&& Common.isNotEmpty(stock)){
			stock.setSystemClassification(preStock.getSystemClassification());
			this.saveOrUpdate(stock);
		}//这一步是将库存管理中 分类没设置的库存  设置  为预库存的  分类。

		// 2.如果已经存在 执行入库操作添加库存数量
		if (Common.isNotEmpty(stock)) {
			stockStatistics.setStock(stock);
			// 調用库存统计模块进行入库操作
			this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, preStock.getHandler());
		} else {
			// 3.如果不存在则执行新商品入库
			stock = new Stock();
			stock.setArea(preStock.getArea());
			stock.setName(preStock.getName());
			stock.setModel(preStock.getModel());
			stock.setSystemClassification(preStock.getSystemClassification());
			//stock.setScope(preStock.getScope());
			stock.setGoodsStorage(preStock.getGoodsStorage());
			stock.setPrice(preStock.getPrice());
			stock.setUnit(preStock.getUnit());
			//stock.setMaintenance(preStock.getMaintenance());
			stock.setEntryName(preStock.getEntryName());
			stock.setItemNo(preStock.getItemNo());
			stock.setSupplier(preStock.getSupplier());
			stock.setType("1");// 1电脑端 2手机端 添加
			stock.setPublisher(preStock.getPublisher());
			this.saveOrUpdate(stock);
			// 创建完库存之后在执行入库操作
			stockStatistics.setStock(stock);
			this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, preStock.getHandler());
		}

		// 修改预库存状态
		// 预入库
		long estimatedInventoryQuantity = preStock.getEstimatedInventoryQuantity();
		// 实际入库=之前入库的加现在入库的
		long actualReceiptQuantity =actnum + preStock.getActualReceiptQuantity();

//		if (estimatedInventoryQuantity >= actualReceiptQuantity) {
//			preStock.setStatus(2);
//		} else if (estimatedInventoryQuantity > actualReceiptQuantity) {
//			// 预入库>大于实际入库
//			preStock.setStatus(3);// 部分入库
//		} else if (estimatedInventoryQuantity < actualReceiptQuantity) {
//			// 实际入库>预入库
//			preStock.setStatus(4);// 超量入库
//		}
		if(estimatedInventoryQuantity-actualReceiptQuantity == 0) {
			preStock.setStatus(2);
		}
		preStock.setActualReceiptQuantity(actualReceiptQuantity);
		this.preStockService.saveOrUpdate(preStock);
	}

	@Override
	public Stock findByAreaNameModel(String areaId, String name, String model,String entryName) {
		Query query = new Query();

		if (Common.isNotEmpty(name)) {
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("name").is(name));
			if (Common.isNotEmpty(areaId)) {
				query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
			}
			if (Common.isNotEmpty(model)) {
				query.addCriteria(Criteria.where("model").is(model));
			}
			if(Common.isNotEmpty(entryName)) {
				query.addCriteria(Criteria.where("entryName").is(entryName));
			}
			return this.findOneByQuery(query, Stock.class);
		}
		return null;

	}

	@Override
	public BasicDataResult pickUpApplicationToStock(PickUpApplication pickUpApplication) {
		Stock stock = this.findOneById(pickUpApplication.getStock().getId(), Stock.class);
		if (stock==null) {
			return new BasicDataResult().build(400, "未能找到库存商品，出库失败！", null);
		}
		long inventory = stock.getInventory();//获取库存数量
		if(inventory<=0) {
			return new BasicDataResult().build(400, "库存数量不足", null);
		}
		long actualIssueQuantity = pickUpApplication.getActualIssueQuantity();
		if(actualIssueQuantity>inventory) {
			return new BasicDataResult().build(400, "库存数量不足,剩余库存:"+inventory, null);
		}
		
		
		
		// 1.通过区域、设备名称、设备型号判断设备是否在库存中已经存在
		StockStatistics stockStatistics = new StockStatistics();
		// 字段绑定
		if (Common.isNotEmpty(pickUpApplication)) {
			stockStatistics.setUser(pickUpApplication.getHandler());// 操作人
			stockStatistics.setInOrOut(false);
			stockStatistics.setNum(pickUpApplication.getActualIssueQuantity());// 设置实际出库数量
			stockStatistics.setProjectName(pickUpApplication.getProjectName());
			stockStatistics.setPersonInCharge(pickUpApplication.getPersonInCharge());
			stockStatistics.setCustomer(pickUpApplication.getCustomer());
			stockStatistics.setDescription(pickUpApplication.getDescription());
			stockStatistics.setStock(stock);
		}
		// 出库完成修改出库状态
		pickUpApplication.setStatus(2);
		this.pickUpApplicationService.saveOrUpdate(pickUpApplication);

		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, pickUpApplication.getHandler());

	}

	@Value("${qrcode.weburl}")
	private String weburl;
	@Value("${qrcode.height}")
	private int height;
	@Value("${qrcode.width}")
	private int width;
	@Value("${upload.savedir}")
	private String dir;
	@Value("${qrcode.qrcodepath}")
	private String qrcodepath;
	@Value("${qrcode.format}")
	private String format;

	@Override
	public QrCode createStockQrCode(String stockId) {
		Stock stock = null;
		if (Common.isNotEmpty(stockId)) {
			stock = this.findOneById(stockId, Stock.class);
			if(stock.getQrCode()!=null) {
				//判断二维码是否存在，不存在则重新创建
				String downLoadPath = stock.getQrCode().getQrcode().getDir()
						+ stock.getQrCode().getQrcode().getSavePath()
						+ stock.getQrCode().getQrcode().getOriginalName();
				File f = new File(downLoadPath);
				if(!f.exists()) {
					stock.setQrCode(null);
				}
			}
			
		}
		if (stock == null) {
			return null;
		}
		
		QrCode qrcode = null;
		if(stock.getQrCode()==null) {
			qrcode = new QrCode();
			try {
				Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
				hints.put(EncodeHintType.CHARACTER_SET, "utf-8"); // 内容所使用字符集编码
				String urlpath = "wechat/cargoFromStorage/"+ stockId;
				String url = weburl + urlpath;
				BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hints);
				// 生成二维码
				String path = dir + qrcodepath + File.separator;
				Common.checkPathAndMkdirs(path);
				String fn = (stock.getName()+stock.getModel()).replace("\\", "").replace("/", "").replace("*", "x").replace(":", "").replace("\"", "").replace("|", "").replace("<", "").replace(">", "");
				File outputFile = new File(path +fn + stock.getId() + ".png");
				MatrixToImageWriter.writeToFile(bitMatrix, format, outputFile);
				// 保存图片信息
				MultiMedia saveQrCode = this.multiMediaService.saveQrCode(outputFile, dir, qrcodepath, "PHOTO");
				qrcode.setQrcode(saveQrCode);
				qrcode.setPath(urlpath);
				qrcode.setStock(stock);
				qrcode.setName(stock.getName());
				qrcode.setType("STOCK");
				this.qrCodeServce.insert(qrcode);
				stock.setQrCode(qrcode);
				this.save(stock);
			} catch (WriterException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return stock.getQrCode();
	}

	@Override
	public List<Stock> findStockByIds(String id) {
		
		
		Query query = new Query();
		String[] split = id.split(",");
		List<String> array = Arrays.asList(split);
		array.forEach(s->{
			this.createStockQrCode(s);
		});	
		
		query.addCriteria(Criteria.where("_id").in(array));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		return this.find(query, Stock.class);

	}

	@Override
	public List<Stock> findStocksByIds(List ids) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").in(ids));
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		return this.find(query, Stock.class);
	}

	@Override
	public void copyStock(String id,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		
		Stock ed = this.findOneById(id, Stock.class);
	
		if(ed!=null) {
			ed.setCreateDate(new Date().toString());
			ed.setCreateTime(new Date());
			ed.setId(null);
			ed.setQrCode(null);
			ed.setInventory(0);
			ed.setUpdateTime(new Date());
			ed.setPublisher(user);// 发布人
			Stock stock = new Stock();
			BeanUtils.copyProperties(ed, stock);
			this.insert(stock);
		}
		
	}

	@Override
	public void updateItemNo(String ids, String itemNo) {
		List<String> array = Arrays.asList(ids.split(","));
		
		for(String id:array) {
			Stock stock = this.findOneById(id, Stock.class);
			stock.setItemNo(itemNo);
			this.save(stock);
		}
		

		
		
	}

	@Override
	public List<Stock> findStockByType(String type) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		if("1".equals(type)) {
			//获取所有二维码
		}else if("2".equals(type)) {
			query.addCriteria(Criteria.where("inventory").gt(0));
		}
		
		List<Stock> list = this.find(query, Stock.class);

		list.forEach(s->{
			this.createStockQrCode(s.getId());
		});	
		
		return list;
		
		
	}

	
	

}
