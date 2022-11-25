//data : $("#BatchPaymentOrderNoForm").serialize(),
//批量出库提交
function BatchPaymentOrderNoForm() {

	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/stock/batchPaymentOrderNo",
		data: $("#BatchPaymentOrderNoForm").serialize(),
		success: function(data) {
			
			if(data.status == 200){
			var batchids = "";
	var id = $("input[name='ids']:checked");
	$(id).each(function() {
		$("#itemNo_"+this.value).text(data.data);
	});

			jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#myPaymentOrderNo").modal('hide');	
				
			
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


