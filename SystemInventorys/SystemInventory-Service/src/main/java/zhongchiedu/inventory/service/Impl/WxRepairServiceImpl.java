package zhongchiedu.inventory.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.ExcelReadUtil;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.common.utils.enums.RepairStatus;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.Impl.MultiMediaServiceImpl;
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.pojo.ProcessInfo;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.pojo.WxReporter;
import zhongchiedu.inventory.service.AfterSalesProjectsService;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.inventory.service.WxReporterService;
import zhongchiedu.log.annotation.SystemServiceLog;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class WxRepairServiceImpl extends GeneralServiceImpl<WxRepair> implements WxRepairService {

    @Autowired
    private MultiMediaServiceImpl multiMediaSerice;

    @Autowired
    private WxBindingService wxBindingService;

    @Autowired
    private WxReporterService wxReporterService;

    @Autowired
    private AfterSalesProjectsService afterSalesProjectsService;

    private final Lock lock = new ReentrantLock();
    // 时间格式化器（匹配模板日期格式，GeneralBean默认时间逻辑）
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // ====================== 原有方法保留 ======================
    @Override
    public Pagination<WxRepair> findpagination(Integer pageNo, Integer pageSize, String search, Integer status, String urgencyLevel, String workerId) {
        Pagination<WxRepair> pagination = null;
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isDelete").is(false));
            if (Common.isNotEmpty(search)) {
                List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
                List<ObjectId> findprojIdsBySearch = this.afterSalesProjectsService.findIdsBySearch(search);
                query.addCriteria(Criteria.where("workOrderNumber").regex(search, "i")
                        .orOperator(Criteria.where("faultInformation").regex(search, "i"),
                                Criteria.where("reportClassroomRepair").regex(search, "i"),
                                Criteria.where("equipmentRepair").regex(search, "i"),
                                Criteria.where("equipmentYear").regex(search, "i"),
                                Criteria.where("repairContent").regex(search, "i"),
                                Criteria.where("workDept").regex(search, "i"),
                                Criteria.where("wxReporter.$id").in(findIdsBySearch),
                                Criteria.where("project.$id").in(findprojIdsBySearch)));
            }
            if (status != null) {
                query.addCriteria(Criteria.where("status").is(status));
            }
            if (Common.isNotEmpty(urgencyLevel)) {
                query.addCriteria(Criteria.where("urgencyLevel").is(urgencyLevel));
            }
            if (Common.isNotEmpty(workerId)) {
                if (workerId.equals("工程部") || workerId.equals("销售部")) {
                    query.addCriteria(Criteria.where("workDept").is(workerId));
                } else {
                    query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
                }
            }
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxRepair.class);
            if (pagination == null) pagination = new Pagination<>();
        } catch (Exception e) {
            log.error("查询报修单列表失败", e);
        }
        return pagination;
    }

    @Override
    public void saveOrUpdate(WxRepair wxRepair, MultipartFile[] photos, String imgPath, String dir) {
        if (photos.length > 0) {
            List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(photos, dir, imgPath, "WXREPAIR");
            if (uploadPictures.size() > 0) wxRepair.setPhotos(uploadPictures);
        }
        if (Common.isNotEmpty(wxRepair)) {
            if (Common.isNotEmpty(wxRepair.getId())) {
                WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
                BeanUtils.copyProperties(wxRepair, ed);
                this.save(wxRepair);
            } else {
                this.insert(wxRepair);
            }
        }
    }

    @Override
    public String delete(String id) {
        try {
            lock.lock();
            List<String> ids = Arrays.asList(id.split(","));
            for (String edid : ids) {
                WxRepair de = this.findOneById(edid, WxRepair.class);
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
    public List<WxRepair> findWxRepairByOpenId(String openId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isDelete").is(false));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        query.addCriteria(Criteria.where("openId").is(openId));
        return this.find(query, WxRepair.class);
    }

    @Override
    public WxRepair assignWorkerToRepair(String repairId, String id, String projectId) {
        WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
        if (wxRepair == null) return null;
        WxBinding wxBinding = this.wxBindingService.findOneById(id, WxBinding.class);
        if (wxBinding == null) return null;
        if (projectId != null) {
            AfterSalesProjects afterSalesProjects = this.afterSalesProjectsService.findOneById(projectId, AfterSalesProjects.class);
            if (afterSalesProjects == null || afterSalesProjects.getIsDelete() || afterSalesProjects.getIsDisable()) return null;
            wxRepair.setProject(afterSalesProjects);
        }
        wxRepair.setWorker(wxBinding);
        wxRepair.setStatus(RepairStatus.ASSIGNED.getCode());
        this.save(wxRepair);
        return wxRepair;
    }

    @Override
    public WxRepair confirmRepair(String repairId) {
        WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
        if (wxRepair == null) return null;
        wxRepair.setStatus(RepairStatus.PROCESSING.getCode());
        this.save(wxRepair);
        return wxRepair;
    }

    @Override
    public List<WxRepair> findOperationsWxRepairByOpenId(String openId, String search, Integer status, String workerId) {
        WxBinding wxBinding = this.wxBindingService.findWxBindingByOpenId(openId);
        PersonnelType personnelType = PersonnelType.getByCode(wxBinding.getPersonnelType()).orElseThrow(() -> new IllegalArgumentException("无效的人员类型编码：" + wxBinding.getPersonnelType()));
        Query query = new Query();
        if (Common.isNotEmpty(search)) {
            List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
            query.addCriteria(Criteria.where("workOrderNumber").regex(search, "i")
                    .orOperator(Criteria.where("faultInformation").regex(search, "i"),
                            Criteria.where("urgencyLevel").regex(search, "i"),
                            Criteria.where("reportClassroomRepair").regex(search, "i"),
                            Criteria.where("equipmentRepair").regex(search, "i"),
                            Criteria.where("wxReporter.$id").in(findIdsBySearch)));
        }
        if (Common.isNotEmpty(status)) query.addCriteria(Criteria.where("status").is(status));
        query.addCriteria(Criteria.where("isDelete").is(false));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        switch (personnelType) {
            case CONSTRUCTION_TEAM:
                if (Common.isEmpty(status)) query.addCriteria(Criteria.where("status").is(2));
                query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(wxBinding.getId())));
                break;
            case DISPATCHER:
                if (Common.isNotEmpty(workerId)) query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
                if (Common.isEmpty(status) && Common.isEmpty(search) && Common.isEmpty(workerId)) query.addCriteria(Criteria.where("status").is(1));
                break;
            default:
                break;
        }
        return this.find(query, WxRepair.class);
    }

    @Override
    public Pagination<WxRepair> findOperationsWxRepairByOpenIdWithPage(String openId, String search, Integer status, String workerId, Integer pageNo, Integer pageSize) {
        WxBinding wxBinding = this.wxBindingService.findWxBindingByOpenId(openId);
        if (wxBinding == null) return new Pagination<>();
        PersonnelType personnelType = PersonnelType.getByCode(wxBinding.getPersonnelType()).orElseThrow(() -> new IllegalArgumentException("无效的人员类型"));
        Query query = new Query();
        if (Common.isNotEmpty(search)) {
            List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
            query.addCriteria(Criteria.where("workOrderNumber").regex(search, "i")
                    .orOperator(Criteria.where("faultInformation").regex(search, "i"),
                            Criteria.where("urgencyLevel").regex(search, "i"),
                            Criteria.where("reportClassroomRepair").regex(search, "i"),
                            Criteria.where("equipmentRepair").regex(search, "i"),
                            Criteria.where("wxReporter.$id").in(findIdsBySearch)));
        }
        if (Common.isNotEmpty(status)) query.addCriteria(Criteria.where("status").is(status));
        query.addCriteria(Criteria.where("isDelete").is(false));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        switch (personnelType) {
            case CONSTRUCTION_TEAM:
                query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(wxBinding.getId())));
                break;
            case DISPATCHER:
                if (Common.isNotEmpty(workerId)) query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
                break;
            default:
                throw new IllegalArgumentException("不支持的人员类型");
        }
        Pagination<WxRepair> pagination = this.findPaginationByQuery(query, pageNo, pageSize, WxRepair.class);
        return pagination == null ? new Pagination<>() : pagination;
    }

    @Override
    public boolean completeRepair(WxRepair wxRepair, MultipartFile[] repairPhotos, String imgPath, String dir) {
        List<MultiMedia> uploadPictures = this.multiMediaSerice.uploadPictures(repairPhotos, dir, imgPath, "WXREPAIRCOMPLETE");
        if (uploadPictures.size() <= 0) return false;
        wxRepair.setRepairPhotos(uploadPictures);
        wxRepair.setStatus(RepairStatus.COMPLETED.getCode());
        try {
            wxRepair.setCompleteTime(Common.getDateYMDHM(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (Common.isNotEmpty(wxRepair.getId())) {
            WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
            BeanUtils.copyProperties(wxRepair, ed);
            this.save(wxRepair);
            return true;
        }
        return false;
    }

    
    @Override
    public boolean completeRepairWithTriplicate(
            WxRepair wxRepair,
            MultipartFile[] repairPhotos,
            MultipartFile[] triplicatePhotos,
            String imgPath, String dir) {

        // 维修完成照片
        List<MultiMedia> repairPics = multiMediaSerice.uploadPictures(repairPhotos, dir, imgPath, "WXREPAIRCOMPLETE");
        if (repairPics.isEmpty()) return false;

        // 三联单照片（必须上传）
        List<MultiMedia> triplicatePics = multiMediaSerice.uploadPictures(triplicatePhotos, dir, imgPath, "TRIPLICATE");
        if (triplicatePics.isEmpty()) return false;

        wxRepair.setRepairPhotos(repairPics);
        wxRepair.setTriplicatePhotos(triplicatePics); // 保存三联单
        wxRepair.setStatus(RepairStatus.COMPLETED.getCode());

        try {
            wxRepair.setCompleteTime(Common.getDateYMDHM(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (Common.isNotEmpty(wxRepair.getId())) {
            WxRepair ed = this.findOneById(wxRepair.getId(), WxRepair.class);
            BeanUtils.copyProperties(wxRepair, ed);
            this.save(wxRepair);
            return true;
        }
        return false;
    }
    
    
    
    @Override
    public List<WxRepair> findExportData(String startTime, String endTime, String workerId, Integer status, String search) {
        Query query = new Query();
        query.addCriteria(Criteria.where("isDelete").is(false));
        if (Common.isNotEmpty(startTime) && Common.isNotEmpty(endTime)) {
            try {
                Date start = dateFormatter.parse(startTime);
                Date end = dateFormatter.parse(endTime);
                Calendar cal = Calendar.getInstance();
                cal.setTime(end);
                cal.add(Calendar.DATE, 1);
                end = cal.getTime();
                query.addCriteria(Criteria.where("createTime").gte(start).lt(end));
            } catch (Exception e) {
                log.error("日期解析失败", e);
            }
        }
        if (Common.isNotEmpty(workerId)) query.addCriteria(Criteria.where("worker.$id").is(new ObjectId(workerId)));
        if (status != null) query.addCriteria(Criteria.where("status").is(status));
        if (Common.isNotEmpty(search)) {
            List<ObjectId> findIdsBySearch = this.wxReporterService.findIdsBySearch(search);
            query.addCriteria(Criteria.where("workOrderNumber").regex(search, "i")
                    .orOperator(Criteria.where("faultInformation").regex(search, "i"),
                            Criteria.where("urgencyLevel").regex(search, "i"),
                            Criteria.where("reportClassroomRepair").regex(search, "i"),
                            Criteria.where("equipmentRepair").regex(search, "i"),
                            Criteria.where("wxReporter.$id").in(findIdsBySearch)));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        return this.find(query, WxRepair.class);
    }

    @Override
    public WxRepair cancelAssign(String repairId) {
        WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
        if (wxRepair == null) return null;
        if (wxRepair.getStatus() != RepairStatus.ASSIGNED.getCode()) throw new RuntimeException("仅已分配状态可取消");
        wxRepair.setWorker(null);
        wxRepair.setFindReadTime(null);
        wxRepair.setStatus(RepairStatus.PENDING.getCode());
        wxRepair.setAssignTime(null);
        this.save(wxRepair);
        return wxRepair;
    }

    @Override
    public WxRepair cancelRepair(String repairId) {
        WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
        if (wxRepair == null) return null;
        if (wxRepair.getStatus() == RepairStatus.COMPLETED.getCode()) throw new RuntimeException("已完成无法取消");
        wxRepair.setWorker(null);
        wxRepair.setStatus(RepairStatus.CANCELLED.getCode());
        this.save(wxRepair);
        return wxRepair;
    }

    @Override
    public WxRepair cancelRepairToComplete(String repairId, String workDept) {
        WxRepair wxRepair = this.findOneById(repairId, WxRepair.class);
        if (wxRepair == null) return null;
        if (wxRepair.getStatus() == RepairStatus.COMPLETED.getCode()) throw new RuntimeException("已完成");
        wxRepair.setWorker(null);
        wxRepair.setWorkDept(workDept);
        wxRepair.setStatus(RepairStatus.COMPLETED.getCode());
        this.save(wxRepair);
        return wxRepair;
    }

    @Override
    public void batchInsert(List<WxRepair> list) {
        if (list == null || list.isEmpty()) return;
        for (WxRepair r : list) super.insert(r);
    }

    @Override
    public ProcessInfo findproInfo(HttpServletRequest request) {
        try {
            return (ProcessInfo) request.getSession().getAttribute("proInfo");
        } catch (Exception e) {
            return new ProcessInfo();
        }
    }

    // ====================== 修复：POI导入（按模板列顺序+GeneralBean时间+状态映射） ======================
    @Override
    @SystemServiceLog(description = "上传报修单Excel")
    public String upload(HttpServletRequest request, HttpSession session) {
        String errorMsg = "";
        try {
            // 上传路径（和你项目CommonRepairItem保持一致）
            String uploadPath = File.separator + "FileUpload" + File.separator + "wxRepair";
            String[] allowFileTypes = {"xls", "xlsx"}; // 允许的文件类型

            // 调用项目通用文件上传工具
            List<Map<String, Object>> uploadResult = FileOperateUtil.upload(request, uploadPath, allowFileTypes);
            if (uploadResult == null || uploadResult.isEmpty()) {
                return "<span class='entypo-attention'></span>文件上传失败：未获取到上传文件</br>";
            }

            // 校验文件格式
            boolean hasValidSuffix = (Boolean) uploadResult.get(0).get("hassuffix");
            if (!hasValidSuffix) {
                return "<span class='entypo-attention'></span>文件格式错误：仅支持xls/xlsx格式</br>";
            }

            // 读取上传后的文件路径
            String filePath = (String) uploadResult.get(0).get("savepath");
            File excelFile = new File(filePath);
            // 执行批量导入
            errorMsg = batchImport(excelFile, session);

        } catch (Exception e) {
            log.error("报修单文件上传异常", e);
            errorMsg = "<span class='entypo-attention'></span>文件上传异常：" + e.getMessage().replaceAll("<|>", "") + "</br>";
        }
        return errorMsg;
    }

    private String batchImport(File file, HttpSession session) {
        StringBuilder err = new StringBuilder();
        String[][] data = null;
        try {
            data = ExcelReadUtil.readExcel(file, 0);
        } catch (IOException e) {
            return "<span>Excel读取失败：" + e.getMessage() + "</span>";
        }
        if (data == null || data.length <= 1) {
            return "<span>无有效数据</span>";
        }

        ProcessInfo info = new ProcessInfo();
        info.allnum = data.length - 1;
        session.setAttribute("proInfo", info);

        int success = 0, fail = 0;
        List<WxRepair> batch = new ArrayList<>(100);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 1; i < data.length; i++) {
            int row = i + 1;
            info.nownum = i;
            info.lastnum = data.length - 1 - i;
            session.setAttribute("proInfo", info);

            String[] rowData = data[i];
            if (rowData == null || rowData.length < 13) {
                fail++;
                err.append("第").append(row).append("行：列数不足，跳过<br>");
                continue;
            }

            try {
                // ========== 严格按你给的真实顺序 ==========
                String repairDateStr = getCell(rowData, 0); // 报修日期
                String schoolName = getCell(rowData, 1);
                String userName = getCell(rowData, 2);
                String contactNumber = getCell(rowData, 3);
                String schoolAddress = getCell(rowData, 4);
                String reportClassroom = getCell(rowData, 5);
                String equipmentRepair = getCell(rowData, 6);
                String faultInfo = getCell(rowData, 7);
                String repairContent = getCell(rowData, 8);
                String workerName = getCell(rowData, 9);
                String planDateStr = getCell(rowData, 10);
                String completeTimeStr = getCell(rowData, 11);
                String completeStatus = getCell(rowData, 12);

                // 空值兜底
                schoolName = StringUtils.isBlank(schoolName) ? "未知学校" : schoolName;
                userName = StringUtils.isBlank(userName) ? "未知报修人" : userName;
                contactNumber = StringUtils.isBlank(contactNumber) ? "未知电话" : contactNumber;
                schoolAddress = StringUtils.isBlank(schoolAddress) ? "未知地址" : schoolAddress;
                reportClassroom = StringUtils.isBlank(reportClassroom) ? "未知教室" : reportClassroom;
                equipmentRepair = StringUtils.isBlank(equipmentRepair) ? "未知设备" : equipmentRepair;
                faultInfo = StringUtils.isBlank(faultInfo) ? "无故障描述" : faultInfo;
                repairContent = StringUtils.isBlank(repairContent) ? "无维修内容" : repairContent;

                // 报修日期 → createTime（格式修复）
                Date createTime = new Date();
                if (StringUtils.isNotBlank(repairDateStr)) {
                    try {
                        createTime = sdf.parse(repairDateStr);
                    } catch (Exception e) {
                        // 格式错误用当前时间
                    }
                }

                // 状态映射：脱期/进行中 → 待完成(1)
                Integer status = 1;
                if ("已完成".equals(completeStatus) || "延期完成".equals(completeStatus)) {
                    status = 4;
                } else if ("脱期".equals(completeStatus) || "进行中".equals(completeStatus)) {
                    status = 1;
                }

                WxRepair repair = new WxRepair();
                repair.setWorkOrderNumber(Common.generateWorkOrderNumber());
                repair.setCreateTime(createTime); // 报修日期赋值到createTime
                repair.setReportClassroomRepair(reportClassroom);
                repair.setEquipmentRepair(equipmentRepair);
                repair.setFaultInformation(faultInfo);
                repair.setRepairContent(repairContent);
                repair.setUrgencyLevel("普通");
                repair.setStatus(status);
                repair.setIsDelete(false);

                // 报修人
                WxReporter reporter = new WxReporter();
                reporter.setSchoolName(schoolName);
                reporter.setUserName(userName);
                reporter.setContactNumber(contactNumber);
                reporter.setSchoolAddress(schoolAddress);
                reporter.setIsBlocked(false);
                repair.setWxReporter(reporter);

                // 维修人员
                if (StringUtils.isNotBlank(workerName)) {
                    WxBinding binding = wxBindingService.findByName(workerName);
                    if (binding != null) {
                        repair.setWorker(binding);
                        if (status == 4) {
                            repair.setAssignTime(null);
                        }
                    }
                }

                batch.add(repair);
                if (batch.size() >= 100) {
                    batchInsert(batch);
                    batch.clear();
                }
                success++;

            } catch (Exception e) {
                fail++;
                err.append("第").append(row).append("行：导入失败 → ").append(e.getMessage()).append("<br>");
            }
        }

        if (!batch.isEmpty()) {
            batchInsert(batch);
        }

        session.removeAttribute("proInfo");
        if (err.length() == 0) {
            return "<span>导入完成：成功 " + success + " 条</span>";
        } else {
            return err + "<hr>总计：成功" + success + "条，失败" + fail + "条";
        }
    }

    private String getCell(String[] arr, int idx) {
        if (idx >= arr.length) return "";
        return arr[idx] == null ? "" : arr[idx].trim();
    }
}