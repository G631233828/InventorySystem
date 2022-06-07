package zhongchiedu.wx.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import net.sf.json.JSONObject;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.common.utils.Types.menuType;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.wx.config.WxMpProperties;
import zhongchiedu.wx.pojo.Button;
import zhongchiedu.wx.pojo.Menu;
import zhongchiedu.wx.pojo.WeChatMenu;
import zhongchiedu.wx.service.WeChatMenuService;

@Service
@Slf4j
public class WeChatMenuServiceImpl extends GeneralServiceImpl<WeChatMenu> implements WeChatMenuService {

	@Autowired
	private WxMpService wxMpService;
	@Autowired
	private  WxMpProperties properties;

	@Override
	public List<WeChatMenu> findWeChatMenus() {
		Query query = new Query();
		query.with(new Sort(Sort.Direction.ASC, "sort"));
		List<WeChatMenu> list = this.find(query, WeChatMenu.class);
		return list.size() > 0 ? list : null;
	}

	@Override
	public void saveOrUpdate(WeChatMenu weChatMenu) {

		if (Common.isNotEmpty(weChatMenu.getParentId())) {
			String parentId = weChatMenu.getParentId();
			if (Common.isNotEmpty(weChatMenu.getId())) {
				// 修改菜单
				WeChatMenu ed = this.findOneById(weChatMenu.getId(), WeChatMenu.class);
				BeanUtils.copyProperties(weChatMenu, ed);
				this.save(ed);
			} else {
				int size = this.getParentSize(parentId);
				if (!parentId.equals("0")) {
					// 添加子菜单
					if (size < 5) {
						weChatMenu.setParentId(parentId);
						WeChatMenu menu = new WeChatMenu();
						weChatMenu.setId(null);
						BeanUtils.copyProperties(weChatMenu, menu);
						this.insert(weChatMenu);
					}
				} else {
					// 添加父菜单
					if (size < 3) {
						weChatMenu.setParentId("0");
						WeChatMenu menu = new WeChatMenu();
						weChatMenu.setId(null);
						BeanUtils.copyProperties(weChatMenu, menu);
						this.insert(weChatMenu);
					}
				}
			}
		}

	}

	@Override
	public int getParentSize(String parentId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("parentId").is(parentId));
		List<WeChatMenu> list = this.find(query, WeChatMenu.class);
		return list.size();
	}

	@Override
	public BasicDataResult findWeChatMenuByid(String id) {
		if (Common.isEmpty(id)) {
			return BasicDataResult.build(400, "获取菜单信息失败", null);
		}

		WeChatMenu weChatMenu = this.findOneById(id, WeChatMenu.class);
		if (Common.isNotEmpty(weChatMenu)) {
			return BasicDataResult.build(200, "查询成功", weChatMenu);
		}
		return BasicDataResult.build(400, "获取菜单信息失败", null);
	}

	@Override
	public void deleteWeChatMenuById(String id) {
		WeChatMenu wechatMenu = this.findOneById(id, WeChatMenu.class);

		if (Common.isNotEmpty(wechatMenu)) {
			if (wechatMenu.getParentId().equals("0")) {
				Query query = new Query();
				query.addCriteria(Criteria.where("parentId").is(wechatMenu.getId()));
				List<WeChatMenu> list = this.find(query, WeChatMenu.class);
				if (list.size() > 0) {
					for (WeChatMenu menu : list) {
						this.remove(menu);
					}
				}
			}
			this.remove(wechatMenu);
		}
	}

	@Override
	public BasicDataResult release() {
		final List<WxMpProperties.MpConfig> configs = this.properties.getConfigs();

		if (Common.isEmpty(configs)) {
			return BasicDataResult.build(203, "请先配置appId,appSecretId", null);
		}
		String jsonMenu = JSONObject.fromObject(this.getMenu()).toString();
		try {
			String result = this.wxMpService.getMenuService().menuCreate(jsonMenu);
			if (Common.isEmpty(result)) {
				return BasicDataResult.build(200, "菜单发布成功", null);
			} else {
				return BasicDataResult.build(400, "菜单发布失败，请联系管理员", null);
			}
		} catch (WxErrorException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Menu getMenu() {
		Menu menu = new Menu();
		// 父按钮
		List listfb = new ArrayList();

		Query query = new Query();
		query.addCriteria(Criteria.where("parentId").is("0")).with(new Sort(Sort.Direction.ASC, "sort"));
		List<WeChatMenu> listmenu = this.find(query, WeChatMenu.class);
		for (WeChatMenu wechat : listmenu) {
			Button supB = new Button();
			// 获得所有parentId为0的wechatMenu
			query = new Query();
			query.addCriteria(Criteria.where("parentId").is(wechat.getId())).with(new Sort(Sort.Direction.ASC, "sort"));
			List<WeChatMenu> listSubMenu = this.find(query, WeChatMenu.class);

			if (listSubMenu.size() > 0) {
				// 获取所有子按钮
				List listsub = new ArrayList();
				for (WeChatMenu sub : listSubMenu) {
					// 父按钮有子菜单
					Button bt = new Button();
					bt.setName(sub.getName());
					bt.setType(sub.getType());
					if (sub.getType().equals(menuType.click)) {
						bt.setKey(sub.getKey());
					} else if (sub.getType().equals(menuType.view)) {
						bt.setUrl(sub.getUrl());
					}
					listsub.add(bt);
				}
				supB.setName(wechat.getName());
				supB.setSub_button(listsub);
				listfb.add(supB);
			} else {
				// 父按钮没有子菜单
				Button bt = new Button();
				bt.setName(wechat.getName());
				bt.setType(wechat.getType());
				if (wechat.getType().equals(menuType.click)) {
					bt.setKey(wechat.getKey());
				} else if (wechat.getType().equals(menuType.view)) {
					bt.setUrl(wechat.getUrl());
				}
				listfb.add(bt);
			}
		}
		menu.setButton(listfb);

		return menu;
	}

}
