
function searchVal() {

	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var searchArea = $("#searchArea").val();
	var status = $("#pickstatus").val();
/*
 * if (search == null || search == "") { swal({ type : "warning", title : "",
 * text : "查询内容不能为空!!", }); return ; }
 */
	window.location.href="pickUpApplications?pageSize="+pageSize+"&search="+search+"&searchArea="+searchArea+"&status="+status;

}


//function searchArea() {
//	
//	var pageSize = $("#pageSize").val();
//	var search = $("#serach").val();
//	window.location.href="stocks?pageSize="+pageSize+"&search="+search+"&searchArea="+searchArea;
//
//	
//}
//
//
//
//
//
//
//function searchSize() {
//	
//	var pageSize = $("#pageSize").val();
//	var search = $("#serach").val();
//	window.location.href="stocks?pageSize="+pageSize+"&search="+search;
//	
//}




function toStatistics(o){
	window.location.href = "stockStatisticss?id=" + o;
	
}

function toExport(){
	jqueryAlert({
	    'icon'    : getRootPath() +'/plugs/alert/img/right.png',
	    'content' : "正在导出请稍等...",
	    'closeTime' : 5000,
	})
	var areaId = $("#searchArea").val();
	
	
	window.location.href = "stock/export?areaId="+areaId;
	
	
}







function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type : 'GET',
		url : 'stock/columns',
		data : "column=" + o + "&flag=" + flag,
		dataType : 'json',
		success : function(data) {
			if (data.status == 200) {
				// $(td).attr("checked":flag);
				if (flag) {
					$(th).show();
				} else {
					$(th).hide();
				}
			}
		}
	})

}

$(document).ready( function() {
					if ($("#upload").val() != "") {

						$('#submit')
								.bind(
										'click',
										function() {
											var eventFun = function() {
												$
														.ajax({
															type : 'GET',
															url : 'stock/uploadprocess',
															data : {},
															dataType : 'json',
															success : function(
																	data) {
																$("#proBar")
																		.attr(
																				"style",
																				"width:"
																						+ (data.nownum / data.allnum)
																						* 100
																						+ '%');
																$('#proBar')
																		.css(
																				'aria-valuenow',
																				data.nownum
																						+ '%');
																$('#proBar')
																		.css(
																				'aria-valuemax',
																				data.allnum
																						+ '%');
																$('#proBartext')
																		.text(
																				"正在导入第"
																						+ data.nownum
																						+ "条记录，总共"
																						+ data.allnum
																						+ "条记录");
																if (data.nownum == data.allnum) {
																	window
																			.clearInterval(intId);
																}
															}
														});
											};
											var intId = window.setInterval(
													eventFun, 100);
										});

					}

					
					
					
					

					
					
					
					
				});





function cleanSearch(){
	window.location.href = "pickUpApplication/clearSearch";
}






function wechatPush(o){
	$.ajax({
		type : 'POST',
		url : "pickUpApplication/pickUpApplicationPush",
		dataType : "json",
		data: "id="+o,
		success : function(data) {
			if(data.status==200){
				jqueryAlert({
				    'icon'    : getRootPath() +'/plugs/alert/img/right.png',
				    'content' : data.data,
				    'closeTime' : 5000,
				})
			}else if(data.status==201){
				jqueryAlert({
				    'icon'    : getRootPath() +'/plugs/alert/img/warning.png',
				    'content' : data.data,
				    'closeTime' : 10000,
				})
			}else{
				jqueryAlert({
				    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
				    'content' : data.data,
				    'closeTime' : 5000,
				})
			}
			
			
			
		}
	});
}









