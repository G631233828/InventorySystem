//批量出库提交
function batchEditStockForm() {
var a = $("input[name='ids']:checked").length;
	if (a == 0) {
		swal({
			type: "warning",
			title: "",
			text: "批量修改库信息至少选择一项!!",
		});

	}


	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/stock/batchEditStock",
		data: $("#batchStockForm").serialize(),
		success: function(data) {
			if(data.status == 200){
	var id = $("input[name='ids']:checked");
	$(id).each(function() {
	$("#area_"+this.value).text($("#batcharea :selected").text())
	});
	

			jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#editStock").modal('hide');	
				
			
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


