package zhongchiedu.inventory.service.Impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.inventory.pojo.InventoryTransfer;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.InventoryTransferService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class InventoryTransferServiceImpl extends GeneralServiceImpl<InventoryTransfer> implements InventoryTransferService {

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired SystemClassificationServiceImpl systemClassificationService;

	private @Autowired CategoryServiceImpl categoryService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired BrandServiceImpl brandService;

	private @Autowired AreaService areaService;

	private @Autowired StockStatisticsService stockStatisticsService;

	@Autowired
	private MultiMediaService multiMediaService;

	/**
	 * 获取所有库存流转
	 */
	@Override
	public Pagination<InventoryTransfer> findpagination(Integer pageNo, Integer pageSize,String start, String end, String search) {
		
		// 分页查询数据
				Pagination<InventoryTransfer> pagination = null;
				try {
					Query query = new Query();

					query = this.findbySearch(search, start,  end);
				
					query.with(new Sort(new Order(Direction.DESC, "createTime")));
					pagination = this.findPaginationByQuery(query, pageNo, pageSize, InventoryTransfer.class);
					if (pagination == null)
						pagination = new Pagination<InventoryTransfer>();
					return pagination;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return pagination;
		
		
	}

	@Override
	@SystemServiceLog(description = "编辑库存流转")
	public void saveOrUpdate(InventoryTransfer inventoryTransfer) {

		if (Common.isNotEmpty(inventoryTransfer)) {
			if (Common.isNotEmpty(inventoryTransfer.getId())) {
				// update
				InventoryTransfer ed = this.findOneById(inventoryTransfer.getId(), InventoryTransfer.class);
				inventoryTransfer.setUpdateTime(new Date());
				BeanUtils.copyProperties(inventoryTransfer, ed);
				this.save(inventoryTransfer);
			} else {
				// insert
				inventoryTransfer.setUpdateTime(new Date());
				this.insert(inventoryTransfer);
			}
		}
	}



	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除库存信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				InventoryTransfer de = this.findOneById(edid, InventoryTransfer.class);
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
	public BasicDataResult todisable(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "无法禁用，请求出现问题，请刷新界面!", null);
		}
		InventoryTransfer inventoryTransfer = this.findOneById(id, InventoryTransfer.class);
		if (inventoryTransfer == null) {
			return BasicDataResult.build(400, "无法获取到供应商信息，该用户可能已经被删除", null);
		}
		inventoryTransfer.setIsDisable(inventoryTransfer.getIsDisable().equals(true) ? false : true);
		this.save(inventoryTransfer);

		return BasicDataResult.build(200, inventoryTransfer.getIsDisable().equals(true) ? "禁用成功" : "启用成功", inventoryTransfer.getIsDisable());

	}

	@Override
	public ProcessInfo findproInfo(HttpServletRequest request) {

		return (ProcessInfo) request.getSession().getAttribute("proInfo");
	}

	@Override
	@SystemServiceLog(description = "批量导入库存流转信息")
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
			InventoryTransfer importInventoryTransfer = new InventoryTransfer();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

				Supplier supplier = null;
				Unit unit = null;
	
				String name = resultexcel[i][j].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现设备名称为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}	
				importInventoryTransfer.setName(name.replaceAll("/", "^")); // 设备名称
				
				
				String model = resultexcel[i][j + 1].trim();
				if (Common.isNotEmpty(model)) {
					importInventoryTransfer.setModel(model);// 设备型号
				}
				
				String scope = resultexcel[i][j + 2].trim();
				if (Common.isNotEmpty(scope)) {
					importInventoryTransfer.setScope(scope);// 使用范围
				}
				
				String transferQuantity = resultexcel[i][j + 3].trim();//中转数量
				if (Common.isEmpty(transferQuantity)||transferQuantity.equals(0)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现数量为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "行请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}	
				importInventoryTransfer.setTransferQuantity(Long.valueOf(transferQuantity));
				
				String price = resultexcel[i][j + 4].trim();
				if (Common.isNotEmpty(price)) {
					importInventoryTransfer.setPrice(price);// 价格
				}
				
				String unitName = resultexcel[i][j + 5].trim();
				if (Common.isNotEmpty(unitName)) {
					//没有计量单位就创建一个
					unit = this.unitService.findByName(unitName);
				}
				importInventoryTransfer.setUnit(unit);
				
				String inventoryTransferDate = resultexcel[i][j + 6].trim();

				if (Common.isNotEmpty(inventoryTransferDate)) {
					try {
						Common.getDateYMD(inventoryTransferDate);
					}catch(Exception e) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现错误的日期，请按照格式输入第<b>&nbsp&nbsp" + (i + 1)
								+ "行，请手动去修改该条信息！&nbsp&nbsp</b></br>";
						e.printStackTrace();
						continue;
					}
					
					//中转日期
					importInventoryTransfer.setInventoryTransferDate(inventoryTransferDate);
				}
				
				String maintenance = resultexcel[i][j + 7].trim();
				if (Common.isNotEmpty(maintenance)) {
					importInventoryTransfer.setMaintenance(maintenance);// 维保	
				}
				
				String receivables =  resultexcel[i][j + 8].trim();// 应收款
				if (Common.isNotEmpty(receivables)) {
					importInventoryTransfer.setReceivables(receivables=="是");
				}
				
				String personInCharge =  resultexcel[i][j + 9].trim();// 负责人
				if (Common.isNotEmpty(personInCharge)) {
					importInventoryTransfer.setPersonInCharge(personInCharge);
				}
				
				String entryName =  resultexcel[i][j + 10].trim();// 项目名称
				if (Common.isNotEmpty(entryName)) {
					importInventoryTransfer.setEntryName(entryName);
				}
				
				String itemNo =  resultexcel[i][j + 11].trim();// 项目编号
				if (Common.isNotEmpty(itemNo)) {
					importInventoryTransfer.setItemNo(itemNo);
				}
				
				String supplierName = resultexcel[i][j + 12].trim();// 供应商名称
				if (Common.isNotEmpty(supplierName)) {
					// 根据供应商名称查找，看供应商是否存在
					supplier = this.supplierService.findByName(supplierName);
					if (Common.isEmpty(supplier)) {
						error += "<span class='entypo-attention'></span>导入文件过程中出现不存在的供应商<b>&nbsp;&nbsp;" + supplierName
								+ "&nbsp;&nbsp;</b>，请先添加供应商，第<b>&nbsp&nbsp" + (i + 1)
								+ "行请手动去修改该条信息！&nbsp&nbsp</b></br>";
						continue;
					}
				}
				importInventoryTransfer.setSupplier(supplier);
				
				
				String customer =  resultexcel[i][j + 13].trim();// 客户
				if (Common.isNotEmpty(customer)) {
					importInventoryTransfer.setCustomer(customer);
				}
				
				String contactNumber =  resultexcel[i][j + 14].trim();// 联系电话
				if (Common.isNotEmpty(contactNumber)) {
					importInventoryTransfer.setContactNumber(contactNumber);
				}
				
					this.insert(importInventoryTransfer);
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

	

	@Override
	public HSSFWorkbook export(String name, String areaId, String searchAgent) {
		// TODO Auto-generated method stub
		return null;
	}


	
	@SystemServiceLog(description = "查询库存信息")
	public Query findbySearch(String search,String start, String end) {
		Query query = new Query();
		Criteria ca = new Criteria();
		Criteria ca1 = new Criteria();
		Criteria ca2 = new Criteria();
		query.addCriteria(Criteria.where("isDelete").is(false));
		if (Common.isNotEmpty(search)) {
			
			ca1.orOperator(Criteria.where("name").regex(search),
					Criteria.where("model").regex(search), Criteria.where("scope").regex(search),
					Criteria.where("entryName").regex(search),Criteria.where("itemNo").regex(search),
					Criteria.where("contactNumber").regex(search),Criteria.where("personInCharge").regex(search)
					);
		}
		if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
			query.addCriteria(Criteria.where("inventoryTransferDate").gte(start).lte(end));
		}
		query.addCriteria(ca.andOperator(ca1));

		return query;

	}

	@Override
	@SystemServiceLog(description="导出流转库存统计信息")
	public Workbook newExport(HttpServletRequest request, String search, String start, String end) {
		
		
		List<InventoryTransfer> findInventoryTransfers = this.findInventoryTransfers(search, start, end);
		
		

		 List<Map<String, Object>> outlist = new ArrayList<>(); 
		 
		for (InventoryTransfer it : findInventoryTransfers) {
			 Map<String, Object> in =  new HashMap<>(); 
			 in.put("name", Common.isEmpty(it.getName())?"":it.getName());
			 in.put("model", Common.isEmpty(it.getModel())?"":it.getModel());
			 in.put("scope", Common.isEmpty(it.getScope())?"":it.getScope());
			 in.put("transferQuantity", Common.isEmpty(it.getTransferQuantity())?"":it.getTransferQuantity());
			 in.put("price", Common.isEmpty(it.getPrice())?"":it.getPrice());
			 if(it.getUnit()!=null) {
				 in.put("unitName", Common.isEmpty(it.getUnit().getName())?"":it.getUnit().getName());
			 }
			 try {
				in.put("inventoryTransferDate", Common.isEmpty(it.getInventoryTransferDate())?"":Common.getDateYMD(it.getInventoryTransferDate()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 in.put("maintenance", Common.isEmpty(it.getMaintenance())?"":it.getMaintenance());
			 in.put("receivables", Common.isEmpty(it.isReceivables())?"是":"否");
			 in.put("personInCharge", Common.isEmpty(it.getPersonInCharge())?"":it.getPersonInCharge());
			 in.put("entryName", Common.isEmpty(it.getEntryName())?"":it.getEntryName());
			 in.put("itemNo", Common.isEmpty(it.getItemNo())?"":it.getItemNo());
			 if(it.getSupplier()!=null) {
				 in.put("supplierName", Common.isEmpty(it.getSupplier().getName())?"":it.getSupplier().getName());
			 }
			 in.put("customer", Common.isEmpty(it.getCustomer())?"":it.getCustomer());
			 in.put("contactNumber", Common.isEmpty(it.getContactNumber())?"":it.getContactNumber());
			 outlist.add(in);
		}
		
		 Map<String, Object> dataMap = new HashMap<>();			 
				 
				dataMap.put("outlist", outlist);
			
				String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
				String fileName = "库存流转导出模板.xlsx";
				   TemplateExportParams params = new TemplateExportParams(ctxPath+fileName,true);
				Workbook doc =null;
				
				try {
					doc =ExcelExportUtil.exportExcel(params, dataMap);
//							WordUtil.exportWord(ctxPath+fileName, dataMap);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				return doc;
			

		
		
	}

	@Override
	public List<InventoryTransfer> findInventoryTransfers(String search, String start, String end) {
		
		Query findbySearch = this.findbySearch(search, start, end);
		
		return this.find(findbySearch, InventoryTransfer.class);
		
	}
	
	
	
	
	
}
