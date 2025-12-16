package zhongchiedu.wechat.controller.wxrepair;

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.common.service.WxOAuth2Service;
import me.chanjar.weixin.mp.api.WxMpService;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.enums.PersonJoinAuditStatusEnum;
import zhongchiedu.common.utils.enums.PersonnelType;
import zhongchiedu.inventory.pojo.AfterSalesProjects;
import zhongchiedu.inventory.pojo.WxBinding;
import zhongchiedu.inventory.pojo.WxRepair;
import zhongchiedu.inventory.service.AfterSalesProjectsService;
import zhongchiedu.inventory.service.WxBindingService;
import zhongchiedu.inventory.service.WxRepairService;
import zhongchiedu.wx.config.WxMpProperties;

/**
 * 报修单相关页面跳转Controller
 */
@Slf4j
@Controller
@RequestMapping("/wechatrp") // 与微信推送链接中的路径保持一致
public class WxRepairDetailController {

	// 注入报修单服务（用于查询报修单数据）
	@Autowired
	private WxRepairService wxRepairService;
	@Autowired
	private AfterSalesProjectsService afterSalesProjectsService;

	@Autowired
	private WxBindingService wxBindingService;

	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private WxMpProperties wxMpProperties;

	@Value("${qrcode.weburl}")
	private String weburl;

	/**
	 * 跳转至报修单详情页面 接收微信消息中携带的报修单ID（路径参数）
	 * 
	 * @param id    报修单ID（从微信消息跳转链接中获取）
	 * @param model 用于向前端页面传递数据
	 * @return 报修单详情页面路径（templates/school/repair_detail.html）
	 */
	@GetMapping("/findWxRepair/{id}")
	public String toRepairDetail(@PathVariable("id") String id, Model model, HttpSession session,
			HttpServletRequest request) {
		try {

			String openId = (String) session.getAttribute("openId");
			if (openId == null) {
				// 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
				String code = request.getParameter("code");

				if (code == null) {
					// 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
					String redirect_uri = weburl + "/wechatrp/findWxRepair/" + id;
					return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
							+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri="
							+ URLEncoder.encode(redirect_uri, "UTF-8")
							+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				}

				// 使用 code 调用微信接口获取 openId
				WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
				WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
				openId = accessToken.getOpenId();

				// 3. 将获取到的 openId 存入 session，以便后续请求使用
				session.setAttribute("openId", openId);
			
			} else {
				System.out.println("openId 已存在于 session 中，直接使用: " + openId);

			}
			
			// 根据OpenId查看是否有调度权限
			WxBinding findWxBindingByOpenId = this.wxBindingService.findWxBindingByOpenId(openId);
			if (Common.isEmpty(findWxBindingByOpenId)
					|| !findWxBindingByOpenId.getAuditStatus().equals(PersonJoinAuditStatusEnum.APPROVED.getCode())
					|| !findWxBindingByOpenId.getPersonnelType().equals(PersonnelType.DISPATCHER.getCode())) {
				// 判断findWxBindingByOpenId 状态 不为空 必须是审核通过和人员类别为调度才能访问
				System.out.println("非调度人员访问！");
				return "school/error"; // 跳转至自定义错误页面
			}
			// 1. 根据ID查询报修单完整信息（需关联查询报修人、图片等关联数据）
			// 注：确保WxRepairService的getById方法已实现关联查询（如MyBatis的关联查询/MP的级联查询）
			WxRepair wxRepair = this.wxRepairService.findOneById(id, WxRepair.class);
			// 2. 校验报修单是否存在
			if (wxRepair == null) {
				log.warn("报修单ID[{}]不存在，跳转至错误页面", id);
				return "school/error"; // 跳转至自定义错误页面
			}

			// 3. 将报修单数据传递给前端页面（前端通过${repair}获取）
			model.addAttribute("repair", wxRepair);
			model.addAttribute("openId", openId);

			// 4. 返回报修单详情页面（视图路径：templates/school/repair_detail.html）
			return "school/repairDetailDiaodu";

		} catch (Exception e) {
			log.error("跳转报修单详情页面失败，报修单ID[{}]，异常信息：", id, e);
			return "school/error"; // 异常时跳转至错误页面
		}
	}

	@GetMapping("/findWxRepairByWorker/{id}")
	public String toRepairDetailByWorker(@PathVariable("id") String id, Model model, HttpSession session,
			HttpServletRequest request) {

		try {
			String openId = (String) session.getAttribute("openId");
			if (openId == null) {
				// 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
				String code = request.getParameter("code");

				if (code == null) {
					// 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
					String redirect_uri = weburl + "/wechatrp/findWxRepairByWorker/" + id;
					return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
							+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri="
							+ URLEncoder.encode(redirect_uri, "UTF-8")
							+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				}

				// 使用 code 调用微信接口获取 openId
				WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
				WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
				openId = accessToken.getOpenId();

				// 3. 将获取到的 openId 存入 session，以便后续请求使用
				session.setAttribute("openId", openId);
			} else {
				System.out.println("openId 已存在于 session 中，直接使用: " + openId);

			}
			// 根据OpenId查看是否有调度权限
			WxBinding findWxBindingByOpenId = this.wxBindingService.findWxBindingByOpenId(openId);
			if (Common.isEmpty(findWxBindingByOpenId)
					|| !findWxBindingByOpenId.getAuditStatus().equals(PersonJoinAuditStatusEnum.APPROVED.getCode())
					|| !findWxBindingByOpenId.getPersonnelType().equals(PersonnelType.CONSTRUCTION_TEAM.getCode())) {
				// 判断findWxBindingByOpenId 状态 不为空 必须是审核通过和人员类别为调度才能访问
				System.out.println("非维修人员访问！");
				return "school/error"; // 跳转至自定义错误页面
			}

			model.addAttribute("openId", openId);

			// 1. 根据ID查询报修单完整信息（需关联查询报修人、图片等关联数据）
			// 注：确保WxRepairService的getById方法已实现关联查询（如MyBatis的关联查询/MP的级联查询）
			WxRepair wxRepair = this.wxRepairService.findOneById(id, WxRepair.class);
			//如果维修人员发送了变更
			if(Common.isEmpty(wxRepair.getWorker())) {
				log.warn("维修人员[{}]不存在，跳转至错误页面", id);
				return "school/error"; // 跳转至自定义错误页面
			}
			if(!wxRepair.getWorker().getOpenId().equals(openId)) {
				//维修人员ID不匹配
				return "school/error"; // 跳转至自定义错误页面
			}
			
			// 2. 校验报修单是否存在
			if (wxRepair == null) {
				log.warn("报修单ID[{}]不存在，跳转至错误页面", id);
				return "school/error"; // 跳转至自定义错误页面
			}

			// 3. 将报修单数据传递给前端页面（前端通过${repair}获取）
			model.addAttribute("repair", wxRepair);

			// 4. 返回报修单详情页面（视图路径：templates/school/repair_detail.html）
			return "school/repairDetailWorker";

		} catch (Exception e) {
			log.error("跳转报修单详情页面失败，报修单ID[{}]，异常信息：", id, e);
			return "school/error"; // 异常时跳转至错误页面
		}
	}

	/**
	 * 跳转至选择施工队人员页面
	 * 
	 * @param repairId 报修单 ID
	 * @param model    用于传递数据到前端
	 * @return 选择施工队页面
	 */
	@GetMapping("/getSGD")
	public String getSGDPage(@RequestParam("repairId") String repairId, Model model, HttpSession session,
			HttpServletRequest request) {
		try {
			log.info("为报修单[{}]分配处理人员，查询施工队列表", repairId);

			String openId = (String) session.getAttribute("openId");
			if (openId == null) {
				// 2. 如果 session 中没有，则说明是第一次请求，需要用 code 获取
				String code = request.getParameter("code");

				if (code == null) {
					// 如果连 code 都没有，说明是未授权的访问，重定向到授权页面
					String redirect_uri = weburl + "/wechatrp/getSGD?repairId=" + repairId;
					return "redirect:https://open.weixin.qq.com/connect/oauth2/authorize?" + "appid="
							+ wxMpProperties.getConfigs().get(0).getAppId() + "&redirect_uri="
							+ URLEncoder.encode(redirect_uri, "UTF-8")
							+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
				}

				// 使用 code 调用微信接口获取 openId
				WxOAuth2Service oAuth2Service = this.wxMpService.getOAuth2Service();
				WxOAuth2AccessToken accessToken = oAuth2Service.getAccessToken(code);
				openId = accessToken.getOpenId();

				// 3. 将获取到的 openId 存入 session，以便后续请求使用
				session.setAttribute("openId", openId);
			
			} else {
				System.out.println("openId 已存在于 session 中，直接使用: " + openId);

			}
			// 根据OpenId查看是否有调度权限
			WxBinding findWxBindingByOpenId = this.wxBindingService.findWxBindingByOpenId(openId);
			if (Common.isEmpty(findWxBindingByOpenId)
					|| !findWxBindingByOpenId.getAuditStatus().equals(PersonJoinAuditStatusEnum.APPROVED.getCode())
					|| !findWxBindingByOpenId.getPersonnelType().equals(PersonnelType.DISPATCHER.getCode())) {
				// 判断findWxBindingByOpenId 状态 不为空 必须是审核通过和人员类别为调度才能访问
				System.out.println("非调度人员访问！");
				return "school/error"; // 跳转至自定义错误页面
			}
			// 1. 从 WxBinding 表中查询所有施工队人员
			// 假设你的 WxBinding 表中有一个字段（如 personnelType）用于标识用户类型
			// 0: 普通用户, 1: 调度员, 2: 施工队人员 (请根据你的实际情况修改)
			List<WxBinding> findBindingsByPersonnelType = this.wxBindingService
					.findBindingsByPersonnelType(PersonnelType.CONSTRUCTION_TEAM, PersonJoinAuditStatusEnum.APPROVED);

			// 2. 将施工队人员列表和报修单 ID 传递给前端页面
			model.addAttribute("openId", openId);
			model.addAttribute("workers", findBindingsByPersonnelType);
			model.addAttribute("repairId", repairId); // 将报修单ID也传递过去，用于后续提交分配
			
		    List<AfterSalesProjects> findAllinServiceProj = this.afterSalesProjectsService.findAllinServiceProj();
	        model.addAttribute("projs", findAllinServiceProj);

			// 3. 返回选择施工队的页面
			return "school/select_worker";

		} catch (Exception e) {
			log.error("查询施工队人员列表失败: ", e);
			// 可以跳转到一个错误提示页面
			return "error";
		}
	}

}