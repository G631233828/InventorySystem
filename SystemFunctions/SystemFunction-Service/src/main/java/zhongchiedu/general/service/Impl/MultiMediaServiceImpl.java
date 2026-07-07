package zhongchiedu.general.service.Impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Contents;
import zhongchiedu.common.utils.FileOperateUtil;
import zhongchiedu.common.utils.ImageTool;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.general.pojo.MultiMedia;
import zhongchiedu.general.service.MultiMediaService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Slf4j
@Service
public class MultiMediaServiceImpl extends GeneralServiceImpl<MultiMedia> implements MultiMediaService {

    @Autowired
    private FileOperateUtil fileOperateUtil;

    @Autowired
    private ImageTool imageTool;

    /**
     * 上传图片（自定义宽高压缩）
     */
    @Override
    public List<MultiMedia> uploadPictures(MultipartFile[] file, String dir, String path, String belong, int width, int height) {
        List<MultiMedia> list = new ArrayList<>();
        if (file == null || file.length <= 0 || !Common.isNotEmpty(file[0].getOriginalFilename())) {
            return list;
        }

        DecimalFormat d = new DecimalFormat("#.00");
        String uploadPath = dir + path;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile m : file) {
            MultiMedia multi = new MultiMedia();
            byte[] fileBytes;
            try {
                // 修复：不调用m.getBytes()，直接读取输入流写入内存，脱离tomcat临时tmp文件
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    FileCopyUtils.copy(m.getInputStream(), bos);
                    fileBytes = bos.toByteArray();
                }
            } catch (IOException e) {
                log.error("读取图片文件字节失败,文件名:{}", m.getOriginalFilename(), e);
                continue;
            }

            // 使用字节数组上传方法
            Map<String, Object> map = fileOperateUtil.uploadByBytes(fileBytes, uploadPath, m.getOriginalFilename());
            // 判断上传失败直接跳过
            if (Boolean.TRUE.equals(map.get(Contents.ERROR))) {
                log.error("图片文件上传失败,文件名:{}", m.getOriginalFilename());
                continue;
            }

            String compName = map.get(Contents.FILENAME).toString();
            String com_Name = "comp_" + compName;
            imageTool.compressPic(uploadPath, uploadPath, compName, com_Name, width, height, true);

            multi.setCompressPicName(com_Name);
            multi.setOriginalName(m.getOriginalFilename());
            multi.setGenerateName(compName);
            multi.setSavePath(path);
            multi.setDir(dir);
            multi.setThumbnail(null);
            multi.setExtension(map.get(Contents.SUFFIXNAME).toString());
            multi.setFileType("IMG");

            // 文件大小格式化
            long size = fileBytes.length;
            String sizeStr;
            double fileSize;
            if (size < 1024) {
                sizeStr = "B";
                fileSize = size;
            } else if (size < 1024 * 1024) {
                sizeStr = "KB";
                fileSize = size / 1024.0;
            } else if (size < 1024L * 1024 * 1024) {
                sizeStr = "MB";
                fileSize = size / (1024.0 * 1024);
            } else {
                sizeStr = "GB";
                fileSize = size / (1024.0 * 1024 * 1024);
            }
            multi.setFileSize(d.format(fileSize));
            multi.setFileSizeStr(sizeStr);
            multi.setBelong(belong);
            this.insert(multi);
            list.add(multi);
        }
        return list;
    }

    /**
     * 上传图片（默认压缩尺寸 640*320）
     */
    @Override
    public List<MultiMedia> uploadPictures(MultipartFile[] file, String dir, String path, String belong) {
        return uploadPictures(file, dir, path, belong, 640, 320);
    }

    /**
     * 上传视频文件
     */
    @Override
    public MultiMedia uploadVideo(MultipartFile m, String dir, String path, String belong) {
        MultiMedia multi = null;
        if (m == null || !Common.isNotEmpty(m.getOriginalFilename())) {
            return multi;
        }
        DecimalFormat d = new DecimalFormat("#.00");
        multi = new MultiMedia();
        String uploadPath = dir + path;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        byte[] fileBytes;
        try {
            // 修复：读取流复制到内存，不依赖磁盘临时文件
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                FileCopyUtils.copy(m.getInputStream(), bos);
                fileBytes = bos.toByteArray();
            }
        } catch (IOException e) {
            log.error("读取视频文件字节失败,文件名:{}", m.getOriginalFilename(), e);
            return null;
        }

        Map<String, Object> map = fileOperateUtil.uploadByBytes(fileBytes, uploadPath, m.getOriginalFilename());
        if (Boolean.TRUE.equals(map.get(Contents.ERROR))) {
            log.error("视频文件上传失败,文件名:{}", m.getOriginalFilename());
            return null;
        }

        multi.setOriginalName(m.getOriginalFilename());
        multi.setGenerateName(map.get(Contents.FILENAME).toString());
        multi.setSavePath(path);
        multi.setDir(dir);
        multi.setThumbnail(null);
        multi.setExtension(map.get(Contents.SUFFIXNAME).toString());
        multi.setFileType("Video");

        long size = fileBytes.length;
        String sizeStr;
        double fileSize;
        if (size < 1024) {
            sizeStr = "B";
            fileSize = size;
        } else if (size < 1024 * 1024) {
            sizeStr = "KB";
            fileSize = size / 1024.0;
        } else if (size < 1024L * 1024 * 1024) {
            sizeStr = "MB";
            fileSize = size / (1024.0 * 1024);
        } else {
            sizeStr = "GB";
            fileSize = size / (1024.0 * 1024 * 1024);
        }
        multi.setFileSize(d.format(fileSize));
        multi.setFileSizeStr(sizeStr);
        multi.setBelong(belong);
        this.insert(multi);
        return multi;
    }

    @Override
    public List<MultiMedia> findInIds(List<String> ids) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(ids));
        return this.find(query, MultiMedia.class);
    }

    private Lock lock = new ReentrantLock();

    @Override
    public void deleteMultiMedia(String id, String type) {
        try {
            lock.lock();
            MultiMedia multiMedia = this.findOneById(id, MultiMedia.class);
            if (Common.isNotEmpty(multiMedia)) {
                String dir = multiMedia.getDir();
                String savePath = multiMedia.getSavePath();
                List<String> deleteFile = new ArrayList<>();
                if ("VIDEO".equals(type)) {
                    String originalName = dir + savePath + "/real/" + multiMedia.getOriginalName();
                    String encodingPath = dir + multiMedia.getEncodingPath() + multiMedia.getGenerateName();
                    String thumbnaiName = dir + multiMedia.getThumbnail() + multiMedia.getThumbnailName();
                    deleteFile.add(originalName);
                    deleteFile.add(encodingPath);
                    deleteFile.add(thumbnaiName);
                } else if ("IMG".equals(type)) {
                    String generateName = dir + savePath + "/" + multiMedia.getGenerateName();
                    String compressPicName = dir + savePath + "/" + multiMedia.getCompressPicName();
                    deleteFile.add(generateName);
                    deleteFile.add(compressPicName);
                }
                Common.deleteFile(deleteFile, dir + savePath + "/");
            }
            multiMedia.setIsDelete(true);
            this.save(multiMedia);
        } catch (Exception e) {
            log.error("删除多媒体文件异常", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public MultiMedia saveQrCode(File file, String dir, String path, String type) {
        DecimalFormat d = new DecimalFormat("#.00");
        MultiMedia multi = new MultiMedia();
        multi.setOriginalName(file.getName());
        multi.setSavePath(path);
        multi.setDir(dir);
        multi.setBelong(type);
        multi.setExtension(Common.getSuffix(file.getName()));
        multi.setFileType("QRCODE");
        long size = file.length();
        String sizeStr;
        double fileSize;
        if (size < 1024) {
            sizeStr = "B";
            fileSize = size;
        } else if (size < 1024 * 1024) {
            sizeStr = "KB";
            fileSize = size / 1024.0;
        } else if (size < 1024L * 1024 * 1024) {
            sizeStr = "MB";
            fileSize = size / (1024.0 * 1024);
        } else {
            sizeStr = "GB";
            fileSize = size / (1024.0 * 1024 * 1024);
        }
        multi.setFileSize(d.format(fileSize));
        multi.setFileSizeStr(sizeStr);
        this.insert(multi);
        return multi;
    }

    @Override
    public void editMultiMedia(String id, MultiMedia multiMedia) {
        if (Common.isNotEmpty(id) && Common.isNotEmpty(multiMedia)) {
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(new ObjectId(id)));
            query.addCriteria(Criteria.where("isDisable").is(false));
            query.addCriteria(Criteria.where("isDelete").is(false));
            MultiMedia multi = this.findOneByQuery(query, MultiMedia.class);
            if (!Common.isEmpty(multi)) {
                multi.setAuthor(multiMedia.getAuthor());
                multi.setTitle(multiMedia.getTitle());
                this.save(multi);
            }
        }
    }

    @Override
    @SystemServiceLog(description = "分页查询多媒体资源信息")
    public Pagination<MultiMedia> findMultiMediaPagination(Integer pageNo, Integer pageSize, String fileType) {
        Pagination<MultiMedia> pagination = null;
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isDelete").is(false));
            if (fileType != null && !fileType.isEmpty()) {
                query.addCriteria(Criteria.where("fileType").is(fileType));
            }
            pagination = this.findPaginationByQuery(query, pageNo, pageSize, MultiMedia.class);
            if (pagination == null) {
                pagination = new Pagination<>();
            }
            return pagination;
        } catch (Exception e) {
            log.error("分页查询多媒体资源失败", e);
        }
        return pagination;
    }
}