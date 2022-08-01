function showColumn() {
	$("#mycolumn").modal('show');
}

function clearaa() {
	$("#start").val('');
	$("#end").val('');
	$("#serach").val('');

}

function searchVal() {
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();
	var type = $("#type").val();
	var searchArea = $("#searchArea").val();
	var searchAgent = $("#agent").val();

	/*
	 * if (search == null || search == "") { swal({ type : "warning", title :
	 * "", text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "stockStatisticss?pageSize=" + pageSize + "&search="
		+ search + "&start=" + start + "&end=" + end + "&type=" + type + "&searchArea=" + searchArea + "&searchAgent=" + searchAgent;
}
// function searchSize() {
//	
// var pageSize = $("#pageSize").val();
// var search = $("#serach").val();
// var start = $("#start").val();
// var end = $("#end").val();
// var type = $("#type").val();
// window.location.href="stockStatisticss?pageSize="+pageSize+"&search="+search+"&start="+start+"&end="+end+"&type="+type;
//	
// }


function toExport() {


	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();
	var type = $("#type").val();
	var areaId = $("#searchArea").val();
	var searchAgent = $("#agent").val();
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/export?search=" + search + "&start=" + start + "&end=" + end + "&type=" + type + "&areaId=" + areaId + "&searchAgent=" + searchAgent;

}


//生成出库单
function outboundOrder(o) {

jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在生成数据请稍等...",
		'closeTime': 5000,
	})
	window.location.href="stockStatistics/exportOutBoundOrder?id="+o;
}





//function checkdate(d1,d2){
//	if(d1 == d2 =="")return true;
//	
//	if(d1>d2){
//		jqueryAlert({
//		    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
//		    'content' : "日期开始时间不能大于结束时间",
//		    'closeTime' : 2000,
//		})
//		return false;
//	}else if(d1 == d2){
//		jqueryAlert({
//			'icon'    : getRootPath() +'/plugs/alert/img/error.png',
//			'content' : "两个日期不能相同",
//			'closeTime' : 2000,
//		})
//		return false;
//	}
//	
//	
//
//	
//}



function revoke(o) {

	var M = {

	}
	if (M.dialog3) {
		return M.dialog3.show();
	}
	M.dialog3 = jqueryAlert({
		'title': '撤销提醒',
		'content': '您当前正在做撤销的操作',
		'modal': true,
		'buttons': {
			'确定': function() {
				$.ajax({
					type: 'GET',
					url: 'stockStatistics/revoke',
					data: "id=" + o,
					dataType: 'json',
					success: function(data) {
						if (data.status == 200) {
							//							$("#revokeNum").text(data.data.revokeNum);
							$("#revoke" + o).css("color", "red");
							$("#revoke" + o).text("已撤销");
							$("#outbound"+o).hide();
							$("#qrcode"+o).hide();
							
							M.dialog3.close();
							jqueryAlert({
								'content': data.msg
							})
						} else {
							M.dialog3.close();
							jqueryAlert({
								'content': data.msg
							})
						}
					}
				})


			},
			'取消': function() {
				M.dialog3.close();
			}
		}
	})

}

//	
//
//	$(document).delegate(".btcancle", 'click', function() {



function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type: 'GET',
		url: 'stockStatistics/columns',
		data: "column=" + o + "&flag=" + flag,
		dataType: 'json',
		success: function(data) {
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

$(".datepicker").datepicker({
	language: "zh-CN",
	format: "yyyy-mm-dd 00:00:00",
	keyboardNavigation: false,
	forceParse: false,
	autoclose: true
});

// $("#data_1 .input-group.date").datepicker({
// keyboardNavigation : false,
// forceParse : false,
// autoclose : true
// });




function toDownloadQRcode(o) {
	if (o != "") {
		//下载单个
		window.location.href = "stockStatistics/downloadQRcode?id=" + o;
	} else {
		var a = $("input[name='ids']:checked").length;
		if (a == 0) {
			swal({
				type: "warning",
				title: "",
				text: "下载二维码至少选择一项!!",
			});
		} else {
			var str = "";
			var ids = $("input[name='ids']:checked");
			$(ids).each(function() {
				str += this.value + ",";
			});
			window.location.href = "stockStatistics/downloadQRcode?id=" + str;
		}



	}





}




















