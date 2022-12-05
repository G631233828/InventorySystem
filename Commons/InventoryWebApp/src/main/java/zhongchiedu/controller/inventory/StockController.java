package zhongchiedu.controller.inventory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;import org.apache.poi.util.SystemOutLogger;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.ZipCompress;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.general.pojo.User;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Brand;
import zhongchiedu.inventory.pojo.GoodsStorage;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.inventory.pojo.Supplier;
import zhongchiedu.inventory.pojo.Unit;
import zhongchiedu.inventory.service.Impl.AreaServiceImpl;
import zhongchiedu.inventory.service.Impl.BrandServiceImpl;
import zhongchiedu.inventory.service.Impl.ColumnServiceImpl;
import zhongchiedu.inventory.service.Impl.GoodsStorageServiceImpl;
import zhongchiedu.inventory.service.Impl.StockServiceImpl;
import zhongchiedu.inventory.service.Impl.SupplierServiceImpl;
import zhongchiedu.inventory.service.Impl.UnitServiceImpl;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 设备
 * 
 * @author fliay
 *
 */
@Controller
@Slf4j
public class StockController {

	private @Autowired StockServiceImpl stockService;

	private @Autowired ColumnServiceImpl columnService;

	private @Autowired GoodsStorageServiceImpl goodsStorageService;

	private @Autowired SupplierServiceImpl supplierService;

	private @Autowired UnitServiceImpl unitService;

	private @Autowired AreaServiceImpl areaService;

	private @Autowired BrandServiceImpl brandService;	
	
	@GetMapping("stocks")
	@RequiresPermissions(value = "stock:list")
	@SystemControllerLog(description = "查询所有库存管理")
	public String list(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session,
			@ModelAttribute("errorImport") String errorImport,
			@RequestParam(value = "search", defaultValue = "") String search,
			@RequestParam(value = "searchArea", defaultValue = "") String searchArea,
			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent
			) {

		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);

		model.addAttribute("errorImport", errorImport);
		Pagination<Stock> pagination = this.stockService.findpagination(pageNo, pageSize, search, searchArea,searchAgent);
		model.addAttribute("pageList", pagination);
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		List<String> listColums = this.columnService.findColumns("stock",user.getId());
		model.addAttribute("listColums", listColums);

		session.setAttribute("pageNo", pageNo);
		session.setAttribute("pageSize", pageSize);
		session.setAttribute("search", search);
		session.setAttribute("searchArea", searchArea);
		session.setAttribute("searchAgent", searchAgent);

		model.addAttribute("pageSize", pageSize);
		model.addAttribute("search", search);
		model.addAttribute("searchArea", searchArea);
		model.addAttribute("searchAgent", searchAgent);
		return "admin/stock/list";
	}
	
	
//	@GetMapping("prestock")
//	@RequiresPermissions(value = "stock:list")
//	@SystemControllerLog(description = "查询所有预库存管理")
//	public String prestock(@RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo, Model model,
//			@RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize, HttpSession session) {
//
//		Pagination<Stock> pagination = this.stockService.findEstimeate(pageNo, pageSize);
//
//		model.addAttribute("pageList", pagination);
//		return "admin/stock/preStock";
//	}

	/**
	 * 跳转到添加页面
	 */
	@GetMapping("/stock")
	@RequiresPermissions(value = "stock:add")
	public String addPage(Model model) {
//		// 所有货架
//		List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false);
//		model.addAttribute("goodsStorages", list);
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		//获取所有品牌
		List<Brand> brands = this.brandService.findAllBrand(false);
		model.addAttribute("brands", brands);
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);

		return "admin/stock/add";
	}

//	/**
//	 * 跳转到预库存添加页面
//	 */
//	@GetMapping("/stockestimate")
//	@RequiresPermissions(value = "stock:add")
//	public String addEstimatePage(Model model) {
//		// 所有供应商
//		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
//		model.addAttribute("suppliers", syslist);
//		// 区域
//		List<Area> areas = this.areaService.findAllArea(false);
//		model.addAttribute("areas", areas);
//		// 计量单位
//		List<Unit> listUnits = this.unitService.findAllUnit(false);
//		model.addAttribute("units", listUnits);
//		return "admin/stock/preStockAdd";
//	}

	/**
	 * 跳转到预库存添加页面
	 */
//	@GetMapping("/stockestimate{id}")
//	@RequiresPermissions(value = "stock:add")
//	public String editEstimatePage(Model model, @PathVariable String id) {
//		// 所有供应商
//		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
//		model.addAttribute("suppliers", syslist);
//		// 区域
//		List<Area> areas = this.areaService.findAllArea(false);
//		model.addAttribute("areas", areas);
//		Stock stock = this.stockService.findOneById(id, Stock.class);
//		model.addAttribute("stock", stock);
//		// 计量单位
//		List<Unit> listUnits = this.unitService.findAllUnit(false);
//		model.addAttribute("units", listUnits);
//		return "admin/stock/preStockAdd";
//	}

	@PostMapping("/stock")
	@RequiresPermissions(value = "stock:add")
	@SystemControllerLog(description = "添加设备")
	public String addUser(@ModelAttribute("stock") Stock stock, HttpSession session)

			throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		stock.setPublisher(user);// 发布人
		this.stockService.saveOrUpdate(stock);
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String searchArea = (String) session.getAttribute("searchArea");

			return "redirect:/stocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;
	
	}
	
	
	@GetMapping("/copyStock")
	@RequiresPermissions(value = "stock:copy")
	@SystemControllerLog(description = "复制设备")
	public String addUser(String id, HttpSession session) throws UnsupportedEncodingException {
		
		

		this.stockService.copyStock(id,session);
		
		
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String searchArea = (String) session.getAttribute("searchArea");
		
		return "redirect:/stocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
		+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;
		
	}
	
	
	
	
	
	

	@PutMapping("/stock")
	@RequiresPermissions(value = "stock:edit")
	@SystemControllerLog(description = "修改设备")
	public String edit(@ModelAttribute("stock") Stock stock, HttpSession session) throws UnsupportedEncodingException {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		stock.setPublisher(user);// 发布人
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String searchArea = (String) session.getAttribute("searchArea");
		this.stockService.saveOrUpdate(stock);
	
			return "redirect:/stocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
					+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;
		

	}

	/**
	 * 跳转到编辑界面
	 * 
	 * @return
	 */
	@GetMapping("/stock{id}")
	@RequiresPermissions(value = "stock:edit")
	@SystemControllerLog(description = "编辑设备")
	public String toeditPage(@PathVariable String id, Model model) {
		Stock stock = this.stockService.findOneById(id, Stock.class);
		model.addAttribute("stock", stock);
		if (Common.isNotEmpty(stock.getArea())) {
			// 所有货架
			List<GoodsStorage> list = this.goodsStorageService.findAllGoodsStorage(false, stock.getArea().getId());
			model.addAttribute("goodsStorages", list);
		}
		// 所有供应商
		List<Supplier> syslist = this.supplierService.findAllSupplier(false);
		model.addAttribute("suppliers", syslist);
		//获取所有品牌
		List<Brand> brands = this.brandService.findAllBrand(false);
		model.addAttribute("brands", brands);
		
		
		// 区域
		List<Area> areas = this.areaService.findAllArea(false);
		model.addAttribute("areas", areas);
		// 计量单位
		List<Unit> listUnits = this.unitService.findAllUnit(false);
		model.addAttribute("units", listUnits);
//		model.addAttribute("isestimate", false);// 预入库存标记
		return "admin/stock/add";

	}

	@RequestMapping("/stock/clearSearch")
//	@RequiresPermissions(value = "projectStock:delete")
	public String clearSearch(HttpSession session) {
		session.removeAttribute("pageNo");
		session.removeAttribute("pageSize");
		session.removeAttribute("search");
		session.removeAttribute("searchArea");
		return "redirect:/stocks";

	}

	@DeleteMapping("/stock/{id}")
	@RequiresPermissions(value = "stock:delete")
	@SystemControllerLog(description = "删除设备")
	public String delete(@PathVariable String id, HttpSession session) throws UnsupportedEncodingException {
		Integer pageNo = (Integer) session.getAttribute("pageNo");
		Integer pageSize = (Integer) session.getAttribute("pageSize");
		String search = (String) session.getAttribute("search");
		String searchArea = (String) session.getAttribute("searchArea");
		log.info("删除设备" + id);
		this.stockService.delete(id);
		log.info("删除设备" + id + "成功");
		return "redirect:/stocks?pageNo=" + pageNo + "&pageSize=" + pageSize + "&search="
				+ URLEncoder.encode(search, "UTF-8") + "&searchArea=" + searchArea;
	}

	/**
	 * 通过ajax获取是否存在重复账号的信息
	 * 
	 * @param printWriter
	 * @param session
	 * @param response
	 */
	@RequestMapping(value = "/stock/ajaxgetRepletes", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult ajaxgetRepletes(@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,
			@RequestParam(value = "model", defaultValue = "") String model) {
		return this.stockService.ajaxgetRepletes(name, areaId, model);
	}

	/**
	 * 根据选择的目录获取菜单
	 * 
	 * @param parentId
	 * @return
	 */
	@RequestMapping(value = "/getStorages", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getparentmenu(@RequestParam(value = "areaId", defaultValue = "") String areaId) {

		List<GoodsStorage> list = this.goodsStorageService.findStorages(areaId);

		return list != null ? BasicDataResult.build(200, "success", list) : BasicDataResult.build(400, "error", null);
	}

	@RequestMapping(value = "/stock/disable", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult toDisable(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.stockService.todisable(id);
	}

	/**
	 * 模版下载
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/stock/download")
	@SystemControllerLog(description = "下载库存管理导入模版")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String storeName = "库存管理模版.xlsx";
		String contentType = "application/octet-stream";
		String UPLOAD = "Templates/";
		FileOperateUtil.download(request, response, storeName, contentType, UPLOAD);
		return null;
	}

	/***
	 * 文件上传
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/stock/upload")
	@SystemControllerLog(description = "批量导入库存管理")
	@RequiresPermissions(value = "stock:batch")
	public ModelAndView upload(HttpServletRequest request, HttpSession session, RedirectAttributes attr) {
		log.info("开始上传文件");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redirect:/stocks");
		String error = this.stockService.upload(request, session);
		attr.addFlashAttribute("errorImport", error);
		return modelAndView;

	}

	/**
	 * process 获取进度
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/stock/uploadprocess")
	@ResponseBody
	public Object process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.stockService.findproInfo(request);
	}

	@RequestMapping(value = "/stock/columns", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult editColumns(@RequestParam(value = "column", defaultValue = "") String column,
			@RequestParam(value = "flag", defaultValue = "") boolean flag,HttpSession session) {
		User user = (User) session.getAttribute(Contents.USER_SESSION);
		return this.columnService.editColumns("stock", column, flag,user.getId());
	}

	@RequestMapping(value = "/stock/getSupplier", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getSupplier(@RequestParam(value = "id", defaultValue = "") String id) {
		return this.supplierService.findOneById(id);
	}

	@RequestMapping(value = "/stock/getProjectNames", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public BasicDataResult getProjectNames() {

		Set<String> projectNames = this.stockService.findProjectNames();

		return new BasicDataResult(200, "查询成功", projectNames);
	}

	/**
	 * 导出excel
	 */
	@RequestMapping(value = "/stock/export")
	public void exportStock(HttpServletResponse response,
			@RequestParam(value = "areaId", defaultValue = "") String areaId,
			@RequestParam(value = "searchAgent", defaultValue = "") String searchAgent) {
		try {
			response.setContentType("application/vnd.ms-excel");
			String name = Common.fromDateYM() + "库存报表";
			String fileName = new String((name).getBytes("gb2312"), "ISO8859-1");
			HSSFWorkbook wb = this.stockService.export(name, areaId,searchAgent);
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xls");
			OutputStream ouputStream = response.getOutputStream();
			wb.write(ouputStream);
			ouputStream.flush();
			ouputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	@Value("${upload.savedir}")
	private String dir;
	@Value("${qrcode.qrcodepath}")
	private String qrcodepath;

	
	/**
	 * 下载二维码
	 * 
	 */
	@RequestMapping(value = "stock/downloadQRcode")
	@SystemControllerLog(description = "下载二维码")
	public ModelAndView download(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {

		List<Stock> list = this.stockService.findStockByIds(id);

		String contentType = "application/octet-stream";
		if (list.size() == 1) {
			try {
				String downLoadPath = list.get(0).getQrCode().getQrcode().getDir()
						+ list.get(0).getQrCode().getQrcode().getSavePath()
						+ list.get(0).getQrCode().getQrcode().getOriginalName();
				FileOperateUtil.downloadbyFilePath(request, response,
						list.get(0).getName() + list.get(0).getQrCode().getQrcode().getExtension(), contentType,
						new File(downLoadPath));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// 如果下载的二维码数量大于1 那么久压缩下载
			List<File> file = new ArrayList();
			list.forEach(stock -> {
				// 获取所有下载文件file
				String downLoadPath = stock.getQrCode().getQrcode().getDir()
						+ stock.getQrCode().getQrcode().getSavePath()
						+ stock.getQrCode().getQrcode().getOriginalName();
				file.add(new File(downLoadPath));

			});

			// 获取临时压缩文件
			String storeName = ZipCompress.getZipFilename();
			log.info("压缩文件：" + storeName);
			File zipfile = new File(dir + qrcodepath + storeName);
			ZipCompress.zipFile(file, zipfile);

			try {
				FileOperateUtil.downloadbyFilePath(request, response, storeName, contentType, zipfile);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				ZipCompress.deleteFile(zipfile);
			}
		}

		return null;
	}

	
	//修复数据
	@RequestMapping(value = "stock/update")
	@ResponseBody
	@SystemControllerLog(description = "数据修复代理商品数据")
	public String updateagent() {	
		List<Stock> stocks = this.stockService.find(new Query(), Stock.class);
		StringBuffer buf = new StringBuffer();
		stocks.forEach(s->{
			
			if(!s.isAgent()){
				buf.append("修复："+s.getName()+"数据"+s.isAgent()+"</br>");
				s.setAgent(false);
				this.stockService.save(s);
			}
		});
		
		
		
		return buf.toString();
		
	}
	
	
	@RequestMapping(value = "/stock/addToStocklist", method = RequestMethod.POST)
	@RequiresPermissions(value = "stockStatistics:out")
	@ResponseBody
	public BasicDataResult addToStocklist(HttpSession session ,@RequestParam(value = "id", defaultValue = "") String id) {
		
		if(Common.isNotEmpty(id)) {
		Stock stock = this.stockService.findOneById(id, Stock.class);
		//判断库存数量是否>0
		if(stock.getInventory()<=0) {
			return new BasicDataResult(400, "当前商品库存数量为0,加入出列表失败！", "");
		}
			
		
		List getstockSession = (List) session.getAttribute(Contents.STOCK_LIST);
		
		if(getstockSession==null) {
			List list = new ArrayList<>();
			list.add(id);
			session.setAttribute(Contents.STOCK_LIST, list);
		}else {
			if(getstockSession.contains(id)) {
				return new BasicDataResult(200, "出库列表已存在，无需重复添加", "");
			}
			
			getstockSession.add(id);
			session.setAttribute(Contents.STOCK_LIST, getstockSession);
		}
		return new BasicDataResult(200, "已加入出库列表", "");
		
	
		}
		return new BasicDataResult(400, "加入出库列表失败", "");
		
		
	}
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@RequestMapping(value = "/stock/batchOut", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	public BasicDataResult batchOut(HttpSession session,String id) {
		List list = (List) session.getAttribute(Contents.STOCK_LIST);
		if(list==null&& Common.isEmpty(id)) {
			return new BasicDataResult(400, "获取出库列表失败，请先添加或选中出库商品！", null);
		}
		if(Common.isNotEmpty(id)) {
			 list = Arrays.asList(id.split(","));
			 list.forEach(o->{
				 List getstockSession = (List) session.getAttribute(Contents.STOCK_LIST);
					
					if(getstockSession==null) {
						List listid = new ArrayList<>();
						listid.add(o);
						session.setAttribute(Contents.STOCK_LIST, listid);
					}else {
						if(!getstockSession.contains(id)) {
							getstockSession.add(o);
							session.setAttribute(Contents.STOCK_LIST, getstockSession);
						}
						
					}
			 });
			
			 
			 
		}

		
		if(Common.isNotEmpty(list)&&list.size()>0) {
			List<Stock> stocks = this.stockService.findStocksByIds(list);
			List<Stock> liststock = new ArrayList<>();
			//便利 数据过滤
			stocks.forEach(s->{
				Stock stock = new Stock();
				stock.setName(s.getName());
				stock.setInventory(s.getInventory());
				stock.setModel(s.getModel());
				stock.setId(s.getId());
				liststock.add(stock);
			});
			return new BasicDataResult(200, "获取出库列表", liststock);
		}else {
			return new BasicDataResult(400, "获取出库列表失败，请先添加出库商品！", null);
		}
		
		
	}
	
	
	
	@RequestMapping(value = "/stock/deleteInSession", method = RequestMethod.POST)
	@ResponseBody
	@RequiresPermissions(value = "stockStatistics:out")
	public BasicDataResult delStockInSession(HttpSession session,@RequestParam(value = "id", defaultValue = "") String id) {
		
		if(Common.isNotEmpty(id)) {
			List list = (List) session.getAttribute(Contents.STOCK_LIST);
			if(list==null) {
				return new BasicDataResult(400, "库列表未获取到商品！", "");
			}
					list.remove(id);
			session.setAttribute(Contents.STOCK_LIST, list);
			return new BasicDataResult(200, "库列表删除成功！", id);
		}
		
		return new BasicDataResult(400, "库列表删除失败！", "");
	}
	
	@RequestMapping(value = "/stock/checkNum", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult checkNum(HttpSession session,
			@RequestParam(value = "id", defaultValue = "") String id,
			@RequestParam(value = "num", defaultValue = "") String num) {
		if(Common.isNotEmpty(id)) {
		
			boolean isnum = StringUtils.isNumeric(num);
			if(!isnum) {
				return new BasicDataResult(400, "请输入合法的数字！", "");
			}
			//根据id获取库存商品
			Stock stock = this.stockService.findOneById(id, Stock.class);
			if(stock.getInventory()<Long.valueOf(num)) {
				return new BasicDataResult(400, "出库数量大于库存数量，请检查！", "");
			}
			return new BasicDataResult(200, "库存无误！", "");
		}
		
		return new BasicDataResult(400, "获取库存信息失败", "");
	}
	
	
	@RequestMapping(value = "/stock/getQRCode", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult getQRCode(HttpSession session,
			@RequestParam(value = "id", defaultValue = "") String id) {
		List<Stock> list = this.stockService.findStockByIds(id);
		String downLoadPath =list.get(0).getQrCode().getQrcode().getSavePath()
				+ list.get(0).getQrCode().getQrcode().getOriginalName();
		
		return new BasicDataResult(200, "success", downLoadPath);
		
	}
	
	
	@RequestMapping(value = "/stock/batchPaymentOrderNo", method = RequestMethod.POST)
	@ResponseBody
	public BasicDataResult batchPaymentOrderNo(HttpSession session,
			@RequestParam(value = "stockid", defaultValue = "") String stockid,
			@RequestParam(value = "itemNo", defaultValue = "") String itemNo
			) {
		
		try {
			this.stockService.updateItemNo(stockid, itemNo);
			return new BasicDataResult(200, "批量修改采购付款申请单成功", itemNo);
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return new BasicDataResult(400, "批量修改采购付款申请单出现问题，请联系管理员", itemNo);
		
		
	}
	
	
	

	
	
	
	

}


	