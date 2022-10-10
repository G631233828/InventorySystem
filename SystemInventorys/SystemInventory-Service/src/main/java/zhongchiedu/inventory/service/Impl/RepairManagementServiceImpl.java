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
import zhongchiedu.inventory.pojo.RepairManagement;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.StockStatistics;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.inventory.service.RepairManagementService;
import zhongchiedu.inventory.service.StockStatisticsService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class RepairManagementServiceImpl extends GeneralServiceImpl<RepairManagement> implements RepairManagementService {


	/**
	 * 获取维修设备信息
	 */
	@Override
	public Pagination<RepairManagement> findpagination(Integer pageNo, Integer pageSize,String start, String end, String search) {
		
		// 分页查询数据
				Pagination<RepairManagement> pagination = null;
				try {
					Query query = new Query();

					query = this.findbySearch(search, start,  end);
					query.addCriteria(Criteria.where("isDelete").is(false));
					query.with(new Sort(new Order(Direction.DESC, "pickingDate")));
					pagination = this.findPaginationByQuery(query, pageNo, pageSize, RepairManagement.class);
					if (pagination == null)
						pagination = new Pagination<RepairManagement>();
					return pagination;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return pagination;
		
		
	}

	@Override
	@SystemServiceLog(description = "编辑维修设备信息")
	public void saveOrUpdate(RepairManagement repairManagement) {

		if (Common.isNotEmpty(repairManagement)) {
			if (Common.isNotEmpty(repairManagement.getId())) {
				// update
				RepairManagement ed = this.findOneById(repairManagement.getId(), RepairManagement.class);
				repairManagement.setUpdateTime(new Date());
				BeanUtils.copyProperties(repairManagement, ed);
				this.save(repairManagement);
			} else {
				// insert
				repairManagement.setUpdateTime(new Date());
				this.insert(repairManagement);
			}
		}
	}



	private Lock lock = new ReentrantLock();

	@Override
	@SystemServiceLog(description = "删除维修信息")
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				RepairManagement de = this.findOneById(edid, RepairManagement.class);
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
		RepairManagement repairManagement = this.findOneById(id, RepairManagement.class);
		if (repairManagement == null) {
			return BasicDataResult.build(400, "无法获取到信息，该用户可能已经被删除", null);
		}
		repairManagement.setIsDisable(repairManagement.getIsDisable().equals(true) ? false : true);
		this.save(repairManagement);

		return BasicDataResult.build(200, repairManagement.getIsDisable().equals(true) ? "禁用成功" : "启用成功", repairManagement.getIsDisable());

	}

	@Override
	public ProcessInfo findproInfo(HttpServletRequest request) {

		return (ProcessInfo) request.getSession().getAttribute("proInfo");
	}

	@Override
	@SystemServiceLog(description = "批量导入返修设备信息")
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
			RepairManagement importRepairManagement = new RepairManagement();

			pri.nownum = i;
			pri.lastnum = rowLength - i;
			session.setAttribute("proInfo", pri);
			int j = 0;
			try {

	
				String name = resultexcel[i][j].trim();// 设备名称
				if (Common.isEmpty(name)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现取件货物为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}	
				importRepairManagement.setName(name.replaceAll("/", "^")); // 设备名称
				
				String source =  resultexcel[i][j+1].trim();// 取件来源
				if(Common.isNotEmpty(source)) {
					importRepairManagement.setSource(source);
				}
				
				
				String pickingDate =  resultexcel[i][j+2].trim();// 取件日期
				if(Common.isNotEmpty(pickingDate)) {
					importRepairManagement.setPickingDate(pickingDate);
				}
				
				String repairNum =  resultexcel[i][j+3].trim();// 取件日期
				if (Common.isEmpty(repairNum)) {
					error += "<span class='entypo-attention'></span>导入文件过程中出现取件数量为空，第<b>&nbsp&nbsp" + (i + 1)
							+ "请手动去修改该条信息！&nbsp&nbsp</b></br>";
					continue;
				}	
				importRepairManagement.setRepairNum(Integer.valueOf(repairNum));
				
				String picker =  resultexcel[i][j+4].trim();// 取件人
				if(Common.isNotEmpty(picker)) {
					importRepairManagement.setPicker(picker);
				}
				
				String dateOfSending =  resultexcel[i][j+5].trim();// 寄件日期
				if(Common.isNotEmpty(dateOfSending)) {
					importRepairManagement.setDateOfSending(dateOfSending);
				}
				
				String mailingOrderNo =  resultexcel[i][j+6].trim();// 寄件单号
				if(Common.isNotEmpty(mailingOrderNo)) {
					importRepairManagement.setMailingOrderNo(mailingOrderNo);
				}
				
				String address =  resultexcel[i][j+7].trim();// 寄件地址
				if(Common.isNotEmpty(address)) {
					importRepairManagement.setAddress(address);
				}
				
				String recipientName =  resultexcel[i][j+8].trim();//收件人
				if(Common.isNotEmpty(recipientName)) {
					importRepairManagement.setRecipientName(recipientName);
				}
				
				String recipientTel =  resultexcel[i][j+9].trim();//电话
				if(Common.isNotEmpty(recipientTel)) {
					importRepairManagement.setRecipientTel(recipientTel);
				}
				
				String quantityResurned =  resultexcel[i][j+10].trim();//返还数量
				if(Common.isNotEmpty(quantityResurned)) {
					importRepairManagement.setQuantityResurned(Integer.valueOf(quantityResurned));
				}
				
				String quantityDate =  resultexcel[i][j+11].trim();//返还日期
				if(Common.isNotEmpty(quantityDate)) {
					importRepairManagement.setQuantityDate(quantityDate);
				}
				
				String returnProcessing =  resultexcel[i][j+12].trim();//返货处理
				if(Common.isNotEmpty(returnProcessing)) {
					importRepairManagement.setReturnProcessing(returnProcessing);
				}
				
				String description =  resultexcel[i][j+13].trim();//备注
				if(Common.isNotEmpty(description)) {
					importRepairManagement.setDescription(description);
				}
					this.insert(importRepairManagement);
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


	
	@SystemServiceLog(description = "查询维修信息")
	public Query findbySearch(String search,String start, String end) {
		Query query = new Query();
		Criteria ca = new Criteria();
		Criteria ca1 = new Criteria();
		Criteria ca2 = new Criteria();
		if (Common.isNotEmpty(search)) {
			
			ca1.orOperator(Criteria.where("name").regex(search),
					Criteria.where("source").regex(search), Criteria.where("pickingDate").regex(search),
					Criteria.where("picker").regex(search),Criteria.where("dateOfSending").regex(search),
					Criteria.where("mailingOrderNo").regex(search),Criteria.where("address").regex(search),
					Criteria.where("recipientName").regex(search),Criteria.where("recipientTel").regex(search),
					Criteria.where("quantityDate").regex(search),Criteria.where("returnProcessing").regex(search),
					Criteria.where("description").regex(search)
					);
		}
		if (Common.isNotEmpty(start) && Common.isNotEmpty(end)) {
			ca2.orOperator(Criteria.where("pickingDate").gte(start).lte(end),Criteria.where("dateOfSending").gte(start).lte(end),Criteria.where("quantityDate").gte(start).lte(end));
		}
		query.addCriteria(ca.andOperator(ca1,ca2));
		return query;

	}

	@Override
	@SystemServiceLog(description="导出流转库存统计信息")
	public Workbook newExport(HttpServletRequest request, String search, String start, String end) {
		
		
		List<RepairManagement> findRepairManagements = this.findRepairManagements(search, start, end);
		
		

		 List<Map<String, Object>> outlist = new ArrayList<>(); 
		 
		for (RepairManagement it : findRepairManagements) {
			 Map<String, Object> in =  new HashMap<>(); 
			 in.put("name", Common.isEmpty(it.getName())?"":it.getName());
			 in.put("source", Common.isEmpty(it.getSource())?"":it.getSource());
			 in.put("repairNum", Common.isEmpty(it.getRepairNum())?"":it.getRepairNum());
			 in.put("picker", Common.isEmpty(it.getPicker())?"":it.getPicker());
			 in.put("mailingOrderNo", Common.isEmpty(it.getMailingOrderNo())?"":it.getMailingOrderNo());
			 in.put("address", Common.isEmpty(it.getAddress())?"":it.getAddress());
			 in.put("recipientName", Common.isEmpty(it.getRecipientName())?"":it.getRecipientName());
			 in.put("recipientTel", Common.isEmpty(it.getRecipientTel())?"":it.getRecipientTel());
			 in.put("quantityResurned", Common.isEmpty(it.getQuantityResurned())?"":it.getQuantityResurned());
			 in.put("returnProcessing", Common.isEmpty(it.getReturnProcessing())?"":it.getReturnProcessing());
			 in.put("description", Common.isEmpty(it.getDescription())?"":it.getDescription());
			 
			 try {
				in.put("pickingDate", Common.isEmpty(it.getPickingDate())?"":Common.getDateYMD(it.getPickingDate()));
				in.put("dateOfSending", Common.isEmpty(it.getDateOfSending())?"":Common.getDateYMD(it.getDateOfSending()));
				in.put("quantityDate", Common.isEmpty(it.getQuantityDate())?"":Common.getDateYMD(it.getQuantityDate()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 outlist.add(in);
		}
		
		 Map<String, Object> dataMap = new HashMap<>();			 
				 
				dataMap.put("outlist", outlist);
			
				String ctxPath = request.getServletContext().getRealPath("/WEB-INF/Templates/");
				String fileName = "返修设备导出模板.xlsx";
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
	public List<RepairManagement> findRepairManagements(String search, String start, String end) {
		
		Query findbySearch = this.findbySearch(search, start, end);
		
		return this.find(findbySearch, RepairManagement.class);
		
	}
	
	
	
	
	
}
