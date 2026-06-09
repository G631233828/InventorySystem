package zhongchiedu.common.utils;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class OcrUtil {
    private static final String TESS_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";

    public static String recognize(MultipartFile file) throws Exception {
        File tempSource = File.createTempFile("src_", getSuffix(file.getOriginalFilename()));
        file.transferTo(tempSource);
        BufferedImage origin = ImageIO.read(tempSource);
        // 通用自适应裁剪（后续实拍单据通用）
        BufferedImage crop = origin.getSubimage((int)(origin.getWidth()*0.03),(int)(origin.getHeight()*0.22),
                (int)(origin.getWidth()*0.92),(int)(origin.getHeight()*0.55));
        BufferedImage bin = toBinary(crop);
        File tempOcr = File.createTempFile("ocr_",".png");
        ImageIO.write(bin,"png",tempOcr);

        Tesseract tess = new Tesseract();
        tess.setDatapath(TESS_PATH);
        tess.setLanguage("chi_sim+eng");
        tess.setOcrEngineMode(1);
        tess.setPageSegMode(6);
        tess.setTessVariable("user_defined_dpi","300");
        tess.setTessVariable("load_system_dawg","0");
        tess.setTessVariable("load_freq_dawg","0");

        String res = tess.doOCR(tempOcr).trim();
        tempSource.delete();
        tempOcr.delete();
        return res;
    }

    private static BufferedImage toBinary(BufferedImage src){
        int w = src.getWidth(),h=src.getHeight();
        BufferedImage gray = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g=gray.createGraphics();g.drawImage(src,0,0,null);g.dispose();
        int th=175;
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            int v=gray.getRGB(x,y)&0xff;
            gray.setRGB(x,y,v>th?Color.WHITE.getRGB():Color.BLACK.getRGB());
        }
        return gray;
    }
    private static String getSuffix(String name){
        return name.substring(name.lastIndexOf("."));
    }

    public static boolean isTriplicateReceipt(String ocrText) {
        if (ocrText == null || ocrText.isEmpty()) return false;
        String content = ocrText.replaceAll("\\s+", "");
        String[] keys = {"设备维修服务单", "维修服务单", "维修单", "报修单", "验收单", "三联单"};
        for (String k : keys) if(content.contains(k))return true;
        return false;
    }
}