package zhongchiedu.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.inventory.pojo.Area;
import zhongchiedu.inventory.service.AreaService;
import zhongchiedu.log.annotation.SystemControllerLog;

/**
 * 
 * @author fliay
 *	区域管理
 *
 */
@Controller
@Slf4j
@RequestMapping("/api")
public class AreaApi {
	
	@Autowired
	private AreaService areaService;
	
	@GetMapping("/areas")
	@SystemControllerLog(description = "调用区域api")
	@ResponseBody
	public List<Area> findAreas(){
		List<Area> list = this.areaService.findAllArea(false);
		return list;
	}
	

}
