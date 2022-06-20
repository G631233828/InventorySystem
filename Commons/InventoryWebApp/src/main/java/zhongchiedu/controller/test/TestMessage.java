package zhongchiedu.controller.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.MatrixToImageWriter;
import zhongchiedu.controller.inventory.StockController;
import zhongchiedu.framework.pagination.Pagination;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.pojo.Stock;
import zhongchiedu.log.annotation.SystemControllerLog;

@Controller
@Slf4j
public class TestMessage {
	
	
	
	@GetMapping("test")
	public void sss(HttpServletRequest request, HttpServletResponse response) {
        String sss = "郭建波/0/0000000000000000000/310230199110084755";
        try {
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Type", "text/html; charset=utf-8");
//            response.setHeader("Content-Type","text/html");
            PrintWriter writer = response.getWriter();
            writer.write(sss.toCharArray());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
	
	

}
