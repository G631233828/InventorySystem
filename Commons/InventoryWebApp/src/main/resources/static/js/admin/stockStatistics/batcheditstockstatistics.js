//data : $("#BatchPaymentOrderNoForm").serialize(),
//批量出库提交
function batchEditStockStatisticsForm() {
var a = $("input[name='item']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改库存统计信息至少选择一项!!",
		});

	}

var inprice = $("#_it0").val();//入库总额
var sailesInvoiceNo = $("#_it1").val();//销售发票号
var purchaseInvoiceNo = $("#_it2").val();//采购发票号
var receiptNo = $("#_it3").val();//银行收款单号
var paymentOrderNo = $("#_it4").val();//银行付款单号
var sailesInvoiceDate = $("#_it5").val();//销售发票开具日期
var newItemNo = $("#_it6").val();//采购付款申请单编码(新)
var purchaseInvoiceDate = $("#_it7").val();//采购发票到票时间
var description = 	$("#_it8").val();//备注
	// alert(newItemNo)
	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/stockStatistics/batchEditStockStatistics",
		data: $("#batchStockStatisticsForm").serialize(),
		success: function(data) {
			if(data.status == 200){
			var batchids = "";
	var id = $("input[name='ids']:checked");
	$(id).each(function() {
		if(inprice!=""){
			$("#inprice_"+this.value).text(inprice);
		}
		if(sailesInvoiceNo!=""){
			$("#sailesInvoiceNo_"+this.value).text(sailesInvoiceNo);
		}
		if(purchaseInvoiceNo!=""){
			$("#purchaseInvoiceNo_"+this.value).text(purchaseInvoiceNo);
		}
		if(receiptNo!=""){
			$("#receiptNo_"+this.value).text(receiptNo);
		}
		if(paymentOrderNo!=""){
			$("#paymentOrderNo_"+this.value).text(paymentOrderNo);
		}
		if(sailesInvoiceDate!=""){
			$("#sailesInvoiceDate_"+this.value).text(sailesInvoiceDate);
		}
		if(newItemNo!=""){
			$("#newItemNo_"+this.value).text(newItemNo);
		}
		if(purchaseInvoiceDate!=""){
			$("#purchaseInvoiceDate_"+this.value).text(purchaseInvoiceDate);
		}

		if(description!=""){
			$("#description_"+this.value).text(description);
		}
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


