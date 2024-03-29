package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.ProjectStock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.ProjectStockService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class ProjectStockServiceImpl extends GeneralServiceImpl<ProjectStock> implements ProjectStockService {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired AreaService areaService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	@SystemServiceLog(description = "编辑项目库存信息")
	public void saveOrUpdate(ProjectStock stock, String areaId) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(areaId)) {
				Area area = this.areaService.findOneById(areaId, Area.class);
				stock.setArea(area);
			}
			if (Common.isNotEmpty(stock.getId())) {
				// update
				// 更新预计采购数量、预计采购单价
				// 更新实际采购单价
				ProjectStock ed = this.findOneById(stock.getId(), ProjectStock.class);
				// 预计采购量
				Integer projectedProcurementVolume = stock.getProjectedProcurementVolume();
				// 预计采购单价
				Double estimatedUnitPrice = stock.getEstimatedUnitPrice();
				// 预计采购总价
				Double projectedTotalPurchasePrice = 0.0;
				// 实际采购数量
				Integer actualPurchaseQuantity = ed.getActualPurchaseQuantity();
				// 实际成本单价
				Double realCostUnitPrice = stock.getRealCostUnitPrice();
				// 实际成本总价
				Double totalActualCost = 0.0;
				if (Common.isNotEmpty(projectedProcurementVolume) && Common.isNotEmpty(estimatedUnitPrice)) {
					projectedTotalPurchasePrice = projectedProcurementVolume * estimatedUnitPrice;
					stock.setProjectedTotalPurchasePrice(projectedTotalPurchasePrice);
				}
				if (Common.isNotEmpty(actualPurchaseQuantity) && Common.isNotEmpty(realCostUnitPrice)) {
					totalActualCost = actualPurchaseQuantity * realCostUnitPrice;
					stock.setTotalActualCost(totalActualCost);
				}
				stock.setInventory(ed.getInventory());
				stock.setNum(ed.getNum());
				stock.setActualPurchaseQuantity(ed.getActualPurchaseQuantity());
				BeanUtils.copyProperties(stock, ed);
				this.redisTemplate.delete("projectStockNames");
				this.save(stock);
			} else {
				if (stock.getNum() > 0) {
					stock.setInventory(stock.getActualPurchaseQuantity() - stock.getNum());
				}
				// insert
				Integer projectedProcurementVolume = stock.getProjectedProcurementVolume();
				Double estimatedUnitPrice = stock.getEstimatedUnitPrice();
				Double projectedTotalPurchasePrice = 0.0;
				Integer actualPurchaseQuantity = stock.getActualPurchaseQuantity();
				Double realCostUnitPrice = stock.getRealCostUnitPrice();
				Double totalActualCost = 0.0;
				if (Common.isNotEmpty(projectedProcurementVolume) && Common.isNotEmpty(estimatedUnitPrice)) {
					projectedTotalPurchasePrice = projectedProcurementVolume * estimatedUnitPrice;
					stock.setProjectedTotalPurchasePrice(projectedTotalPurchasePrice);
				}
				if (Common.isNotEmpty(actualPurchaseQuantity) && Common.isNotEmpty(realCostUnitPrice)) {
					totalActualCost = actualPurchaseQuantity * realCostUnitPrice;
					stock.setTotalActualCost(totalActualCost);
				}
				this.redisTemplate.delete("projectStockNames");
				this.insert(stock);
			}
		}
	}



	@Override
	@SystemServiceLog(description = "获取所有非禁用项目库存信息")
	public List<ProjectStock> findAllProjectStock(boolean isdisable, String areaId) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, ProjectStock.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除项目库存信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				ProjectStock de = this.findOneById(edid, ProjectStock.class);
				de.setIsDelete(true);
				this.redisTemplate.delete("projectStockNames");
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
	@SystemServiceLog(description = "分页查询项目库存信息")
	public Pagination<ProjectStock> findpagination(Integer pageNo, Integer pageSize, String search, String projectName,
			String searchArea) {
		// 分页查询数据
		Pagination<ProjectStock> pagination = null;
		try {
			Query query = new Query();
			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}
			query = this.findbySearch(search, projectName, query);
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.DESC, "inventory")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, ProjectStock.class);
			if (pagination == null)
				pagination = new Pagination<ProjectStock>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;
	}

	@SystemServiceLog(description = "查询项目库存信息")
	public Query findbySearch(String search, String projectName, Query query) {
		if (Common.isNotEmpty(search)) {
			List<Object> suppliersId = this.findSuppliersId(search);
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("supplier.$id").in(suppliersId),
					Criteria.where("name").regex(search), Criteria.where("projectName").regex(search),
					Criteria.where("model").regex(search), Criteria.where("scope").regex(search)));
		}
		if (Common.isNotEmpty(projectName)) {
			query.addCriteria(Criteria.where("projectName").is(projectName));
		}

		return query;
	}

	/**
	 * 模糊匹配查过供应商以及供应商设备信息Id
	 * 
	 * @param search
	 * @return
	 */
	@SystemServiceLog(description = "查询供应商信息")
	public List<Object> findSuppliersId(String search) {
		List<Object> list = new ArrayList<>();
		Query query = new Query();
		Criteria ca = new Criteria();
		query.addCriteria(ca.orOperator(Criteria.where("price").regex(search), Criteria.where("wechat").regex(search),
				Criteria.where("qq").regex(search), Criteria.where("afterSaleService").regex(search),
				Criteria.where("introducer").regex(search), Criteria.where("name").regex(search),
				Criteria.where("address").regex(search), Criteria.where("email").regex(search),
				Criteria.where("contact").regex(search), Criteria.where("contactNumber").regex(search)));

		query.addCriteria(Criteria.where("isDelete").is(false));
		List<Supplier> lists = this.supplierService.find(query, Supplier.class);
		for (Supplier li : lists) {
			list.add(new ObjectId(li.getId()));
		}
		return list;
	}

	@Override
	@SystemServiceLog(description = "根据名称查询项目库存信息")
	public BasicDataResult ajaxgetRepletes(String projectName, String name, String model) {
		if (Common.isNotEmpty(name)) {
			Query query = new Query();
			query.addCriteria(Criteria.where("name").is(name));
			query.addCriteria(Criteria.where("isDelete").is(false));
			ProjectStock stock = this.findOneByQuery(query, ProjectStock.class);
			return stock != null ? BasicDataResult.build(206, "当前供应商信息已经存在，请检查", null) : BasicDataResult.ok();
		}
		return BasicDataResult.build(400, "未能获取到请求的信息", null);
	}

	@Override
	@SystemServiceLog(description = "启用禁用项目库存信息")
	public BasicDataResult todisable(String id) {

		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		ProjectStock stock = this.findOneById(id, ProjectStock.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());

	}

	@SystemServiceLog(description = "批量导入项目库存信息")
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
			ProjectStock importProjectStock = new ProjectStock();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {
				ProjectStock stock = null; // 项目库存信息
				List<Supplier> supplier = null;
				Supplier supp = null;
				String areaName = resultexcel[i][j].trim();// 区域
				// 通过区域名称查询区域是否存在
				Area getarea = this.areaService.findByName(areaName);
				if (Common.isEmpty(getarea)) {
					error += "<span class='entypo-attention'></span>导入文件过程中，第<b>&nbsp&nbsp" + (i + 1)
							+ "行出现未添加的区域，请手动去修改该条信息或创建区域！&nbsp&nbsp</b></br>";
					return error;
				}

				importProjectStock.setArea(getarea);

				String projectName = resultexcel[i][j + 1].trim();// 项目名称
				if (Common.isEmpty(projectName)) {
					error += "<span class='entypo-attention'></span>导入文件过程中，第<b>&nbsp&nbsp" + (i + 1)
							+ "行出现项目名称为空，请手动去修改该条信息！&nbsp&nbsp</b></br>";
					return error;
				}
				importProjectStock.setProjectName(projectName);

				String name = resultexcel[i][j + 2].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importProjectStock.setName(name); // 设备名称

				String model = resultexcel[i][j + 3].trim();
				importProjectStock.setModel(model);// 设备型号

				importProjectStock.setScope(resultexcel[i][j + 4].trim());// 使用范围

				Integer projectedProcurementVolume = 0;
				Double estimatedUnitPrice = 0.0;
				try {
					projectedProcurementVolume = Integer.valueOf(resultexcel[i][j + 5]);// 预计采购量
				} catch (NumberFormatException e) {
					log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
					String aa = e.getLocalizedMessage();
					String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
					error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
							+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字";
					if ((i + 1) < rowLength) {
						continue;
					}

				}
				try {
					estimatedUnitPrice = Double.valueOf(resultexcel[i][j + 6]);// 预计采购单价
				} catch (NumberFormatException e) {
					log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
					String aa = e.getLocalizedMessage();
					String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
					error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
							+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字";
					if ((i + 1) < rowLength) {
						continue;
					}

				}

				importProjectStock.setProjectedProcurementVolume(projectedProcurementVolume);// 预计采购量
				importProjectStock.setEstimatedUnitPrice(estimatedUnitPrice);// 预计采购单价

				Double projectedTotalPurchasePrice = 0.0;
				if (Common.isNotEmpty(projectedProcurementVolume) && Common.isNotEmpty(estimatedUnitPrice)) {
					projectedTotalPurchasePrice = projectedProcurementVolume * estimatedUnitPrice;
				}
				importProjectStock.setProjectedTotalPurchasePrice(projectedTotalPurchasePrice);// 预计采购总价

				Integer actualPurchaseQuantity = 0; // 实际采购数量
				try {
					actualPurchaseQuantity = Integer.valueOf(resultexcel[i][j + 7]);
				} catch (NumberFormatException e) {
					log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
					String aa = e.getLocalizedMessage();
					String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
					error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
							+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字";
					if ((i + 1) < rowLength) {
						continue;
					}
				}
				importProjectStock.setActualPurchaseQuantity(actualPurchaseQuantity);// 实际采购数量

				Double realCostUnitPrice = 0.0; // 实际采购单价
				try {
					realCostUnitPrice = Double.valueOf(resultexcel[i][j + 8]);
				} catch (NumberFormatException e) {
					log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
					String aa = e.getLocalizedMessage();
					String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
					error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
							+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字";
					if ((i + 1) < rowLength) {
						continue;
					}
				}
				importProjectStock.setRealCostUnitPrice(realCostUnitPrice);// 实际成本单价
				Double totalActualCost = 0.0;
				if (Common.isNotEmpty(actualPurchaseQuantity) && Common.isNotEmpty(realCostUnitPrice)) {
					totalActualCost = actualPurchaseQuantity * realCostUnitPrice;
				}
				importProjectStock.setTotalActualCost(totalActualCost);// 计算实际成本总价

				importProjectStock.setPaymentTime(resultexcel[i][j + 9]);// 付款时间

				// long paymentAmount = 0;
				//

				String paymentAmount = resultexcel[i][j + 10];
				if (Common.isNotEmpty(paymentAmount)) {
					try {
						importProjectStock.setPaymentAmount(Double.valueOf(paymentAmount));// 付款金额
					} catch (NumberFormatException e) {
						log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" + e);
						String aa = e.getLocalizedMessage();
						String b = aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", "");
						error += "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1)
								+ "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字";
						if ((i + 1) < rowLength) {
							continue;
						}
					}

				}

				/*
				 * String nums = resultexcel[i][j + 9]; long num = 0; if
				 * (Common.isNotEmpty(nums)) { try { num = Long.valueOf(nums); } catch
				 * (NumberFormatException e) { log.debug("导入文件过程中出现错误第" + (i + 1) + "行出现错误" +
				 * e); String aa = e.getLocalizedMessage(); String b =
				 * aa.substring(aa.indexOf(":") + 1, aa.length()).replaceAll("\"", ""); error +=
				 * "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1) +
				 * "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp" + b + "&nbsp&nbsp</b></br> 请输入正确数字"; if
				 * ((i + 1) < rowLength) { continue; } } importProjectStock.setNum(num);// 出库数量
				 * 
				 * if (num > actualPurchaseQuantity) { error +=
				 * "<span class='entypo-attention'></span>导入文件过程中出现错误第<b>&nbsp&nbsp" + (i + 1) +
				 * "&nbsp&nbsp</b>行出现错误内容为<b>&nbsp&nbsp出库数量大于实际采购数量，无法完成出库，请检查&nbsp&nbsp</b></br>";
				 * continue; }
				 * 
				 * }
				 */

				String supplierName = resultexcel[i][j + 11].trim();// 供应商名称
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByRegxName(supplierName);
					if (supplier.size() == 0) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "行，请手动去修改该条信息！&nbsp&nbsp</b></br>";
						importProjectStock.setSupplier(null);
					} else if (supplier.size() > 1) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，匹配到多个类似供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "行，请手动去修改该条信息！&nbsp&nbsp</b></br>";
						importProjectStock.setSupplier(null);
					} else {
						supp = supplier.get(0);
						importProjectStock.setSupplier(supplier.get(0));
					}

				} else {
					importProjectStock.setSupplier(null);
				}

				stock = this.findByName(name, model, projectName, supp);
				if (Common.isNotEmpty(stock)) {
					// 设备已存在
					error += "<span class='entypo-attention'></span>导入文件过程中设备已经存在，设备名称<b>&nbsp;&nbsp;" + stock.getName()
							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1) + "行，请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				} else {
					// 添加
					this.insert(importProjectStock);
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
	@SystemServiceLog(description = "上传项目库存信息")
	public String upload(HttpServletRequest request, HttpSession session) {
		String error = "";
		try {
			Map<String, Object> map = new HashMap<String, Object>();
			// 别名
			String upname = File.separator + "FileUpload" + File.separator + "projectStock";

			// 可以上传的文件格式
			log.info("准备项目库存数据");
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
	 * 根据单位名称查找单位，如果没有则创建一个
	 */
	@Override
	public ProjectStock findByName(String name, String model, String projectName, Supplier supplier) {
		Query query = new Query();
		if (Common.isNotEmpty(supplier)) {
			query.addCriteria(Criteria.where("supplier.$id").is(new ObjectId(supplier.getId())));
		}
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("model").is(model));
		query.addCriteria(Criteria.where("projectName").is(projectName));
		query.addCriteria(Criteria.where("isDelete").is(false));
		ProjectStock stock = this.findOneByQuery(query, ProjectStock.class);
		/*
		 * if(Common.isEmpty(stock)){ ProjectStock ca = new ProjectStock();
		 * ca.setName(name); this.insert(ca); return ca; }
		 */
		return stock;
	}

	@SystemServiceLog(description = "根据id查询项目库存信息")
	public BasicDataResult findOneById(String id) {
		ProjectStock stock = this.findOneById(id, ProjectStock.class);

		return Common.isNotEmpty(stock) ? BasicDataResult.build(200, "查询成功", stock)
				: BasicDataResult.build(400, "查询失败", null);
	}

	@Override
	@SystemServiceLog(description = "导出项目库存信息")
	public HSSFWorkbook export(String name, String areaId) {
		HSSFWorkbook wb = new HSSFWorkbook();

		// 创建sheet
		HSSFSheet sheet = wb.createSheet(name);
		HSSFCellStyle style = createStyle(wb);
		List<String> title = this.title();
		this.createHead(sheet, title, style, name);
		this.createTitle(sheet, title, style);
		this.createProjectStock(sheet, title, style, areaId);
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
	public void createProjectStock(HSSFSheet sheet, List<String> title, HSSFCellStyle style, String areaId) {

		int j = 1;
		// 获取所有的项目库存
		List<ProjectStock> list = this.findAllProjectStock(false, areaId);
		for (ProjectStock stock : list) {
			// 获取所有的设备
			HSSFRow row = sheet.createRow(j + 1);

			HSSFCell cell = row.createCell(0);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getArea().getName());// 项目名称

			cell = row.createCell(1);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getProjectName());// 项目名称

			cell = row.createCell(2);
			cell.setCellStyle(style);
			cell.setCellValue(Common.isNotEmpty(stock.getSupplier()) ? stock.getSupplier().getName() : "");// 供应商

			cell = row.createCell(3);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getName());// 设备名称

			cell = row.createCell(4);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getModel());// 型号

			cell = row.createCell(5);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getScope()); // 使用范围

			cell = row.createCell(6);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getProjectedProcurementVolume());// 预计采购数量

			cell = row.createCell(7);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getEstimatedUnitPrice());// 预计采购单价

			cell = row.createCell(8);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getProjectedTotalPurchasePrice());// 预计总价

			cell = row.createCell(9);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getActualPurchaseQuantity());// 实际采购数量

			cell = row.createCell(10);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getRealCostUnitPrice());// 实际成本单价

			cell = row.createCell(11);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getTotalActualCost());// 实际成本总价

			cell = row.createCell(12);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getPaymentTime());// 付款时间

			cell = row.createCell(13);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getPaymentAmount());// 付款金额

			cell = row.createCell(14);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getInventory());// 剩余库存量

			cell = row.createCell(14);
			cell.setCellStyle(style);
			cell.setCellValue(stock.getNum());// 出库数量

			j++;
		}

	}

	/**
	 * 设置title
	 * 
	 * @return
	 */
	public List<String> title() {
		List<String> list = new ArrayList<>();
		list.add("区域");
		list.add("项目名称");
		list.add("供应商");
		list.add("设备名称");
		list.add("设备型号");
		list.add("使用范围");
		list.add("预计采购量");
		list.add("预计采购单价");
		list.add("预计采购总价");
		list.add("实际采购量");
		list.add("实际成本单价");
		list.add("实际成本总价");
		list.add("付款时间");
		list.add("付款金额");
		list.add("库存量");
		list.add("出库数量");
		return list;
	}

	@Override
	@SystemServiceLog(description = "查询低项目库存量信息")
	public List<ProjectStock> findLowProjectStock(int num) {

		Query query = new Query();
		if (num >= 0) {
			query.addCriteria(Criteria.where("inventory").lte(num));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.ASC, "inventory")));
			List<ProjectStock> list = this.find(query, ProjectStock.class);
			return list;
		}

		return null;
	}

	@Override
	public Set findProjectNames() {

		Set<String> getset = (Set<String>) this.redisTemplate.opsForValue().get("projectStockNames");

		if (Common.isNotEmpty(getset)) {
			return getset;
		}

		Query query = new Query();

		Set<String> set = new HashSet<>();

		query.addCriteria(Criteria.where("isDelete").is(false));

		List<ProjectStock> list = this.find(query, ProjectStock.class);

		list.forEach((projectStock) -> {
			set.add(projectStock.getProjectName());
		});

		this.redisTemplate.opsForValue().set("projectStockNames", set);

		return set;
	}

}
