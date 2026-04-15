package zhongchiedu.wechat.controller.lihua;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.inventory.pojo.AttendanceManagement;
import zhongchiedu.inventory.pojo.School;
import zhongchiedu.inventory.pojo.SignTask;
import zhongchiedu.inventory.service.AttendanceManagementService;
import zhongchiedu.inventory.service.SchoolService;
import zhongchiedu.inventory.service.SignTaskService;
import zhongchiedu.wx.config.WxMpProperties;

@Controller
@RequestMapping("/wechatrp")
@CrossOrigin
@Slf4j
public class SignInController {

	@Value("${upload-dir}")
	private String dir;

	@Autowired
	private AttendanceManagementService attendanceManagementService;

	@Autowired
	private WxMpProperties wxMpProperties;

	@Value("${qrcode.weburl}")
	private String weburl;

	@Value("${upload-imgpath}")
	private String imgPath;

	@Autowired
	private WxMpService wxMpService;
	
	@Autowired 
	private SchoolService schoolService;
	
	@Autowired 
	private SignTaskService signTaskService;
	
	@GetMapping({ "/signInPage", "/signIn" })
	public String toSignInPage(HttpServletRequest request, Model model, HttpSession session) {
		try {
			String openId = (String) session.getAttribute("openId");
			if (Common.isEmpty(openId)) {
				String code = request.getParameter("code");
				if (Common.isEmpty(code)) {
					String redirect_uri = weburl + "/wechatrp/signInPage";
					return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?"
							+ "appid=" + wxMpProperties.getConfigs().get(0).getAppId()
							+ "&redirect_uri=" + java.net.URLEncoder.encode(redirect_uri, "UTF-8")
							+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				}
				WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
				WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
				openId = accessToken.getOpenId();
				session.setAttribute("openId", openId);
				WxOAuth2UserInfo userInfo = oAuth2Service.getUserInfo(accessToken, null);
				model.addAttribute("userInfo", userInfo);
			}
			model.addAttribute("openId", openId);

			// ======================================
			// 1. 先查：是否有【有效签到任务】
			// ======================================
			boolean hasValidTask = hasValidSignTask();
			if (!hasValidTask) {
				return "lihua/noTask"; // 无任务页面
			}

			// ======================================
			// 2. 再查：今日是否已签到
			// ======================================
			// 今日签到记录（只查一次数据库）
			AttendanceManagement todayRecord = attendanceManagementService.getTodaySignRecord(openId);

			// 如果不为null → 今天已签到
			if (todayRecord != null) {
			    // 直接回显历史问题
			    model.addAttribute("problems", todayRecord.getProblems());
			    return "lihua/signSuccess";
			}

		} catch (WxErrorException | java.io.UnsupportedEncodingException e) {
			if (e instanceof WxErrorException
					&& ((WxErrorException) e).getError().getErrorCode() == 40163) {
				session.invalidate();
				return "redirect:/wechatrp/signInPage";
			}
			e.printStackTrace();
		}
		return "lihua/signin";
	}

	// ======================================
	// 判断：当前是否有【时间范围内】的签到任务（取第一条）
	// ======================================
	private boolean hasValidSignTask() {
	    try {
	        Query query = new Query(); 
	        // 直接调用，方法内部已经自动按时间过滤
	        List<SignTask> tasks = this.signTaskService.findListByQuery(query, SignTask.class);
	        
	        // 只要查到 >=1 条，说明有正在进行的任务
	        return tasks != null && !tasks.isEmpty();
	        
	    } catch (Exception e) {
	        log.error("查询签到任务异常", e);
	        return false;
	    }
	}

	@PostMapping("/sign")
	@ResponseBody
	public BasicDataResult submitSign(
	        @RequestParam("openId") String openId,
	        @RequestParam("name") String name,
	        @RequestParam("schoolId") String schoolId,
	        @RequestParam("address") String address,
	        @RequestParam(value = "problems", required = false) String problems,
	        @RequestParam(value = "photos", required = true) MultipartFile[] photos) {

	    try {
	    	
	        String addr = address;
	        String classRoom = "";
	        if (address.contains(",")) {
	            String[] arr = address.split(",");
	            addr = arr[0];       // 真实地址
	            classRoom = arr[1];  // 教室
	        }

	        AttendanceManagement attendance = new AttendanceManagement();
	        attendance.setOpenId(openId);
	        attendance.setName(name);
	        attendance.setProblems(problems);
	        attendance.setClassRoom(classRoom);
	        attendance.setAddress(addr);

	        School school = new School();
	        school.setId(schoolId);
	        attendance.setSchool(school);


	        String signTime = Common.getDateYMDHM(new Date());
	        attendance.setSignTime(signTime);

	        this.attendanceManagementService.saveSign(attendance, photos, imgPath, dir);

	        return BasicDataResult.build(200, "签到成功", null);

	    } catch (Exception e) {
	        log.error("签到真正错误：", e);  // 重要！后台能看到真实原因
	        return BasicDataResult.build(500, "后台异常：" + e.getMessage(), null);
	    }
	}
	
	// 搜索学校（模糊匹配）
	@GetMapping("/searchSchool")
	@ResponseBody
	public BasicDataResult searchSchool(@RequestParam(value = "keyword", defaultValue = "") String keyword) {
	    try {
	        List<School> allSchools = schoolService.findAllName(false);
	        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

	        List<School> result = allSchools.stream()
	                .filter(school -> {
	                    String sn = school.getSchoolName();
	                    return sn != null && sn.toLowerCase().contains(kw);
	                })
	                .collect(Collectors.toList());

	        return BasicDataResult.ok(result);

	    } catch (Exception e) {
	        log.error("学校搜索异常", e);
	        return BasicDataResult.build(500, "搜索异常", null);
	    }
	}
	
	
	
	
	@PostMapping("/saveProblem")
	@ResponseBody
	public BasicDataResult saveProblem(
	        @RequestParam String openId,
	        @RequestParam String content) {
	    try {
	        // 按 openId + 今日日期，找到今天的签到记录，更新问题
	        attendanceManagementService.updateTodayProblem(openId, content);
	        return BasicDataResult.ok("保存成功");
	    } catch (Exception e) {
	        log.error("保存问题失败", e);
	        return BasicDataResult.error("保存失败");
	    }
	}
	
	
	
	
	
	
	

}