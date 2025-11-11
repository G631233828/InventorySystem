
function searchVal() {

	var pageSize = $("#pageSize").val();
	var searchArea = $("#searchArea").val();
	var status = $("#pickstatus").val();
	var pnameid = $("#pnameid").val();
	var customerid = $("#customerid").val();
	var stockid = $("#stockid").val();
	var modelid = $("#modelid").val();
	var searchModel =  $("#searchModel").val();
	var searchStock =  $("#searchStock").val();
	var publisherid=$("#publisherid").val();
	/*
	 * if (search == null || search == "") { swal({ type : "warning", title : "",
	 * text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "pickUpApplications?pageSize=" + pageSize + "&searchArea=" + searchArea + "&pnameid=" + pnameid + "&status=" + status+ "&customerid=" + customerid+ "&stockid=" + stockid+ "&modelid=" + modelid +"&publisherid=" +publisherid+ "&searchModel=" + searchModel+ "&searchStock=" + searchStock;

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


	window.location.href = "stock/export?areaId=" + areaId;


}

function Export() {
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})

	window.location.href = "pickUpApplication/export";


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
	window.location.href = "pickUpApplication/clearSearch";
}






function wechatPush(o) {
	$.ajax({
		type: 'POST',
		url: "pickUpApplication/pickUpApplicationPush",
		dataType: "json",
		data: "id=" + o,
		success: function(data) {
			if (data.status == 200) {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.data,
					'closeTime': 5000,
				})
			} else if (data.status == 201) {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/warning.png',
					'content': data.data,
					'closeTime': 10000,
				})
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.data,
					'closeTime': 5000,
				})
			}



		}
	});
}


function batchImput() {
	$("#mybatchUpload").modal('show');

}





/**
*批量导出
*
*/
function batchAdd() {
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
        url: 'pickUpApplication/getbatch',
        data: "id=" + batchids,
        dataType: 'json',
        success: function(data) {
            if (data.status == 200) {
                var stocklist = "";
                $.each(data.data, function(index, item) {
                    // 处理 actualIssueQuantity 为 null 的情况，默认设为 0
                    const actualQty = item.actualIssueQuantity === null ? 0 : item.actualIssueQuantity;
                    // 计算差值（确保用处理后的值）
                    const diff = item.estimatedIssueQuantity - actualQty;
                    
                    stocklist += ` <tr id=stock_` + item.id + `>
                                   <td class="numeric">`+ item.stock.name + `</td>
                                   <td class="numeric">`+ item.stock.model + `</td>
                                   <td class="numeric">`+ item.accepter + `</td>
                                   <td class="numeric">`+ item.estimatedIssueQuantity + `</td>
                                   <!-- 显示 actualIssueQuantity 时处理为两位小数 -->
                                   <td class="numeric">`+ actualQty.toFixed(2) + `</td>
                                   <td class="numeric">
                                   <input type="hidden" name="batchid" value="`+ item.id + `"> 
                                   <!-- 差值保留两位小数 -->
                                   <input type="text" value="`+ diff.toFixed(2) + `" onblur="return setpickUpNum('`+ item.id + `','` + item.estimatedIssueQuantity + `','` + actualQty + `')"  class="form-control stockval batchout" id=pickupnum_` + item.id + `   name="batchnum" >
                                   </td>
                                   <td class="numeric">
                                   <button class="btn " type="button" onclick="return deleteStock('`+ item.id + `')" > <i  class="fa fa-trash-o"> </i>
                                  </button>
                                    </td>  </tr>`;
                });
                $("#pickuplist").html(stocklist);
                $("#mybatchAdd").modal('show');
            } else {
                jqueryAlert({
                    'icon': getRootPath() + '/plugs/alert/img/error.png',
                    'content': data.msg,
                    'closeTime': 2000,
                });
            }
        }
    });
}



//批量取货完成 删除
function deleteStock(o) {
	$("#stock_" + o).html('');
}





//校验库存
function setpickUpNum(o, o2, o3) {
    // 获取输入框元素（避免重复查询DOM）
    const $input = $(`#pickupnum_${o}`);
    // 获取并转换输入值
    const val = Number($input.val());
    // 转换参数为数字
    const numO2 = Number(o2);
    const numO3 = Number(o3);
    // 最大允许值（预出库与实际出库的差值）
    const maxVal = numO2 - numO3;

    // 校验输入是否为有效数字（包括整数和两位小数）
    const decimalPattern = /^\d+(\.\d{1,2})?$/;
    if (!decimalPattern.test($input.val().trim())) {
        showErrorMsg($input, '请输入有效的数字（最多保留两位小数）');
        return;
    }

    // 校验数值是否大于0
    if (val <= 0) {
        showErrorMsg($input, '输入值必须大于0');
        return;
    }

    // 校验数值是否超过最大值
    if (val > maxVal) {
        showErrorMsg($input, `实际出库值不能大于${maxVal.toFixed(2)}`);
        return;
    }

    // 若所有校验通过，可在此处添加后续逻辑（如保留两位小数显示）
    $input.val(val.toFixed(2));
}

// 提取错误提示共用方法，减少重复代码
function showErrorMsg($input, message) {
    jqueryAlert({
        'icon': getRootPath() + '/plugs/alert/img/error.png',
        'content': message,
        'closeTime': 2000,
    });
    $input.val('').focus(); // 清空并聚焦，方便重新输入
}






function  batchFormSubmit() {
$.ajax({
		type: 'POST',
		url: "pickUpApplication/batchAdd",
		dataType: "json",
		data: $("#batchAddForm").serialize(),
		success: function(data) {
			if (data.status == 200) {
			
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.data,
					'closeTime': 1000,
				})
				  location.reload();
			} 



		}
	});

}


function getitem(o) {

	$("#_"+o).val('')
	$("#"+o).toggle();

}


function batchedit() {

	var a = $("input[name='ids']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改库存统计信息至少选择一项!!",
		});

	} else {

		$("#editForm").modal('show');

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




function batchEditForm() {
	var a = $("input[name='item']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改预库存信息至少选择一项!!",
		});

	}
	 var description = 	$("#_it7").val();//备注
	// alert(newItemNo)
	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/pickUpApplication/batchEdit",
		data: $("#batchStockStatisticsForm").serialize(),
		success: function(data) {
			if(data.status == 200){
				var batchids = "";
				var id = $("input[name='ids']:checked");
				$(id).each(function() {
		
					 if(description!=""){
					 	$("#description_"+this.value).text(description);
					}
				});

				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#editForm").modal('hide');


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





















