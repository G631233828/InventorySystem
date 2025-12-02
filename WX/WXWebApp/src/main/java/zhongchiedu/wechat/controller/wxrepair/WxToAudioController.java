package zhongchiedu.wechat.controller.wxrepair;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/wechatrp")
public class WxToAudioController {



	
    /**
     * 跳转到绑定页面
     *
     * @param openId 从微信授权后获取的 openId
     * @param model  用于向页面传递数据
     * @return 绑定页面
     */
    @GetMapping("/toAudio")
    public String toBindingPage(HttpServletRequest request, Model model, HttpSession session) {
    	
        return "school/audio"; 
    }

    
}