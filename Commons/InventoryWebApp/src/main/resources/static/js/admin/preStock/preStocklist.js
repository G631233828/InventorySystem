

function batchImput() {
	$("#mybatchUpload").modal('show');

}

function searchVal() {

	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var searchArea = $("#searchArea").val();
	var status = $("#prestatus").val();
	var ssC = $("#searchssC").val();
/*
 * if (search == null || search == "") { swal({ type : "warning", title : "",
 * text : "查询内容不能为空!!", }); return ; }
 */
	window.location.href="preStocks?pageSize="+pageSize+"&search="+search+"&searchArea="+searchArea+"&status=" + status + "&ssC=" + ssC;

}

function editprestock() {

	var a = $("input[name='ids']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改库存统计信息至少选择一项!!",
		});

	} else {

		$("#editprestock").modal('show');

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
	jqueryAlert({
	    'icon'    : getRootPath() +'/plugs/alert/img/right.png',
	    'content' : "正在导出请稍等...",
	    'closeTime' : 5000,
	})
	var areaId = $("#searchArea").val();
	
	
	window.location.href = "prestock/export?&searchArea=" + area  + "&userId=&ssC="+ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo +
		"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo;
	
	
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
	window.location.href = "preStock/clearSearch";
}






function wechatPush(o){
	$.ajax({
		type : 'POST',
		url : "preStock/preStockPush",
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
		url: 'prestock/getItems',
		data: "id=" + batchids,
		dataType: 'json',
		success: function(data) {
			if (data.status == 200) {
				var prestocklist = "";
				$.each(data.data, function(index, item) {
					prestocklist += ` <tr id=stock_` + item.id + `>
                               <td class="numeric">`+ item.name + `</td>
                               <td class="numeric">`+ item.model + `</td>
                               <td class="numeric">`+ item.actualReceiptQuantity + `</td>
                               <td class="numeric">
							   <input type="hidden" name="batchid" value="`+ item.id + `"> 
                               <input type="text" onblur="return setStockNum('`+ item.id + `')"  class="form-control stockval batchout" id=stocknum_` + item.id + `   name="batchnum" >
                               </td>
                               <td class="numeric">
                               <button class="btn " type="button" onclick="return deleteStock('`+ item.id + `')" > <i  class="fa fa-trash-o">移除 </i>
							  </button>
                                </td>  </tr>`
				});
				$("#prestocklist").html(prestocklist)
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

function setStockNum(o) {

	var val = $("#stocknum_" + o).val();

	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/prestock/checkNum",
		data: "id=" + o + "&num=" + val,
		success: function(data) {

			if (data.status == 200) {


			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#stocknum_" + o).val('')

			}

		}
	});


}

function deleteStock(o) {
	$("#stock_" + o).html('');
}

function batchFormSubmit() {

	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/prestock/batchOut",
		data: $("#BatchOutstockStatisticsForm").serialize(),
		success: function(data) {
			if (data.status == 200) {
				$.each(data.data, function(index, item) {
					var eiq = item.estimatedInventoryQuantity;
					var aiq = item.actualReceiptQuantity;
					var eaiq= eiq - aiq;
					$("#eaquantity_" + item.id).text(eaiq);
					$("#acquantity_" + item.id).text(aiq);
				});
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})

				$("#mystockbatchout").modal('hide');
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
			}


		}
	});


}

function showColumn() {
	$("#mycolumn").modal('show');
}

function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type: 'GET',
		url: 'prestock/columns',
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

function getitem(o) {

	$("#_"+o).val('')
	$("#"+o).toggle();

}

function batchEditprestockForm() {
	var a = $("input[name='item']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改预库存信息至少选择一项!!",
		});

	}

	// var inprice = $("#_it0").val();//入库总额
	// var sailesInvoiceNo = $("#_it1").val();//销售发票号
	var purchaseInvoiceNo = $("#_it2").val();//采购发票号
	// var receiptNo = $("#_it3").val();//银行收款单号
	var paymentOrderNo = $("#_it4").val();//银行付款单号
	// var sailesInvoiceDate = $("#_it5").val();//销售发票开具日期
	var itemNo = $("#_it6").val();//采购付款申请单编码(新)
	var purchaseInvoiceDate = $("#_it7").val();//采购发票到票时间
	// var description = 	$("#_it8").val();//备注
	// alert(newItemNo)
	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/prestock/batchEditprestock",
		data: $("#batchStockStatisticsForm").serialize(),
		success: function(data) {
			if(data.status == 200){
				var batchids = "";
				var id = $("input[name='ids']:checked");
				$(id).each(function() {
					// if(inprice!=""){
					// 	$("#inprice_"+this.value).text(inprice);
					// }
					// if(sailesInvoiceNo!=""){
					// 	$("#sailesInvoiceNo_"+this.value).text(sailesInvoiceNo);
					// }
					if(purchaseInvoiceNo!=""){
						$("#purchaseInvoiceNo_"+this.value).text(purchaseInvoiceNo);
					}
					// if(receiptNo!=""){
					// 	$("#receiptNo_"+this.value).text(receiptNo);
					// }
					if(paymentOrderNo!=""){
						$("#paymentOrderNo_"+this.value).text(paymentOrderNo);
					}
					// if(sailesInvoiceDate!=""){
					// 	$("#sailesInvoiceDate_"+this.value).text(sailesInvoiceDate);
					// }
					if(itemNo!=""){
						$("#itemNo_"+this.value).text(itemNo);
					}
					if(purchaseInvoiceDate!=""){
						$("#purchaseInvoiceDate_"+this.value).text(purchaseInvoiceDate);
					}

					// if(description!=""){
					// 	$("#description_"+this.value).text(description);
					// }
				});

				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#editStockStatistics").modal('hide');


			}else{
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})

			}


		}
	});


}
$(".datepicker").datepicker({
	language: "zh-CN",
	format: "yyyy-mm-dd",
	keyboardNavigation: false,
	forceParse: false,
	autoclose: true
});

function  returnEmpty(str){
	if(str === null  || str === undefined ){
		return '';
	}
	return str;
}

function  pageS(pageNo,size,totalpage,Bo,status){
	var bo = JSON.parse(Bo);
	var name=returnEmpty(bo.name);
	var model=returnEmpty(bo.model);
	var area=returnEmpty(bo.searchArea);
	var ssC=returnEmpty(bo.ssC);
	var supplier=returnEmpty(bo.supplier);
	var entryName=returnEmpty(bo.entryName);
	var itemNo=returnEmpty(bo.itemNo);
	var purchaseInvoiceNo=returnEmpty(bo.purchaseInvoiceNo);
	var purchaseInvoiceDate=returnEmpty(bo.purchaseInvoiceDate);
	var paymentOrderNo=returnEmpty(bo.paymentOrderNo);
	window.location.href = "preStocks?pageNo=" + pageNo +"&pageSize=" + size  +  "&ssC=" + ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo + "&searchArea=" + area
		+"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo + "&status=" +status;
}

function searchBo() {
	var status=$('#prestatus').val();
	var searchArea = $("#searchArea").val();
	var pageSize = $("#pageSize").val();
	var name=$('[name="name"]').val();
	var model=$('[name="model"]').val();
	var supplier=$('[name="supplier"]').val();
	var entryName=$('[name="entryName"]').val();
	var itemNo=$('[name="itemNo"]').val();
	var purchaseInvoiceNo=$('[name="purchaseInvoiceNo"]').val();
	var purchaseInvoiceDate=$('[name="purchaseInvoiceDate"]').val();
	var paymentOrderNo=$('[name="paymentOrderNo"]').val();
	var ssC = $("#searchssC").val();
	/*
	 * if (search == null || search == "") { swal({ type : "warning", title : "",
	 * text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "preStocks?pageSize=" + pageSize + "&ssC=" + ssC
		+"&name=" + name + "&model=" + model + "&supplier=" + supplier + "&entryName=" + entryName + "&itemNo=" + itemNo + "&searchArea=" + searchArea
		+"&purchaseInvoiceNo=" + purchaseInvoiceNo + "&purchaseInvoiceDate=" + purchaseInvoiceDate + "&paymentOrderNo="+paymentOrderNo +"&status=" +status;

}



