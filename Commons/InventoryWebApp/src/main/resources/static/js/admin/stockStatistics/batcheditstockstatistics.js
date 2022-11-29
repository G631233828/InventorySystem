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


