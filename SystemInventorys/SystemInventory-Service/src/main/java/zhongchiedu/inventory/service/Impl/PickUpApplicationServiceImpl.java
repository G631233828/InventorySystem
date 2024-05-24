package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.NewCustomer;
import zhongchiedu.inventory.pojo.PickUpApplication;
import zhongchiedu.inventory.pojo.Pname;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.SystemClassification;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.NewCustomerService;
import zhongchiedu.inventory.service.PickUpApplicationService;
import zhongchiedu.inventory.service.PnameService;
import zhongchiedu.inventory.service.StockService;
import zhongchiedu.inventory.service.SupplierService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j

public class PickUpApplicationServiceImpl extends GeneralServiceImpl<PickUpApplication>
		implements PickUpApplicationService {

	private @Autowired AreaService areaService;
	@Lazy
	private @Autowired StockService stockService;
	private @Autowired SupplierService supplierService;
	private @Autowired NewCustomerService newCustomerService;
	private @Autowired PnameService pnameService;

	@Override
	@SystemServiceLog(description = "获取所有待出库信息")
	public Pagination<PickUpApplication> findpagination(Integer pageNo, Integer pageSize,
			String searchArea, String status,String pnameid,String customerid,String stockid,String modelid) {
		// 分页查询数据
		Pagination<PickUpApplication> pagination = null;
		try {
			Query query = new Query();

			if (Common.isNotEmpty(searchArea)) {
				query = query.addCriteria(Criteria.where("area.$id").is(new ObjectId(searchArea)));
			}
			if (Common.isEmpty(status)||status.equals("0")) {
				List<Integer> l = new ArrayList();
				l.add(1);
				l.add(3);
				query.addCriteria(Criteria.where("status").in(l));
			} else {
				query.addCriteria(Criteria.where("status").is(Integer.valueOf(status)));
			}
			
			List<Criteria> orCriteriaList = new ArrayList<>();
			
			//查询 项目名称跟客户 
			if(Common.isNotEmpty(pnameid)) {
				orCriteriaList.add(Criteria.where("pname.$id").is(new ObjectId(pnameid)));
			}
			if(Common.isNotEmpty(customerid)) {
				orCriteriaList.add(Criteria.where("newCustomer.$id").is(new ObjectId(customerid)));
			}

			if(Common.isNotEmpty(stockid)) {
				orCriteriaList.add(Criteria.where("stock.$id").is(new ObjectId(stockid)));
			}
			if(Common.isNotEmpty(modelid)) {
				orCriteriaList.add(Criteria.where("stock.$id").is(new ObjectId(modelid)));
			}
			
			if (!orCriteriaList.isEmpty()) {
			    Criteria orCriteria = new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0]));
			    query.addCriteria(orCriteria);
			}
			query.addCriteria(Criteria.where("isDisable").is(false));
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.with(new Sort(new Order(Direction.DESC, "createTime")));
			pagination = this.findPaginationByQuery(query, pageNo, pageSize, PickUpApplication.class);
			if (pagination == null)
				pagination = new Pagination<PickUpApplication>();
			return pagination;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pagination;

	}

	@Override
	@SystemServiceLog(description = "编辑待出库信息")
	public void saveOrUpdate(PickUpApplication stock) {
		if (Common.isNotEmpty(stock)) {
			if (Common.isNotEmpty(stock.getId())) {
				// update
				PickUpApplication ed = this.findOneById(stock.getId(), PickUpApplication.class);
				BeanUtils.copyProperties(stock, ed);
				this.save(stock);
			} else {
				// insert
				this.insert(stock);
			}
		}
	}

	@Override
	@SystemServiceLog(description = "启用禁用待库存信息")
	public BasicDataResult disable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		PickUpApplication stock = this.findOneById(id, PickUpApplication.class);
		if (Common.isEmpty(stock)) {
			return BasicDataResult.build(400, "禁用失败，该条信息可能已被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);
		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "恢复成功", stock.getIsDisable());
	}

	@Override
	@SystemServiceLog(description = "获取所有非禁用库存信息")
	public List<PickUpApplication> findAllPickUpApplication(boolean isdisable, String areaId) {
		Query query = new Query();
		if (Common.isNotEmpty(areaId)) {
			query.addCriteria(Criteria.where("area.$id").is(new ObjectId(areaId)));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PickUpApplication.class);
	}

	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除待出库信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				PickUpApplication de = this.findOneById(edid, PickUpApplication.class);
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
	@SystemServiceLog(description = "启用禁用待出库信息")
	public BasicDataResult todisable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		PickUpApplication stock = this.findOneById(id, PickUpApplication.class);
		if (stock == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		stock.setIsDisable(stock.getIsDisable().equals(true) ? false : true);
		this.save(stock);

		return BasicDataResult.build(200, stock.getIsDisable().equals(true) ? "禁用成功" : "启用成功", stock.getIsDisable());
	}

	@Override
	public List<PickUpApplication> findAllPickUpApplicationByStatus(boolean isdisable, int status) {
		Query query = new Query();
		if (Common.isNotEmpty(status)) {
			query.addCriteria(Criteria.where("status").is(status));
		}
		query.addCriteria(Criteria.where("isDisable").is(isdisable == true ? true : false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PickUpApplication.class);
	}

	@Override
	public List<PickUpApplication> findPickUpApplicationsByStockId(String stockId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("stock.$id").is(new ObjectId(stockId)))
				.addCriteria(Criteria.where("isDelete").is(false)).addCriteria(Criteria.where("isDisable").is(false))
				.addCriteria(Criteria.where("status").ne(2));
		return this.find(query, PickUpApplication.class);

	}

	/**
	 * 执行上传文件，返回错误消息
	 */
	@SystemServiceLog(description = "上传预出库信息")
	@Override
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

	@SystemServiceLog(description = "批量导入预出库信息")
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
			PickUpApplication importPickup = new PickUpApplication();
			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

				PickUpApplication pickUpApplication = null; // 库存信息
				String areaName = resultexcel[i][j].trim();// 区域名称
				// 通过区域名称查询区域是否存在
				Area area = this.areaService.findByName(areaName);
				if (Common.isEmpty(area)) {
					error += "<span class='entypo-attention'></span>导入文件过程中，第<b>&nbsp&nbsp" + (i + 1)
							+ "行出现未添加的区域，请手动去修改该条信息或创建区域！&nbsp&nbsp</b></br>";
					return error;
				}
				importPickup.setArea(area);

				String name = resultexcel[i][j + 1].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}

				String model = resultexcel[i][j + 2].trim();
				if (Common.isEmpty(model)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备型号为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}

				String supplierName = resultexcel[i][j + 3].trim();// 供应商名称
				Supplier supplier = null;
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByName(supplierName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				} else {
					error += "<span class='entypo-attention'></span>导入文件过程中出现供应商为空<b>&nbsp;&nbsp;" + supplierName
							+ "&nbsp;&nbsp;</b>，请添加对应供应商，第<b>&nbsp&nbsp" + (i + 1) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}

				// 通过设备名称 型号 区域 供应商 来获取库存信息
				Stock stock = this.stockService.findByAreaNameModel(area.getId(), name, model, supplier.getId());
				// 绑定库存信息
				importPickup.setStock(stock);

				String customerName = resultexcel[i][j + 4].trim();// 供应商名称
				NewCustomer newCustomer = null;
				if (Common.isNotEmpty(customerName)) {
					// 根据供应商名称查找，看供应商是否存在
					newCustomer = this.newCustomerService.findByName(customerName);
					if (Common.isEmpty(newCustomer)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的客户<b>&nbsp;&nbsp;" + customerName
								+ "&nbsp;&nbsp;</b>，请先添加供客户，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				} else {
					error += "<span class='entypo-attention'></span>导入文件过程中出现客户为空<b>&nbsp;&nbsp;" + customerName
							+ "&nbsp;&nbsp;</b>，请添加对应供应商，第<b>&nbsp&nbsp" + (i + 1) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}

				importPickup.setNewCustomer(newCustomer);

				String n = resultexcel[i][j + 5].trim();
				if (Common.isEmpty(n)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现预出库数量为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}

				boolean num = Common.isInteger(n);
				if (num) {
					importPickup.setEstimatedIssueQuantity(Long.valueOf(n));
				} else {
					error += "<span class='entypo-attention'></span>导入文件过程中出现不合法的预出库数量<b>&nbsp;&nbsp;" + n
							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				//检查库存数量 以及预出库中的数量 做比较
				List<PickUpApplication> checkpickUpApplication = this.findPickUpApplicationsByStockId(stock.getId());
				long ycknum = checkpickUpApplication.stream().map(PickUpApplication::getEstimatedIssueQuantity).reduce((long) 0,
						Long::sum);
				long acnum = checkpickUpApplication.stream().map(PickUpApplication::getActualIssueQuantity).reduce((long) 0,
						Long::sum);

				if (Long.valueOf(n) > (stock.getInventory() - (ycknum - acnum))) {
					error += "<span class='entypo-attention'></span>导入文件过程中第<b>&nbsp&nbsp"+ (i + 1)  +"出现不合法的库存数量不足<b>&nbsp;&nbsp;当前剩余可出库数量" + String.valueOf(stock.getInventory() - (ycknum - acnum))
							+ "&nbsp;&nbsp;</b>，请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				
				
				
				
				

				String projname = resultexcel[i][j + 6].trim();// 项目名称

				String pitemid = resultexcel[i][j + 7].trim(); // 项目编号

				Pname pname = null;

				if (Common.isEmpty(projname) && Common.isEmpty(pitemid)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现项目名称跟项目编号都为空<b>&nbsp;&nbsp;" + n
							+ "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1) + "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				} else {
					pname = this.pnameService.findByNameAndItemid(projname, pitemid);
					if (Common.isEmpty(pname)) {
						error += "<span class='entypo-attention'></span>导入文件过程中无法找到项目名称和编号，请核实后在导入<b>&nbsp;&nbsp;"
								+ customerName + "&nbsp;&nbsp;</b>，第<b>&nbsp&nbsp" + (i + 1)
								+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importPickup.setPname(pname);

				String accepter = resultexcel[i][j + 8].trim();// 领料人
				if (Common.isEmpty(n)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现预出库领料人为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}
				importPickup.setAccepter(accepter);
				importPickup.setStatus(1);
				User suser = (User) session.getAttribute(Contents.USER_SESSION);
				importPickup.setPublisher(suser);
				this.insert(importPickup);
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

	@Override
	public List<PickUpApplication> getbatchByids(List ids) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").in(ids));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.addCriteria(Criteria.where("isDelete").is(false));
		return this.find(query, PickUpApplication.class);
	}

}
