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
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.pojo.CommonRepairItem;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.AfterSalesProjectsService;
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
public class AfterSalesProjectsServiceImpl extends GeneralServiceImpl<AfterSalesProjects>
		implements AfterSalesProjectsService {

	private final Lock lock = new ReentrantLock();

	/**
	 * 分页查询售后项目 搜索规则：纯数字匹配项目年份，非数字匹配项目名称
	 */
	@Override
	@SystemServiceLog(description = "分页查询售后项目保障信息")
	public Pagination<AfterSalesProjects> findpagination(Integer pageNo, Integer pageSize, String search) {
		// 分页参数兜底（避免空指针）
		int finalPageNo = (pageNo == null || pageNo < 1) ? 1 : pageNo;
		int finalPageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;

		Pagination<AfterSalesProjects> pagination = new Pagination<>();
		try {
			Query query = new Query();
			// 基础过滤：仅查询未删除的数据
			query.addCriteria(Criteria.where("isDelete").is(false));

			// 多字段模糊搜索条件构建（search不为空时）
			if (Common.isNotEmpty(search)) {
				// 构建正则表达式：忽略大小写 + 模糊匹配（包含搜索词）
				// Pattern.quote避免搜索词含特殊字符（如.*+?）导致正则异常
				String regex = Pattern.quote(search.trim());
				Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

				// 构建所有需要模糊匹配的字段列表（OR 条件）
				List<Criteria> searchCriterias = new ArrayList<>();
				// 1. 项目名称
				searchCriterias.add(Criteria.where("projectName").regex(pattern));
				// 2. 学校名称
				searchCriterias.add(Criteria.where("schoolName").regex(pattern));
				// 3. 中标单位
				searchCriterias.add(Criteria.where("winningBidder").regex(pattern));
				// 4. 联系老师
				searchCriterias.add(Criteria.where("contactTeacher").regex(pattern));
				// 5. 老师电话
				searchCriterias.add(Criteria.where("teacherPhone").regex(pattern));
				// 6. 学校地址
				searchCriterias.add(Criteria.where("schoolAddress").regex(pattern));
				// 7. 施工队
				searchCriterias.add(Criteria.where("worker").regex(pattern));
				// 8. 保修开始时间
				searchCriterias.add(Criteria.where("warrantyStartTime").regex(pattern));
				// 9. 保修截止时间
				searchCriterias.add(Criteria.where("warrantyEndTime").regex(pattern));
				// 10. 项目年份
				searchCriterias.add(Criteria.where("projectYear").regex(pattern));

				// OR 拼接：搜索词匹配任意一个字段即命中
				query.addCriteria(new Criteria().orOperator(searchCriterias.toArray(new Criteria[0])));
			}

			// 排序：按创建时间倒序（最新的在前）
			query.with(Sort.by(new Order(Direction.DESC, "createTime")));

			// 执行分页查询（底层需确保分页逻辑正确：skip + limit）
			pagination = this.findPaginationByQuery(query, finalPageNo, finalPageSize, AfterSalesProjects.class);

			log.info("分页查询售后项目成功 → 页码：{}，页大小：{}，搜索词：{}，查询总数：{}", finalPageNo, finalPageSize,
					search == null ? "无" : search, pagination.getTotalCount());

		} catch (Exception e) {
			log.error("分页查询售后项目保障失败 → 页码：{}，页大小：{}，搜索词：{}", finalPageNo, finalPageSize, search, e);
			// 兜底返回空分页，避免接口报错
			pagination.setPageNo(finalPageNo);
			pagination.setPageSize(finalPageSize);
			pagination.setTotalCount(0);
		}
		return pagination;
	}

	/**
	 * 保存/更新售后项目 types参数预留（若后续需关联其他实体，可扩展）
	 */
	@Override
	@SystemServiceLog(description = "编辑售后项目保障信息")
	public void saveOrUpdate(AfterSalesProjects afterSalesProjects) {
		if (Common.isNotEmpty(afterSalesProjects)) {
			if (Common.isNotEmpty(afterSalesProjects.getId())) {
				// update
				AfterSalesProjects ed = this.findOneById(afterSalesProjects.getId(), AfterSalesProjects.class);
				BeanUtils.copyProperties(afterSalesProjects, ed);
				this.save(afterSalesProjects);
				log.info("修改成功");
			} else {
				// insert
				this.insert(afterSalesProjects);
				log.info("添加成功");
			}
		}
	}

	/**
	 * 查询所有售后项目（区分禁用状态）
	 */
	@Override
	@SystemServiceLog(description = "查询所有售后项目保障信息（区分禁用状态）")
	public List<AfterSalesProjects> findAllName(boolean isdisable) {
		try {
			Query query = new Query();
			// 过滤已删除数据
			query.addCriteria(Criteria.where("isDelete").is(false));
			// 筛选禁用/启用状态
			query.addCriteria(Criteria.where("isDisable").is(isdisable));
			// 按项目名称正序排序
			query.with(Sort.by(Direction.ASC, "projectName"));
			return this.find(query, AfterSalesProjects.class);
		} catch (Exception e) {
			log.error("查询售后项目保障列表失败", e);
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	@Override
	@SystemServiceLog(description = "批量导入售后项目保障信息")
	public String BatchImport(File file, int row, HttpSession session) {
		StringBuilder errorMsg = new StringBuilder();
		String[][] excelData = null;

		// 读取Excel数据：ignoreRows=0 保留表头行
		try {
			excelData = ExcelReadUtil.readExcel(file, 0);
		} catch (IOException e) {
			log.error("读取售后项目保障导入Excel失败", e);
			errorMsg.append("<span class='entypo-attention'></span>Excel文件读取失败：").append(e.getMessage())
					.append("</br>");
			return errorMsg.toString();
		}

		// 校验有效数据
		if (excelData == null || excelData.length <= 1) {
			errorMsg.append("<span class='entypo-attention'></span>Excel文件无有效数据（表头行之后无内容）</br>");
			return errorMsg.toString();
		}

		// 初始化进度 + 统计数
		ProcessInfo processInfo = new ProcessInfo();
		processInfo.allnum = excelData.length - 1;
		session.setAttribute("proInfo", processInfo);
		int successCount = 0;
		int failCount = 0;

		// 逐行处理（跳过表头，i从1开始）
		for (int i = 1; i < excelData.length; i++) {
			int showRowNum = i + 1; // Excel实际行号
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

				AfterSalesProjects project = new AfterSalesProjects();
				int colIndex = 0;

				// 1. 项目名称（必填）
				String projectName = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (Common.isEmpty(projectName)) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：【项目名称】为空（必填项），跳过导入</br>");
					continue;
				}
				project.setProjectName(projectName);
				colIndex++;

				// 2. 学校名称（必填）
				String schoolName = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (Common.isEmpty(schoolName)) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：【学校名称】为空（必填项），项目名称：").append(projectName).append("，跳过导入</br>");
					continue;
				}
				project.setSchoolName(schoolName);
				colIndex++;

				// 3. 中标单位（选填）
				String winningBidder = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				project.setWinningBidder(winningBidder);
				colIndex++;

				// 4. 联系老师（选填）
				if (rowData.length > colIndex) {
					project.setContactTeacher(rowData[colIndex].trim());
				}
				colIndex++;

				// 5. 老师电话（选填）
				if (rowData.length > colIndex) {
					project.setTeacherPhone(rowData[colIndex].trim());
				}
				colIndex++;

				// 6. 学校地址（选填）
				String schoolAddress = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (rowData.length > colIndex) {
					project.setSchoolAddress(schoolAddress);
				}
				colIndex++;

				// 7. 施工队（选填）
				String worker = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				project.setWorker(worker);
				colIndex++;

				// 8. 保修开始时间（选填+格式校验）
				String startTime = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (rowData.length > colIndex && !Common.isEmpty(startTime)) {
					if (!startTime.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
						failCount++;
						errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
								.append("&nbsp;</b>行：【保修开始时间】格式错误（需为yyyy-MM-dd），项目名称：").append(projectName)
								.append("，跳过导入</br>");
						continue;
					}
					project.setWarrantyStartTime(startTime);
				}
				colIndex++;

				// 9. 保修截止时间（选填+格式校验）
				String endTime = rowData.length > colIndex ? rowData[colIndex].trim() : "";
				if (rowData.length > colIndex && !Common.isEmpty(rowData[colIndex].trim())) {
//                    String endTime = rowData[colIndex].trim();
					if (!endTime.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
						failCount++;
						errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;</b>").append(showRowNum)
								.append("&nbsp;</b>行：【保修截止时间】格式错误（需为yyyy-MM-dd），项目名称：").append(projectName)
								.append("，跳过导入</br>");
						continue;
					}
					project.setWarrantyEndTime(endTime);
				}
				colIndex++;

				// 10. 项目年份（选填+格式校验）
				String projectYear = "";
				if (rowData.length > colIndex && !Common.isEmpty(rowData[colIndex].trim())) {
					projectYear = rowData[colIndex].trim();
					if (!projectYear.matches("^\\d{4}$")) {
						failCount++;
						errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;</b>").append(showRowNum)
								.append("&nbsp;</b>行：【项目年份】格式错误（需为4位数字），项目名称：").append(projectName)
								.append("，跳过导入</br>");
						continue;
					}
				}
				project.setProjectYear(projectYear);
				colIndex++;

				// ===================== MongoDB重复校验（核心修改）=====================
				boolean isDuplicate = this.existsByUniqueKey(projectName, // 项目名称
						schoolName, // 学校名称
						winningBidder, // 中标单位
						worker, // 施工队
						projectYear, // 项目年份
						schoolAddress, // 学校地址
						startTime, endTime);
				if (isDuplicate) {
					failCount++;
					errorMsg.append("<span class='entypo-attention'></span>第<b>&nbsp;").append(showRowNum)
							.append("&nbsp;</b>行：数据已存在（项目名称：").append(projectName).append("，学校名称：").append(schoolName)
							.append("，中标单位：").append(Common.isEmpty(winningBidder) ? "无" : winningBidder)
							.append("，施工队：").append(Common.isEmpty(worker) ? "无" : worker).append("，项目年份：")
							.append(Common.isEmpty(projectYear) ? "无" : projectYear).append("），跳过重复导入</br>");
					continue;
				}

				// 默认值设置
				project.setIsDelete(false);
				project.setIsDisable(false);
				project.setCreateTime(new Date());

				// 保存到MongoDB（insert方法适配MongoDB）
				this.insert(project);
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

		// 导入完成：清空进度 + 拼接统计结果
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

	private boolean existsByUniqueKey(String projectName, String schoolName, String winningBidder, String worker,
			String projectYear, String schoolAddress, String startTime, String endTime) {

		Query query = new Query();
		// 过滤已删除数据
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		if (Common.isNotEmpty(projectName)) {
			query.addCriteria(Criteria.where("projectName").is(projectName));
		}
		if (Common.isNotEmpty(schoolName)) {
			query.addCriteria(Criteria.where("schoolName").is(schoolName));
		}
		if (Common.isNotEmpty(winningBidder)) {
			query.addCriteria(Criteria.where("winningBidder").is(winningBidder));
		}
		if (Common.isNotEmpty(worker)) {
			query.addCriteria(Criteria.where("worker").is(worker));
		}
		if (Common.isNotEmpty(projectYear)) {
			query.addCriteria(Criteria.where("projectYear").is(projectYear));
		}
		if (Common.isNotEmpty(schoolAddress)) {
			query.addCriteria(Criteria.where("schoolAddress").is(schoolAddress));
		}
//        if(Common.isNotEmpty(startTime)) {
//        	query.addCriteria(Criteria.where("startTime").is(startTime));
//        }
//        if(Common.isNotEmpty(endTime)) {
//        	query.addCriteria(Criteria.where("endTime").is(endTime));
//        }
		List<AfterSalesProjects> find = this.find(query, AfterSalesProjects.class);

		return find.size() > 0;
	}

	/**
	 * 上传售后项目Excel文件并触发导入
	 */
	@Override
	@SystemServiceLog(description = "上传售后项目保障Excel文件")
	public String upload(HttpServletRequest request, HttpSession session) {
		String errorMsg = "";
		try {
			// 售后项目专属上传路径
			String uploadPath = File.separator + "FileUpload" + File.separator + "afterSalesProjects";
			// 允许的文件格式
			String[] allowFileTypes = { "xls", "xlsx" };

			// 执行文件上传
			List<Map<String, Object>> uploadResult = FileOperateUtil.upload(request, uploadPath, allowFileTypes);
			if (uploadResult == null || uploadResult.isEmpty()) {
				errorMsg = "<span class='entypo-attention'></span>文件上传失败：未获取到上传文件</br>";
				return errorMsg;
			}

			// 校验文件格式
			boolean hasValidSuffix = (Boolean) uploadResult.get(0).get("hassuffix");
			if (!hasValidSuffix) {
				errorMsg = "<span class='entypo-attention'></span>文件格式错误：仅支持xls/xlsx格式</br>";
				return errorMsg;
			}

			// 调用批量导入
			String filePath = (String) uploadResult.get(0).get("savepath");
			File excelFile = new File(filePath);
			errorMsg = this.BatchImport(excelFile, 1, session);

		} catch (Exception e) {
			log.error("售后项目保障文件上传失败", e);
			errorMsg = "<span class='entypo-attention'></span>文件上传异常：" + e.getMessage() + "</br>";
		}
		return errorMsg;
	}

	/**
	 * 获取导入进度信息
	 */
	@Override
	@SystemServiceLog(description = "查询售后项目保障导入进度")
	public ProcessInfo findproInfo(HttpServletRequest request) {
		try {
			return (ProcessInfo) request.getSession().getAttribute("proInfo");
		} catch (Exception e) {
			log.error("获取售后项目保障导入进度失败", e);
			return new ProcessInfo();
		}
	}

	/**
	 * 获取所有在保项目
	 */
	@Override
	public List<AfterSalesProjects> findAllinServiceProj() {
		Query query = new Query();
		// 过滤已删除数据
		query.addCriteria(Criteria.where("isDelete").is(false));
		query.addCriteria(Criteria.where("isDisable").is(false));
		String date = Common.fromDateYMD();
		query.addCriteria(Criteria.where("warrantyEndTime").gte(date));
		return this.find(query, AfterSalesProjects.class);
	}

	@Override
	public List<ObjectId> findIdsBySearch(String search) {
		
		if(Common.isNotEmpty(search)) {
			Query query = new Query();
			Criteria ca = new Criteria();
			query.addCriteria(ca.orOperator(Criteria.where("projectName").regex(search),
					Criteria.where("winningBidder").regex(search),
					Criteria.where("projectYear").regex(search),
					Criteria.where("schoolName").regex(search),
					Criteria.where("contactTeacher").regex(search)));
			List<AfterSalesProjects> wxreporters = this.find(query, AfterSalesProjects.class);
			return wxreporters.stream()
		            .map(wxReporter -> {
		                String id = wxReporter.getId();
		                    return new ObjectId(id);
		            })
		            .filter(Objects::nonNull)
		            .collect(Collectors.toList());
		}
		
		
		return null;
	}


	


}