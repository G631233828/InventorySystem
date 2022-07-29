package zhongchiedu.controller.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.aliyun.dingtalk.service.DingTalkUserService;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiCollectionFormListRequest;
import com.dingtalk.api.request.OapiCollectionInstanceListRequest;
import com.dingtalk.api.request.OapiKacDatavVideoliveDetailListRequest;
import com.dingtalk.api.request.OapiKacDatavVideoliveDetailListRequest.VideoLiveDetailRequest;
import com.dingtalk.api.response.OapiCollectionFormListResponse;
import com.dingtalk.api.response.OapiCollectionInstanceListResponse;
import com.dingtalk.api.response.OapiKacDatavVideoliveDetailListResponse;
import com.taobao.api.ApiException;
@Controller
public class DingdingTestController {

	@Autowired
    private DingTalkUserService dingTalkUserService;
	 @GetMapping("/testaaa")
	public String test() throws ApiException {
		 
		 
		 
		 
		 
		 DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/collection/form/list");
		 OapiCollectionFormListRequest req = new OapiCollectionFormListRequest();
		 req.setBizType(0L);
		 req.setCreator("016548280036435669");
		 req.setOffset(0L);
		 req.setSize(200L);
		 OapiCollectionFormListResponse rsp = client.execute(req, this.dingTalkUserService.getAccessToken());
		 System.out.println(rsp.getBody());
		 
		 
		 
		 DingTalkClient client2 = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/collection/instance/list");
		 OapiCollectionInstanceListRequest req2 = new OapiCollectionInstanceListRequest();
		 req2.setFormCode("PROC-3CAB258C-8D99-452D-A927-CF2C830477C5");
		 req2.setOffset(0L);
		 req2.setSize(10L);
		 req2.setBizType(0L);
		 OapiCollectionInstanceListResponse rsp2 = client2.execute(req2, this.dingTalkUserService.getAccessToken());
		 System.out.println(rsp2.getBody());
		
		 
//		form code  PROC-BB4DA2F7-D3D3-4B37-A8B5-EB5A2F76960F  OapiKacDatavVideoliveDetailListResponse rsp = client.execute(req,this.dingTalkUserService.getAccessToken() );
//		 System.out.println(rsp.getBody());
		
		return null;
	}
	
	
}
