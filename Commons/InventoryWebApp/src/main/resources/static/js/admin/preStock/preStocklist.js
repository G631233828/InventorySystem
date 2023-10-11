function batchImput() {
	$("#mybatchUpload").modal('show');

}

function searchVal() {

	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var searchArea = $("#searchArea").val();
	var status = $("#prestatus").val();
/*
 * if (search == null || search == "") { swal({ type : "warning", title : "",
 * text : "查询内容不能为空!!", }); return ; }
 */
	window.location.href="preStocks?pageSize="+pageSize+"&search="+search+"&searchArea="+searchArea+"&status="+status;

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
	
	
	window.location.href = "prestock/export?areaId="+areaId;
	
	
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







