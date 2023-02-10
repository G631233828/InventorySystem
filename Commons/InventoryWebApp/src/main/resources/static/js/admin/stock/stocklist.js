/**
 * 批量导入
 * 
 * @returns
 */
function batchImput() {
	$("#mybatchUpload").modal('show');

}

function showColumn() {
	$("#mycolumn").modal('show');
}

function showQRCode(o, o2) {
	$("#showqrcode").modal('show');

	$.ajax({
		type: 'POST',
		url: getRootPath() + "/stock/getQRCode",
		dataType: "json",
		data: "id=" + o,
		success: function(data) {
			$("#qrcodename").text(o2)
			$("#qrcode").attr("src", data.data)
		}
	});

}



/**
*批量导出
*
*/
function batchOut() {
	var batchids = "";
	var id = $("input[name='ids']:checked");
	var str = "";
	$(id).each(function() {
		str += this.value + ",";
	});
	if (str != "") {
		batchids = str.substring(0, str.length - 1);
	}
	$.ajax({
		type: 'POST',
		url: 'stock/batchOut',
		data: "id=" + batchids,
		dataType: 'json',
		success: function(data) {
			if (data.status == 200) {
				var stocklist = "";
				$.each(data.data, function(index, item) {
					stocklist += ` <tr id=stock_` + item.id + `>
                               <td class="numeric">`+ item.name + `</td>
                               <td class="numeric">`+ item.model + `</td>
                               <td class="numeric">`+ item.inventory + `</td>
                               <td class="numeric">
							   <input type="hidden" name="batchid" value="`+ item.id + `"> 
                               <input type="text" onblur="return setStockNum('`+ item.id + `')"  class="form-control stockval batchout" id=stocknum_` + item.id + `   name="batchnum" >
                               </td>
                               <td class="numeric">
                               <button class="btn " type="button" onclick="return deleteStock('`+ item.id + `')" > <i  class="fa fa-trash-o">移除 </i>
							  </button>
                                </td>  </tr>`
				});
				$("#stocklist").html(stocklist)
				$("#mystockbatchout").modal('show');
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
			}






		}
	})








}






function deleteStock(o) {
	$.ajax({
		type: 'POST',
		url: getRootPath() + "/stock/deleteInSession",
		dataType: "json",
		data: "id=" + o,
		success: function(data) {
			$("#stock_" + data.data).html('');
		}
	});
}


/**
 * 查找设备信息
 * 
 * @param o
 * @returns
 */
function showStockStatistics(o) {
	$(".stockval").val('');
	$(".stockval").text('');

	$.ajax({
		type: 'POST',
		url: 'stockStatistics/findStock',
		data: "id=" + o,
		dataType: 'json',
		success: function(data) {
			if (data.status == 200) {
				var datas = data.data;
				var name = datas.name;
				var inventory = datas.inventory;
				var stockid = datas.id;
				var description = datas.description;
				var unit = "";
				if (datas.unit != null) {
					unit = datas.unit.name;
				}
				$("#stockId").val(stockid);
				$("#stockName").text(name);
				$("#description").text(description);
				$("#loadinventory").text(inventory + "  " + unit);

			}
		}
	})
	$("#mystockStatistics").modal('show');
}


/**
 * 出库
 */
function showStockStatistics2(o) {
	$(".stockval").val('');
	$(".stockval").text('');

	$.ajax({
		type: 'POST',
		url: 'stockStatistics/findStock',
		data: "id=" + o,
		dataType: 'json',
		success: function(data) {
			if (data.status == 200) {
				var datas = data.data;
				var name = datas.name;
				var inventory = datas.inventory;
				var stockid = datas.id;
				var description = datas.description;
				var unit = "";
				if (datas.unit != null) {
					unit = datas.unit.name;
				}
				$("#stockIdOut").val(stockid);
				$("#descriptionOut").text(description);
				$("#stockNameOut").text(name);
				$("#loadinventoryOut").text(inventory + "  " + unit);

			}
		}
	})
	$("#mystockStatistics2").modal('show');
}


/**
*
*加入出库列表
*/
function addToStocklist(o) {

	$.ajax({
		type: 'POST',
		url: 'stock/addToStocklist',
		data: "id=" + o,
		dataType: 'json',
		success: function(data) {

			if (data.status == 200) {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})

			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})

			}

		}
	})

}





function copyStock(o, o2) {

	swal({
		title: "您确定要复制【" + o + "】这条库存数据吗？",
		text: "复制后库存新数据库存数量需手动录入",
		type: "warning",
		showCancelButton: true,
		confirmButtonColor: "#DD6B55",
		confirmButtonText: "是的，我要复制！",
		cancelButtonText: "让我再考虑一下…",
		closeOnConfirm: false,
		closeOnCancel: false
	}, function(a) {
		if (a) {
			window.location.href = "copyStock?id=" + o2;

		} else {
			swal("已取消", "您取消了复制操作！", "error")
		}
	})

}






function showSupplier(o) {
	$(".supplier2").text('');
	$.ajax({
		type: 'POST',
		url: 'stock/getSupplier',
		data: "id=" + o,
		dataType: 'json',
		success: function(data) {
			if (data.status == 200) {
				var name = data.data.name != null ? data.data.name : "";
				var systemClassification = data.data.systemClassification.name != null ? data.data.systemClassification.name : "";
				var categorys = '';
				var cas = data.data.categorys ? data.data.categorys : null;
				if (cas != null) {
					$.each(data.data.categorys, function(key, value) {
						categorys += value.name + ",  ";
					})
				}
				var brand = data.data.brand != null ? data.data.brand.name : "";
				var contact = data.data.contact != null ? data.data.contact : "";
				var contactNumber = data.data.contactNumber != null ? data.data.contactNumber : "";
				var qq = data.data.qq != null ? data.data.qq : "";
				var wechat = data.data.wechat != null ? data.data.wechat : "";
				var email = data.data.email != null ? data.data.email : "";
				var address = data.data.address != null ? data.data.address : "";
				var introducer = data.data.introducer != null ? data.data.introducer : "";
				var productQuality = data.data.productQuality != null ? data.data.productQuality : "";
				var supplyPeriod = data.data.supplyPeriod != null ? data.data.supplyPeriod : "";
				var payMent = data.data.payMent != null ? data.data.payMent : "";
				var afterSaleService = data.data.afterSaleService != null ? data.data.afterSaleService : "";

				$("#supplier_name").text(name);
				$("#supplier_systemClassification").text(systemClassification);
				$("#supplier_categorys").text(categorys);
				$("#supplier_brand").text(brand);
				$("#supplier_contact").text(contact);
				$("#supplier_contactNumber").text(contactNumber);
				$("#supplier_qq").text(qq);
				$("#supplier_wechat").text(wechat);
				$("#supplier_email").text(email);
				$("#supplier_address").text(address);
				$("#supplier_introducer").text(introducer);
				$("#supplier_productQuality").text(productQuality);
				$("#supplier_").text(systemClassification);
				$("#supplier_supplyPeriod").text(supplyPeriod);
				$("#supplier_payMent").text(payMent);
				$("#supplier_afterSaleService").text(afterSaleService);

			}
		}
	})


	$("#mysupplier").modal('show');

}







function searchVal() {

	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var searchArea = $("#searchArea").val();
	var searchAgent = $("#agent").val();
	/*
	 * if (search == null || search == "") { swal({ type : "warning", title : "",
	 * text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "stocks?pageSize=" + pageSize + "&search=" + search + "&searchArea=" + searchArea + "&searchAgent=" + searchAgent;

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




function toStatistics(o) {
	window.location.href = "stockStatisticss?id=" + o;

}

function toExport() {
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	var areaId = $("#searchArea").val();
	var searchAgent = $("#agent").val();


	window.location.href = "stock/export?areaId=" + areaId + "&searchAgent=" + searchAgent;


}







function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type: 'GET',
		url: 'stock/columns',
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

$(document).ready(function() {
	if ($("#upload").val() != "") {

		$('#submit')
			.bind(
				'click',
				function() {
					var eventFun = function() {
						$
							.ajax({
								type: 'GET',
								url: 'stock/uploadprocess',
								data: {},
								dataType: 'json',
								success: function(
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





function cleanSearch() {
	window.location.href = "stock/clearSearch";
}




function toDownloadQRcode(o) {
var search = $("#serach").val();
	if(search=="所有库存二维码"||search=="有库存二维码"){
		window.location.href = "stock/downloadQRcode?id=" + o+"&search="+search;
		return;
}

	if (o != "") {
		//下载单个
		window.location.href = "stock/downloadQRcode?id=" + o;
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
			window.location.href = "stock/downloadQRcode?id=" + str;
		}



	}





}




function batchPaymentOrderNo() {

	var a = $("input[name='ids']:checked").length;
		if (a == 0) {
			swal({
				type: "warning",
				title: "",
				text: "批量修改采购付款单编号至少选择一项!!",
			});
		
		}else{
		
	$("#myPaymentOrderNo").modal('show');

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






