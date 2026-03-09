package zhongchiedu.inventory.service.Impl;

import lombok.extern.slf4j.Slf4j;

import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.CommonRepairItem;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.CommonRepairItemService;
import zhongchiedu.log.annotation.SystemServiceLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommonRepairItemServiceImpl extends GeneralServiceImpl<CommonRepairItem>
		implements CommonRepairItemService {

	private final Lock lock = new ReentrantLock();

	/**
	 * 分页查询常见报修项
	 */
	@Override
	@SystemServiceLog(description = "分页查询常见报修项信息")
	public Pagination<CommonRepairItem> findpagination(Integer pageNo, Integer pageSize, String search) {
		int finalPageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
		int finalPageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

		Pagination<CommonRepairItem> pagination = new Pagination<>();
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));

			// 模糊搜索：设备名称/故障描述
			if (Common.isNotEmpty(search)) {
				String regex = Pattern.quote(search.trim());
				Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

				List<Criteria> searchCriterias = new ArrayList<>();
				searchCriterias.add(Criteria.where("deviceName").regex(pattern));
				searchCriterias.add(Criteria.where("faultDesc").regex(pattern));

				query.addCriteria(new Criteria().orOperator(searchCriterias.toArray(new Criteria[0])));
			}

			// 按排序号升序，创建时间降序
			query.with(Sort.by(new Order(Direction.ASC, "sort"), new Order(Direction.DESC, "createTime")));

			pagination = this.findPaginationByQuery(query, finalPageNo, finalPageSize, CommonRepairItem.class);

			log.info("分页查询常见报修项成功 → 页码：{}，页大小：{}，搜索词：{}，查询总数：{}", finalPageNo, finalPageSize,
					search == null ? "无" : search, pagination.getTotalCount());

		} catch (Exception e) {
			log.error("分页查询常见报修项失败 → 页码：{}，页大小：{}，搜索词：{}", finalPageNo, finalPageSize, search, e);
			pagination.setPageNo(finalPageNo);
			pagination.setPageSize(finalPageSize);
			pagination.setTotalCount(0);
		}
		return pagination;
	}

	/**
	 * 保存/更新报修项
	 */
	@Override
	@SystemServiceLog(description = "编辑常见报修项信息")
	public void saveOrUpdate(CommonRepairItem commonRepairItem) {
			if (Common.isNotEmpty(commonRepairItem)) {
				if (Common.isNotEmpty(commonRepairItem.getId())) {
					// update
					CommonRepairItem ed = this.findOneById(commonRepairItem.getId(), CommonRepairItem.class);
					BeanUtils.copyProperties(commonRepairItem, ed);
					this.save(commonRepairItem);
					log.info("修改成功");
				} else {
					// insert
					this.insert(commonRepairItem);
					log.info("添加成功");
				}
			}
	}

	/**
	 * 查询所有报修项（区分禁用状态）
	 */
	@Override
	@SystemServiceLog(description = "查询所有常见报修项信息（区分禁用状态）")
	public List<CommonRepairItem> findAllName(boolean isdisable) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("isDelete").is(false));
			query.addCriteria(Criteria.where("isDisable").is(isdisable));
			query.with(Sort.by(Direction.ASC, "sort"));
			return this.find(query, CommonRepairItem.class);
		} catch (Exception e) {
			log.error("查询常见报修项列表失败", e);
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	/**
	 * 批量导入报修项
	 */
	@Override
	@SystemServiceLog(description = "批量导入常见报修项信息")
	public String BatchImport(File file, int row, HttpSession session) {
		StringBuilder errorMsg = new StringBuilder();
		String[][] excelData = null;

		try {
			excelData = ExcelReadUtil.readExcel(file, 0);
		} catch (IOException e) {
			log.error("读取常见报修项导入Excel失败", e);
			errorMsg.append("<span class='entypo-attention'></span>Excel文件读取失败：").append(e.getMessage())
					.append("</br>");
			return errorMsg.toString();
		}

		if (excelData == null || excelData.length <= 1) {
			errorMsg.append("<span class='entypo-attention'></span>Excel文件无有效数据（表头行之后无内容）</br>");
			return errorMsg.toString();
		}

		ProcessInfo processInfo = new ProcessInfo();
		processInfo.allnum = excelData.length - 1;
		session.setAttribute("proInfo", processInfo);
		int successCount = 0;
		int failCount = 0;

		for (int i = 1; i < excelData.length; i++) {
			int showRowNum = i + 1;
			try {
				processInfo.nownum = i;
				processInfo.lastnum = (excelData.length - 1) - i;
				session.setAttribute("proInfo", processInfo);

				String[] rowData = excelData[i];
				if (rowData == null || rowData.length == 0) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：无任何数据，跳过导入</br>");
					continue;
				}

				CommonRepairItem item = new CommonRepairItem();
				int colIndex = 0;

				// 1. 设备名称（必填）
				String deviceName = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (Common.isEmpty(deviceName)) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：【设备名称】为空（必填项），跳过导入</br>");
					continue;
				}
				item.setDeviceName(deviceName);
				colIndex++;

				// 2. 故障描述（必填）
				String faultDesc = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (Common.isEmpty(faultDesc)) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：【故障描述】为空（必填项），设备名称：").append(deviceName).append("，跳过导入</br>");
					continue;
				}
				item.setFaultDesc(faultDesc);
				colIndex++;

				// 3. 排序号（选填）
				if (rowData.length > colIndex && !Common.isEmpty(rowData[colIndex].trim())) {
					try {
						item.setSort(rowData[colIndex].trim());
					} catch (NumberFormatException e) {
						failCount++;
						errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
								.append("&nbsp;</b>行：【排序号】格式错误（需为数字），设备名称：").append(deviceName).append("，跳过导入</br>");
						continue;
					}
				} else {
					item.setSort("0");
				}

				// 重复校验：设备名称+故障描述唯一
				boolean isDuplicate = this.existsByUniqueKey(deviceName, faultDesc);
				if (isDuplicate) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：数据已存在（设备名称：").append(deviceName).append("，故障描述：").append(faultDesc)
							.append("），跳过重复导入</br>");
					continue;
				}

				item.setIsDelete(false);
				item.setIsDisable(false);
				item.setCreateTime(new Date());

				this.insert(item);
				successCount++;

			} catch (ArrayIndexOutOfBoundsException e) {
				failCount++;
				log.error("第{}行导入失败：列数不足", showRowNum, e);
				errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
						.append("&nbsp;</b>行：Excel列数不足，跳过导入</br>");
			} catch (Exception e) {
				failCount++;
				log.error("处理第{}行导入失败", showRowNum, e);
				String errMsg = e.getMessage() != null ? e.getMessage().replaceAll("<|>", "") : "未知错误";
				errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
						.append("&nbsp;</b>行：导入失败 → ").append(errMsg).append("，跳过导入</br>");
			}
		}

		session.removeAttribute("proInfo");
		String resultMsg;
		if (errorMsg.length() == 0) {
			resultMsg = "<span class='entypo-check'></span>导入成功！共导入" + successCount + "条数据</br>";
		} else {
			resultMsg = errorMsg.toString() + "<hr/>" + "<span class='entypo-info'></span>统计：总数据行"
					+ (excelData.length - 1) + "条 → 成功" + successCount + "条 → 失败" + failCount + "条</br>";
		}
		return resultMsg;
	}

	/**
	 * 重复校验：设备名称+故障描述
	 */
	private boolean existsByUniqueKey(String deviceName, String faultDesc) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.addCriteria(Criteria.where("deviceName").is(deviceName));
		query.addCriteria(Criteria.where("faultDesc").is(faultDesc));
		
		List<CommonRepairItem> find = this.find(query, CommonRepairItem.class);
		return find.size() > 0;
	}

	/**
	 * 上传导入文件
	 */
	@Override
	@SystemServiceLog(description = "上传常见报修项Excel文件")
	public String upload(HttpServletRequest request, HttpSession session) {
		String errorMsg = "";
		try {
			String uploadPath = File.separator + "FileUpload" + File.separator + "commonRepairItem";
			String[] allowFileTypes = { "xls", "xlsx" };

			List<Map<String, Object>> uploadResult = FileOperateUtil.upload(request, uploadPath, allowFileTypes);
			if (uploadResult == null || uploadResult.isEmpty()) {
				errorMsg = "<span class='entypo-attention'></span>文件上传失败：未获取到上传文件</br>";
				return errorMsg;
			}

			boolean hasValidSuffix = (Boolean) uploadResult.get(0).get("hassuffix");
			if (!hasValidSuffix) {
				errorMsg = "<span class='entypo-attention'></span>文件格式错误：仅支持xls/xlsx格式</br>";
				return errorMsg;
			}

			String filePath = (String) uploadResult.get(0).get("savepath");
			File excelFile = new File(filePath);
			errorMsg = this.BatchImport(excelFile, 1, session);

		} catch (Exception e) {
			log.error("常见报修项文件上传失败", e);
			errorMsg = "<span class='entypo-attention'></span>文件上传异常：" + e.getMessage() + "</br>";
		}
		return errorMsg;
	}

	/**
	 * 获取导入进度
	 */
	@Override
	@SystemServiceLog(description = "查询常见报修项导入进度")
	public ProcessInfo findproInfo(HttpServletRequest request) {
		try {
			return (ProcessInfo) request.getSession().getAttribute("proInfo");
		} catch (Exception e) {
			log.error("获取常见报修项导入进度失败", e);
			return new ProcessInfo();
		}
	}

	/**
	 * 根据搜索词查询ID列表
	 */
	@Override
	public List<ObjectId> findIdsBySearch(String search) {
		if(Common.isNotEmpty(search)) {
			Query query = new Query();
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(
					Criteria.where("deviceName").regex(search),
					Criteria.where("faultDesc").regex(search)
			));
			List<CommonRepairItem> items = this.find(query, CommonRepairItem.class);
			return items.stream()
		            .map(item -> new ObjectId(item.getId()))
		            .filter(Objects::nonNull)
		            .collect(Collectors.toList());
		}
		return null;
	}

	/**
	 * 获取所有启用的报修项（用于前端快速选择）
	 */
	@Override
	public List<CommonRepairItem> findAllEnabledItems() {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.with(Sort.by(Direction.ASC, "sort"));
		return this.find(query, CommonRepairItem.class);
	}
	@Override
	public String delete(String id) {
		try {
			lock.lock();
			List<String> ids = Arrays.asList(id.split(","));
			for (String edid : ids) {
				CommonRepairItem de = this.findOneById(edid, CommonRepairItem.class);
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
	public List<CommonRepairItem> listByDeviceName(String deviceName) {
		Query query = new Query();
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		query.with(Sort.by(Direction.ASC, "sort"));
		query.addCriteria(Criteria.where("deviceName").is(deviceName));
		return this.find(query, CommonRepairItem.class);
	}
}