
$(function() {
	//入库

	// window.myInprice=inprice;
	//销售

	// window.sailPrice=sailprice;
	var inprice=$("#heji").text();
	var sailprice=$("#sailPrice").text();
	var inNum=$("#inNumHJ").text();
	var outNum=$("#outNumHJ").text();

	const checkboxs=document.querySelectorAll('input[name=ids]');
	let inpriceHJ=$("#heji");
	let sailPriceHJ=$("#sailPrice");
	let inNumHJ=$("#inNumHJ");
	let outNumHJ=$("#outNumHJ");
	function changeHeji(checkbox,total,windowparam,selector,param1) {
		const isckeck=$('input[type="checkbox"][name="ids"]:checked').length === 0;
		const isFirstCheck=$('input[type="checkbox"][name="ids"]:checked').length === 1;
		const id=checkbox.id;
		const paramId=$('#' + param1 + id);
		// const amount=Number(paramId.text());
		const amount=isNaN(Number(paramId.text()))?0:Number(paramId.text());
		if (checkbox.checked) {
			total= isFirstCheck?0:total;
			total += amount;
		} else {
			total -= amount;
			total = isckeck?windowparam:total;

		}
		selector.text(total)
		return total
	};

	var data = {
		inptotal: 0,
		sailtotal: 0,
		innumtotal:0,
		outnumtotal:0,
		baozhuang: function(checkbox) {
			var self = this;
			return () => {
				self.inptotal =changeHeji(checkbox, self.inptotal, inprice, inpriceHJ, "inprice_");
				self.sailtotal = changeHeji(checkbox, self.sailtotal, sailprice, sailPriceHJ, "sailPrice_");
				self.innumtotal =changeHeji(checkbox, self.innumtotal, inNum, inNumHJ, "inNum_");
				self.outnumtotal = changeHeji(checkbox, self.outnumtotal, outNum, outNumHJ, "outNum_");

			};
		}
	};
	checkboxs.forEach(function(checkbox) {
		checkbox.addEventListener('change', data.baozhuang(checkbox));
	});
	// function baozhuang(checkbox){
	// 	changeHeji(checkbox,window.total,window.myInprice,totalele,"inprice_")
	// 	changeHeji(checkbox,total1,window.sailPrice,total1sail,"sailPrice_")
	// }
	// checkboxs.forEach(function(checkbox) {
	// 	checkbox.addEventListener('change', function() {
	// 		baozhuang(checkbox);
	// 	});
	// });



	// checkboxs.forEach(checkbox => {
	// 	checkbox.addEventListener('change', function() {
	// 		const isckeck=$('input[type="checkbox"][name="ids"]:checked').length === 0;
	// 		const isFirstCheck=$('input[type="checkbox"][name="ids"]:checked').length === 1;
	// 			const id=checkbox.id;
	// 			const inpriceid=$('#inprice_' + id);
	// 			const amount=Number(inpriceid.text());
	//
	// 			const sailpriceid=$('#sailPrice_' + id);
	// 			const sailamount=Number(sailpriceid.text());
	// 			if (this.checked) {
	// 				total= isFirstCheck?0:total;
	// 				total += amount;
	//
	// 				total1= isFirstCheck?0:total1;
	// 				total1 += sailamount;
	// 			} else {
	// 				total -= amount;
	// 				total = isckeck?window.myInprice:total;
	//
	// 				total1 -= sailamount;
	// 				total1 = isckeck?window.sailPrice:total1;
	// 			}
	// 		totalele.text(total)
	// 		total1sail.text(total1)
	// 	});
	// });


})





function showColumn() {
	$("#mycolumn").modal('show');
}

function showColumn2() {
	$("#mycolumn2").modal('show');
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
	var revoke = $("#revoke").val();
	var confirm = $("#confirm").val();
	var ssC = $("#searchssC").val();
	/*
	 * if (search == null || search == "") { swal({ type : "warning", title :
	 * "", text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "stockStatisticss?pageSize=" + pageSize + "&search="
		+ search + "&start=" + start + "&end=" + end  + "&type=" + type + "&searchArea=" + searchArea + "&searchAgent=" + searchAgent+ "&revoke=" + revoke + "&confirm=" + confirm + "&ssC=" + ssC;
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


	var area = $("#searchArea").val();
	var name=$('[name="name"]').val();
	var model=$('[name="model"]').val();
	var supplier=$('[name="supplier"]').val();
	var entryName=$('[name="entryName"]').val();
	var itemNo=$('[name="itemNo"]').val();
	var purchaseInvoiceNo=$('[name="purchaseInvoiceNo"]').val();
	var purchaseInvoiceDate=$('[name="purchaseInvoiceDate"]').val();
	var paymentOrderNo=$('[name="paymentOrderNo"]').val();
	var ssC = $("#searchssC").val();
	var start=$("#start").val();
	var end=$("#end").val();
	var type=$('[name="type"]').val();
	var revoke=$('[name="revoke"]').val();
	var confirm=$('[name="confirm"]').val();
	var projectName=$('[name="projectName"]').val();
	var customer=$('[name="customer"]').val();
	if(start==""||end==""){
		jqueryAlert({
			'icon'    : getRootPath() +'/plugs/alert/img/error.png',
			'content' : "导入的时间段不能为空",
			'closeTime' : 2000,
		})
		return;
	}
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/export?&searchArea=" + area +"&type="
		+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
		"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo
		+"&customer=" +customer+"&projectName=" +projectName;

}

function toJD() {
	var area = $("#searchArea").val();
	var name=$('[name="name"]').val();
	var model=$('[name="model"]').val();
	var supplier=$('[name="supplier"]').val();
	var entryName=$('[name="entryName"]').val();
	var itemNo=$('[name="itemNo"]').val();
	var purchaseInvoiceNo=$('[name="purchaseInvoiceNo"]').val();
	var purchaseInvoiceDate=$('[name="purchaseInvoiceDate"]').val();
	var paymentOrderNo=$('[name="paymentOrderNo"]').val();
	var ssC = $("#searchssC").val();
	var start=$("#start").val();
	var end=$("#end").val();
	var type=$('[name="type"]').val();
	var revoke=$('[name="revoke"]').val();
	var confirm=$('[name="confirm"]').val();
	if(start==""||end==""){
		jqueryAlert({
			'icon'    : getRootPath() +'/plugs/alert/img/error.png',
			'content' : "导入的时间段不能为空",
			'closeTime' : 2000,
		})
		return;
	}
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/toJD?&searchArea=" + area +"&type="
		+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
		"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo;

}


function findMineEidt(){
	
var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();
	var type = $("#type").val();
	var searchArea = $("#searchArea").val();
	var searchAgent = $("#agent").val();
	var userId = $("#userId").val();

	/*
	 * if (search == null || search == "") { swal({ type : "warning", title :
	 * "", text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "stockStatisticss?pageSize=" + pageSize + "&search="
		+ search + "&start=" + start + "&end=" + end  + "&type=" + type + "&searchArea=" + searchArea + "&searchAgent=" + searchAgent+ "&userId=" + userId;

}

//导出新库存统计
function toExportNew() {
	var area = $("#searchArea").val();
	var name=$('[name="name"]').val();
	var model=$('[name="model"]').val();
	var supplier=$('[name="supplier"]').val();
	var entryName=$('[name="entryName"]').val();
	var itemNo=$('[name="itemNo"]').val();
	var purchaseInvoiceNo=$('[name="purchaseInvoiceNo"]').val();
	var purchaseInvoiceDate=$('[name="purchaseInvoiceDate"]').val();
	var paymentOrderNo=$('[name="paymentOrderNo"]').val();
	var ssC = $("#searchssC").val();
	var start=$("#start").val();
	var end=$("#end").val();
	var type=$('[name="type"]').val();
	var revoke=$('[name="revoke"]').val();
	var confirm=$('[name="confirm"]').val();
	var projectName=$('[name="projectName"]').val();
	var customer=$('[name="customer"]').val();
	if(start==""||end==""){
	jqueryAlert({
		    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
		    'content' : "导入的时间段不能为空",
		    'closeTime' : 2000,
		})
		return;
	}
	
	
	
	
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/exportNew?&searchArea=" + area +"&type="
	+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
	+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
	"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo
		+"&customer=" +customer+"&projectName=" +projectName;

}




//导出新库存统计
function toExportNew3() {


	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();
	var type = $("#type").val();
	var areaId = $("#searchArea").val();
	var searchAgent = $("#agent").val();
	
	if(start==""||end==""){
	jqueryAlert({
		    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
		    'content' : "导入的时间段不能为空",
		    'closeTime' : 2000,
		})
		return;
	}
	
	
	
	
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/exportNew3?search=" + search + "&start=" + start + "&end=" + end  + "&type=" + type + "&areaId=" + areaId + "&searchAgent=" + searchAgent;

}








//生成出库单
function outboundOrder(o) {

	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在生成数据请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "stockStatistics/exportOutBoundOrder?id=" + o;
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
							$("#outbound" + o).hide();
							$("#qrcode" + o).hide();
							$("#confirm" + o).css("display","none");
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

function confirm(o) {


	var M = {

	}
	if (M.dialog3) {
		return M.dialog3.show();
	}
	M.dialog3 = jqueryAlert({
		'title': '核对提醒',
		'content': '您当前正在做核对的操作',
		'modal': true,
		'buttons': {
			'确定': function() {
				$.ajax({
					type: 'GET',
					url: 'stockStatistics/confirm',
					data: "id=" + o,
					dataType: 'json',
					success: function(data) {
						if (data.status == 200) {
							//							$("#revokeNum").text(data.data.revokeNum);

							$("#confirm_" + o).text("已核对");
							$("#outbound" + o).hide();
							$("#qrcode" + o).hide();
							$("#ckcode" + o).hide();
							var a='cx_'+o;
							var  b='cwx_'+o;
							$("button[name="+a+"]").hide();
							$("button[name="+b+"]").hide();
							$("#confirm" + o).addClass("danger")
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
	format: "yyyy-mm-dd",
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




function showQRCode(o, o2) {
	$("#showqrcode").modal('show');

	$.ajax({
		type: 'POST',
		url: getRootPath() + "/stockStatistics/getQRCode",
		dataType: "json",
		data: "id=" + o,
		success: function(data) {
			if (o2 != null) {
				$("#qrcodename").text(o2)
			}
			$("#qrcode").attr("src", data.data)
		}
	});

}



function editStockStatistics() {

	var a = $("input[name='ids']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改库存统计信息至少选择一项!!",
		});

	} else {

		$("#editStockStatistics").modal('show');

		var batchids = "";
		var id = $("input[name='ids']:checked");
		var str = "";
		$(id).each(function() {
			str += this.value + ",";
		});
		if (str != "") {
			batchids = str.substring(0, str.length - 1);
		}

		$("#stockid").val(batchids);
	}

}

function confirms(){
	var a = $("input[name='ids']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量核对库存统计信息至少选择一项!!",
		});

	} else{

		var batchids = "";
		var id = $("input[name='ids']:checked");
		var str = "";
		$(id).each(function() {
			str += this.value + ",";
		});
		if (str != "") {
			batchids = str.substring(0, str.length - 1);
		}

		var M = {

		}
		if (M.dialog3) {
			return M.dialog3.show();
		}
		M.dialog3 = jqueryAlert({
			'title': '核对提醒',
			'content': '您当前正在做核对的操作',
			'modal': true,
			'buttons': {
				'确定': function() {
					$.ajax({
						type: 'GET',
						url: 'stockStatistics/confirm',
						data: "id=" + batchids,
						dataType: 'json',
						success: function(data) {
							if (data.status == 200) {

								$(id).each(function() {
								var o=this.value;
								$("#confirm_" + o).text("已核对");
								$("#outbound" + o).hide();
								$("#qrcode" + o).hide();
								$("#ckcode" + o).hide();
								var a='cx_'+o;
								var  b='cwx_'+o;
								$("button[name="+a+"]").hide();
								$("button[name="+b+"]").hide();
								$("#confirm" + o).addClass("danger")
								});
								M.dialog3.close();
								jqueryAlert({
									'content': data.msg
								})
							} else {
								M.dialog3.close();
								console.log("失败"+batchids);
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

}



function getitem(o) {

$("#_"+o).val('')
$("#"+o).toggle();

}

function searchbyN(size,totalpage,Bo){
	var bo = JSON.parse(Bo);
	var N=$("#searchN").val();
	var Tpage=parseInt(totalpage);
	if(N <= 0 || N > Tpage){
		swal({
			type: "warning",
			title: "",
			text: "页数不对，重新填写!!",
		});
	}else{
		var name=returnEmpty(bo.name);
		var ssC=returnEmpty(bo.ssC);
		var area=returnEmpty(bo.searchArea);
		var model=returnEmpty(bo.model);
		var supplier=returnEmpty(bo.supplier);
		var entryName=returnEmpty(bo.entryName);
		var itemNo=returnEmpty(bo.itemNo);
		var purchaseInvoiceNo=returnEmpty(bo.purchaseInvoiceNo);
		var purchaseInvoiceDate=returnEmpty(bo.purchaseInvoiceDate);
		var paymentOrderNo=returnEmpty(bo.paymentOrderNo);
		var type=returnEmpty(bo.type);
		var start=returnEmpty(bo.start);
		var end=returnEmpty(bo.end);
		var revoke=returnEmpty(bo.revoke);
		var confirm=returnEmpty(bo.confirm);
		var customer=returnEmpty(bo.customer);
		var projectName=returnEmpty(bo.projectName);
		// var userId=returnEmpty(bo.userId);
		window.location.href = "stockStatisticss?pageNo=" + N +"&pageSize=" + size + "&searchArea=" + area +"&type="
			+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
			"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo
			+"&customer=" +customer+"&projectName=" +projectName;
	}

}

function  returnEmpty(str){
	if(str === null  || str === undefined ){
		return '';
	}
	return str;
}

function  pageS(pageNo,size,totalpage,Bo){
	var bo = JSON.parse(Bo);
	var name=returnEmpty(bo.name);
	var ssC=returnEmpty(bo.ssC);
	var area=returnEmpty(bo.searchArea);
	var model=returnEmpty(bo.model);
	var supplier=returnEmpty(bo.supplier);
	var entryName=returnEmpty(bo.entryName);
	var itemNo=returnEmpty(bo.itemNo);
	var purchaseInvoiceNo=returnEmpty(bo.purchaseInvoiceNo);
	var purchaseInvoiceDate=returnEmpty(bo.purchaseInvoiceDate);
	var paymentOrderNo=returnEmpty(bo.paymentOrderNo);
	var type=returnEmpty(bo.type);
	var start=returnEmpty(bo.start);
	var end=returnEmpty(bo.end);
	var revoke=returnEmpty(bo.revoke);
	var confirm=returnEmpty(bo.confirm);
	var customer=returnEmpty(bo.customer);
	var projectName=returnEmpty(bo.projectName);

	// var userId=returnEmpty(bo.userId);

	window.location.href = "stockStatisticss?pageNo=" + pageNo +"&pageSize=" + size + "&searchArea=" + area +"&type="
		+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
		"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo
		+"&customer=" +customer+"&projectName=" +projectName;
}

function searchBo() {
	var area = $("#searchArea").val();
	var size = $("#pageSize").val();
	var name=$('[name="name"]').val();
	var model=$('[name="model"]').val();
	var supplier=$('[name="supplier"]').val();
	var entryName=$('[name="entryName"]').val();
	var itemNo=$('[name="itemNo"]').val();
	var purchaseInvoiceNo=$('[name="purchaseInvoiceNo"]').val();
	var purchaseInvoiceDate=$('[name="purchaseInvoiceDate"]').val();
	var paymentOrderNo=$('[name="paymentOrderNo"]').val();
	var ssC = $("#searchssC").val();
	var start=$("#start").val();
	var end=$("#end").val();
	var type=$('[name="type"]').val();
	var revoke=$('[name="revoke"]').val();
	var confirm=$('[name="confirm"]').val();
	var projectName=$('[name="projectName"]').val();
	var customer=$('[name="customer"]').val();
	// var userId=$('[name="userId"]').val();
	/*
	 * if (search == null || search == "") { swal({ type : "warning", title : "",
	 * text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "stockStatisticss?&pageSize=" + size + "&searchArea=" + area +"&type="
		+ type + "&start=" + start + "&end=" + end + "&revoke=" + revoke + "&confirm=" + confirm + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
		"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo
	+"&customer=" +customer+"&projectName=" +projectName;

}





