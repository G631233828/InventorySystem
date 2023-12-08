package zhongchiedu.controller.inventory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.WordUtil;
import zhongchiedu.common.utils.ZipCompress;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.*;
import zhongchiedu.inventory.service.Impl.*;
import zhongchiedu.inventory.service.SignService;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class StockStatisticsController {

	private @Autowired StockStatisticsServiceImpl stockStatisticsService;

	private @Autowired StockServiceImpl stockService;

	private @Autowired ColumnServiceImpl columnService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired SystemClassificationServiceImpl ssCService;

	@GetMapping("stockStatisticss")
	@RequiresPermissions(value = "stockStatistics:list")
	@SystemControllerLog(description = "查询库存统计")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
//			@RequestParam(value = "search", defaultValue = "") String search,
			@ModelAttribute RequestBo requestBo
//			@RequestParam(value = "start", defaultValue = "") String start,
//			@RequestParam(value = "end", defaultValue = "") String end,
//			@RequestParam(value = "type", defaultValue = "") String type,
//			@RequestParam(value = "id", defaultValue = "") String id,
//			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
//			@RequestParam(value = "userId", defaultValue = "") String userId,
//			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent,
//			@RequestParam(value = "revoke", defaultValue = "2") String revoke,
//			@RequestParam(value = "confirm", defaultValue = "") String confirm,
//			@RequestParam(value = "ssC", defaultValue = "") String ssC
			)throws JsonProcessingException {
		// 区域

		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		ObjectMapper objectMapper=new ObjectMapper();
		String jsonString=objectMapper.writeValueAsString(requestBo);
		model.addAttribute("Bo",jsonString);
		model.addAttribute("requestBo",requestBo);
		List<SystemClassification>  ssCs=this.ssCService.findAllSystemClassification(false);
		model.addAttribute("ssCs",ssCs);
//		revoke="2";默认都是正常 隐藏 已撤销
//		Pagination<StockStatistics> pagination = this.stockStatisticsService.findpagination(pageNo, pageSize, search,
//				start, end, type, id, searchArea, searchAgent,userId,revoke,confirm,ssC);
		Pagination<StockStatistics> pagination = this.stockStatisticsService.findpagination(pageNo,pageSize,requestBo);
		model.addAttribute("pageList", pagination);
//		double sum = pagination.getDatas().stream().mapToDouble(StockStatistics::getInprice).sum();
//		model.addAttribute("sum", sum);
//		List<StockStatistics> datas = pagination.getDatas();
//		model.addAttribute("stlist", datas);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		List<String> listColums = this.columnService.findColumns("stockStatistics",user.getId());


		model.addAttribute("listColums", listColums);

//		model.addAttribute("search", search);
//		model.addAttribute("revoke", revoke);
//		model.addAttribute("start", start);
//		model.addAttribute("end", end);
//		model.addAttribute("id", id);
//		model.addAttribute("type", type);
		model.addAttribute("pageSize", pageSize);
//		model.addAttribute("searchArea", searchArea);
//		model.addAttribute("ssC",ssC);
//		model.addAttribute("searchAgent", searchAgent);
//		model.addAttribute("userId", userId);
//		model.addAttribute("confirm",confirm);

		return "admin/stockStatistics/list";
	}

	@RequestMapping(value = "/stockStatistics/in", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:in")
	@SystemControllerLog(description = "设备入库")
	public BasicDataResult in(@ModelAttribute("stockStatistics") StockStatistics stockStatistics, HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);
	}

	@RequestMapping(value = "/stockStatistics/out", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	@SystemControllerLog(description = "设备出库")
	public BasicDataResult out(@ModelAttribute("stockStatistics") StockStatistics stockStatistics,
			HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		String orderNum = Common.getOrderNum();
		stockStatistics.setOutboundOrder(orderNum);
		return this.stockStatisticsService.inOrOutstockStatistics(stockStatistics, user);

	}

	@RequestMapping(value = "/stockStatistics/batchOut", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	@SystemControllerLog(description = "设备出库")
	public BasicDataResult batchOut(String batchid, String batchnum, String batchdescription,
			String batchpersonInCharge, String batchprojectName, String batchcustomer,String accepter, HttpSession session) {

		String[] ids = batchid.split(",");
		String[] nums = batchnum.split(",");
		List<String> batchidList = Arrays.asList(ids);
		List<String> batchnumList = Arrays.asList(nums);

		if (batchidList.size() != batchnumList.size()) {
			return new BasicDataResult(400, "出库商品与id不匹配", "");
		}
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		String orderNum = Common.getOrderNum();
		List<Object> list = new ArrayList<>();

		for (int i = 0; i < batchidList.size(); i++) {
			Stock stock = this.stockService.findOneById(batchidList.get(i), Stock.class);
			StockStatistics st = new StockStatistics();
			st.setStock(stock);
			st.setNum(Long.valueOf(batchnumList.get(i)));
			st.setAccepter(accepter);
			st.setPersonInCharge(batchpersonInCharge);
			st.setProjectName(batchprojectName);
			st.setCustomer(batchcustomer);
			st.setDescription(batchdescription);
			st.setInOrOut(false);
			st.setOutboundOrder(orderNum);
			BasicDataResult r = this.stockStatisticsService.inOrOutstockStatistics(st, user);
			StockStatistics statics = (StockStatistics) r.getData();

			StockStatistics s = new StockStatistics();
			s.setId(statics.getStock().getId());
			s.setNewNum(statics.getNewNum());
			list.add(s);
		}
		// 出库成功清除session
		session.removeAttribute(Contents.STOCK_LIST);

		return new BasicDataResult(200, "批量出库成功!", list);
	}

	@RequestMapping(value = "/stockStatistics/findStock", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getStock(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockService.findOneById(id);
	}

	@RequestMapping(value = "/stockStatistics/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		
		return this.columnService.editColumns("stockStatistics", column, flag,user.getId());
	}

	@RequestMapping(value = "/stockStatistics/revoke", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "出入库撤销")
	@RequiresPermissions(value = "stockStatistics:revoke")
	public BasicDataResult revoke(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockStatisticsService.revoke(id);
	}

	@RequestMapping(value = "/stockStatistics/confirm", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "核对")
	@RequiresPermissions(value = "stockStatistics:confirm")
	public BasicDataResult confirm(@RequestParam(value = "id", defaultValue = "") String id) {
		if(id.contains(",")){
			String[] ids = id.split(",");
			for (String newid: ids) {
				StockStatistics st = this.stockStatisticsService.findOneById(newid, StockStatistics.class);
				st.setConfirm(true);
				this.stockStatisticsService.save(st);
			}
			return BasicDataResult.build(200, "已核对", "");
		}else{
			return this.stockStatisticsService.confirm(id);
		}
	}


	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/stockStatistics/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportStock (
//			@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
//			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
//			@RequestParam(value = "search", defaultValue = "") String search,
//			@RequestParam(value = "start", defaultValue = "") String start,
//			@RequestParam(value = "end", defaultValue = "") String end,
//			@RequestParam(value = "type", defaultValue = "") String type,
//			@RequestParam(value = "id", defaultValue = "") String id,
//			@RequestParam(value = "areaId", defaultValue = "") String areaId,
//			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent,
			HttpServletResponse response,
			HttpServletRequest request,
				@ModelAttribute RequestBo bo
	) throws Exception{
	
		String exportName = Common.fromDateYMD() + "库存统计";

		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.stockStatisticsService.newExport(request, bo);

		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();
		
		
		
	}

	/**
	 * 导出金蝶适配excel的报表
	 */
	@RequestMapping(value = "/stockStatistics/toJD", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportJD (
//			                 @RequestParam(value = "search", defaultValue = "") String search,
//							 @RequestParam(value = "start", defaultValue = "") String start,
//							 @RequestParam(value = "end", defaultValue = "") String end,
//							 @RequestParam(value = "type", defaultValue = "") String type,
//							 @RequestParam(value = "id", defaultValue = "") String id,
//							 @RequestParam(value = "areaId", defaultValue = "") String areaId,
//							 @RequestParam(value = "searchAgent", defaultValue = "") String searchAgent,
							@ModelAttribute RequestBo requestBo,
							   HttpServletResponse response,
							 HttpServletRequest request) throws Exception{

		String exportName = Common.fromDateYMD() + "库存统计";

		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.stockStatisticsService.toJD(request, requestBo);

		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();



	}
	
	/**
	 * 导出excel（新）
	 */
	@RequestMapping(value = "/stockStatistics/exportNew", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportNew (
//			@RequestParam(value = "search", defaultValue = "") String search,
//			@RequestParam(value = "start", defaultValue = "") String start,
//			@RequestParam(value = "end", defaultValue = "") String end,
//			@RequestParam(value = "type", defaultValue = "") String type,
//			@RequestParam(value = "id", defaultValue = "") String id,
//			@RequestParam(value = "areaId", defaultValue = "") String areaId,
//			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent,
			@ModelAttribute RequestBo bo,
			HttpServletResponse response,
			HttpServletRequest request) throws Exception{
		
		String exportName = bo.getStart()+"~"+bo.getEnd()+"库存统计";
		
		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.stockStatisticsService.newExport2(request,bo);
		
		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();
		
		
		
	}
	
	@RequestMapping(value = "/stockStatistics/exportNew3", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportNew3 (@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "start", defaultValue = "") String start,
			@RequestParam(value = "end", defaultValue = "") String end,
			@RequestParam(value = "type", defaultValue = "") String type,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,
			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent, HttpServletResponse response,
			HttpServletRequest request) throws Exception{
		
		String exportName = start+"~"+end+"库存统计";
		
		response.setContentType("application/vnd.ms-excel");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
		Workbook newExport = this.stockStatisticsService.newExport3(request, search, start, end, type, fileName, areaId, searchAgent);
		
		OutputStream out = response.getOutputStream();
		newExport.write(out);
		out.flush();
		out.close();
		
		
		
	}
//	/**
//	 * 导出excel
//	 */
//	@RequestMapping(value = "/stockStatistics/export", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//	@SystemControllerLog(description = "")
//	public void exportStock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
//			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize, HttpSession session,
//			@RequestParam(value = "search", defaultValue = "") String search,
//			@RequestParam(value = "start", defaultValue = "") String start,
//			@RequestParam(value = "end", defaultValue = "") String end,
//			@RequestParam(value = "type", defaultValue = "") String type,
//			@RequestParam(value = "id", defaultValue = "") String id,
//			@RequestParam(value = "areaId", defaultValue = "") String areaId,
//			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent, HttpServletResponse response) {
//		try {
//			
//			String name = "";
//			if (type.equals("in")) {
//				name = "入库统计记录";
//			} else if (type.equals("out")) {
//				name = "出库统计记录";
//			} else {
//				name = "出库入库统计记录";
//			}
//			String exportName = Common.fromDateYMD() + name;
//			
//			HSSFWorkbook wb = this.stockStatisticsService.export(search, start, end, type, exportName, areaId,
//					searchAgent);
//			response.setContentType("application/vnd.ms-excel");
//			String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
//			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
//			OutputStream ouputStream = response.getOutputStream();
//			wb.write(ouputStream);
//			ouputStream.flush();
//			ouputStream.close();
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	/**
	 * 导出excel生成出库单
	 * 
	 * @throws Exception
	 */
	@RequestMapping(value = "/stockStatistics/exportOutBoundOrder", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@SystemControllerLog(description = "")
	public void exportOutBoundOrder(@RequestParam(value = "id", defaultValue = "") String id,
			HttpServletRequest request, HttpSession session, HttpServletResponse response) throws Exception {

		String exportName = Common.fromDateYMD() + "销货单";

		response.setContentType("application/vnd.ms-word;charset=utf-8");
		String fileName = new String((exportName).getBytes("gb2312"), "ISO8859-1");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".docx");
		byte[] exportWord = this.stockStatisticsService.exportWord(id, request, session);

		OutputStream out = response.getOutputStream();
		out.write(exportWord);
		out.flush();
		out.close();

	}

	@RequestMapping(value = "/stockStatistics/update", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "更新数据状态")
	public String updateAgent() {

		List<StockStatistics> stocks = this.stockStatisticsService.find(new Query(), StockStatistics.class);
		StringBuffer buf = new StringBuffer();
		stocks.forEach(s -> {
			System.out.println(s.getStock() == null);
			if (s.getStock() != null) {
				System.out.println("修复：" + s.getId() + s.getStock().getName() + "数据" + s.isAgent() + "</br>");
				buf.append("修复：" + s.getStock().getName() + "数据" + s.isAgent() + "</br>");
				s.setAgent(s.getStock().isAgent());
				this.stockStatisticsService.save(s);
			}

		});

		return buf.toString();
	}

	/**
	 * 下载二维码
	 * 
	 */
	@RequestMapping(value = "stockStatistics/downloadQRcode")
	@SystemControllerLog(description = "下载二维码")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {

		StockStatistics stockStatistics = this.stockStatisticsService.createStockStatisticsQrCodeAndDownload(id);
		String contentType = "application/octet-stream";

		try {
			String downLoadPath = stockStatistics.getQrCode().getQrcode().getDir()
					+ stockStatistics.getQrCode().getQrcode().getSavePath()
					+ stockStatistics.getQrCode().getQrcode().getOriginalName();
			FileOperateUtil.downloadbyFilePath(request, response,
					stockStatistics.getProjectName() + stockStatistics.getCustomer()
							+ stockStatistics.getOutboundOrder()
							+ stockStatistics.getQrCode().getQrcode().getExtension(),
					contentType, new File(downLoadPath));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		//根据id获取当前选择的库存统计项
//		StockStatistics stock = this.stockStatisticsService.findOneById(id, StockStatistics.class);
//		//获取当前库存统计中一同出库的内容    
//		String contentType = "application/octet-stream";
//		if (list.size() == 1) {
//			try {
//				String downLoadPath = list.get(0).getQrCode().getQrcode().getDir()
//						+ list.get(0).getQrCode().getQrcode().getSavePath()
//						+ list.get(0).getQrCode().getQrcode().getOriginalName();
//				FileOperateUtil.downloadbyFilePath(request, response,
//						list.get(0).getName() + list.get(0).getQrCode().getQrcode().getExtension(), contentType,
//						new File(downLoadPath));
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} else {
//			// 如果下载的二维码数量大于1 那么久压缩下载
//			List<File> file = new ArrayList();
//			list.forEach(stock -> {
//				// 获取所有下载文件file
//				String downLoadPath = stock.getQrCode().getQrcode().getDir()
//						+ stock.getQrCode().getQrcode().getSavePath()
//						+ stock.getQrCode().getQrcode().getOriginalName();
//				file.add(new File(downLoadPath));
//
//			});
//
//			// 获取临时压缩文件
//			String storeName = ZipCompress.getZipFilename();
//			log.info("压缩文件：" + storeName);
//			File zipfile = new File(dir + qrcodepath + storeName);
//			ZipCompress.zipFile(file, zipfile);
//
//			try {
//				FileOperateUtil.downloadbyFilePath(request, response, storeName, contentType, zipfile);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//
//				ZipCompress.deleteFile(zipfile);
//			}
//		}
//
		return null;
	}

	
	
	
	@RequestMapping(value = "/stockStatistics/getQRCode", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getQRCode(HttpSession session,
			@RequestParam(value = "id", defaultValue = "") String id) {
		
		StockStatistics stockStatistics = this.stockStatisticsService.createStockStatisticsQrCodeAndDownload(id);

			String downLoadPath = stockStatistics.getQrCode().getQrcode().getSavePath()
					+ stockStatistics.getQrCode().getQrcode().getOriginalName();
		
		
		return new BasicDataResult(200, "success", downLoadPath.trim());
		
	}
	
	
	
	
	
	
	@Autowired
	private SignService signService;

	@RequestMapping(value = "/stockStatistics/updateSign", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	@SystemControllerLog(description = "修复签名数据")
	public BasicDataResult updateSign() {
		Query query = new Query();
		query.addCriteria(Criteria.where("outboundOrder").exists(true));
		List<StockStatistics> st = this.stockStatisticsService.find(query, StockStatistics.class);
		Set<String> outbound = new HashSet<>();

		st.forEach(s -> {
			outbound.add(s.getOutboundOrder());
		});

		Iterator<String> it = outbound.iterator();
		while (it.hasNext()) {
			// 获取所有相同订单的数据，并且判断有没有签名，如果有签名，获取签名并且将签名保存到mysign中去
			List<StockStatistics> stock = this.stockStatisticsService.findByoutboundOrder(it.next());
			String sign = stock.get(0).getSign();

			if (Common.isNotEmpty(sign)) {
				Sign s = new Sign();
				s.setSign(sign);
				this.signService.save(s);
				
				stock.forEach(a -> {
					a.setMysign(s);
					a.setSign("");
					this.stockStatisticsService.save(a);
				});

			}

		}

		return new BasicDataResult().ok();

	}

	
	
	@RequestMapping(value = "/stockStatistics/batchEditStockStatistics", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult batchPaymentOrderNo(HttpSession session,
			@RequestParam(value = "stockid", defaultValue = "") String stockid,
			@RequestParam(value = "inprice", defaultValue = "null") String inprice,
			@RequestParam(value = "purchaseInvoiceNo", defaultValue = "null") String purchaseInvoiceNo,
			@RequestParam(value = "receiptNo", defaultValue = "null") String receiptNo,
			@RequestParam(value = "paymentOrderNo", defaultValue = "null") String paymentOrderNo,
			@RequestParam(value = "sailesInvoiceNo", defaultValue = "null") String sailesInvoiceNo,
			@RequestParam(value = "sailesInvoiceDate", defaultValue = "null") String sailesInvoiceDate,
			@RequestParam(value = "purchaseInvoiceDate", defaultValue = "null") String purchaseInvoiceDate,
			@RequestParam(value = "sailPrice", defaultValue = "null") String sailPrice,
			@RequestParam(value = "newItemNo", defaultValue = "null") String newItemNo,
			@RequestParam(value = "description", defaultValue = "null") String description
			) {
		
			Double dinprice=null;
			Double dsailPrice=null;
			try {
				if(!inprice.equals("null")) {
					dinprice = Double.parseDouble(inprice);
				}else {
					dinprice=null;
				}
			}catch(Exception e) {
				e.printStackTrace();
				return new BasicDataResult(400, "入库金额输入有误，请输入正确的数字", "");
				
			}

		try {
			if(!sailPrice.equals("null")) {
				dsailPrice = Double.parseDouble(sailPrice);
			}else {
				dsailPrice=null;
			}
		}catch(Exception e) {
			e.printStackTrace();
			return new BasicDataResult(400, "销售金额输入有误，请输入正确的数字", "");

		}
			User user = (User) session.getAttribute(Contents.USER_SESSION);
			this.stockStatisticsService.updateStockStatistics(stockid, dinprice, purchaseInvoiceNo, receiptNo, paymentOrderNo,sailesInvoiceNo,sailesInvoiceDate,user,
					purchaseInvoiceDate,dsailPrice,newItemNo,description);
			
			return new BasicDataResult(200, "修改统计数据成功", "");
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
