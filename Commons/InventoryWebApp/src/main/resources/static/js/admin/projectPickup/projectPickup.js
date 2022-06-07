
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

	/*
	 * if (search == null || search == "") { swal({ type : "warning", title :
	 * "", text : "查询内容不能为空!!", }); return ; }
	 */
	window.location.href = "projectPickups?pageSize=" + pageSize + "&search="
		+ search + "&start=" + start + "&end=" + end;
}




$(".datepicker").datepicker({
	language: "zh-CN",
	format: "yyyy-mm-dd 00:00:00",
	keyboardNavigation: false,
	forceParse: false,
	autoclose: true
});






function toExport(o) {


	if (o != "") {
		//下载单个
		window.location.href = "projectPickup/export?id=" + o;
	} else {
		var a = $("input[name='ids']:checked").length;
		if (a == 0) {
			var search = $("#serach").val();
			var start = $("#start").val();
			var end = $("#end").val();
			//swal({
			//type: "warning",
			//title: "",
			//text: "出库单导出至少选择一项!!",
			//});
			window.location.href = "projectPickup/export?search=" + search + "&start=" + start + "&end=" + end;
		} else {
			var str = "";
			var ids = $("input[name='ids']:checked");
			$(ids).each(function() {
				str += this.value + ",";
			});
			window.location.href = "projectPickup/export?id=" + str;
		}



	}

}



function stockStaticsOut(o, o2) {
	if (o != "" && o != null) {
		swal({
			title: "您确定要对【" + o2 + "】进行出库吗？",
			text: "请确认出库数量是否正确，否则会影响正常库存数！",
			type: "warning",
			showCancelButton: true,
			confirmButtonColor: "#DD6B55",
			confirmButtonText: "是的，我要出库！",
			cancelButtonText: "让我再考虑一下…",
			closeOnConfirm: false,
			closeOnCancel: false
		}, function(a) {
			if (a) {

				$.ajax({
					type: 'POST',
					url: 'projectPickupStockStatistics/in',
					data: "id=" + o,
					dataType: 'json',
					success: function(data) {
						if (data.status == 200) {
						
						swal("提示信息", data.msg, "success")
						
						$("#tostock").text("已出库");
						$("#tostock").attr("disabled",true);
						
						} else {
						
						swal("提示信息", data.msg, "error")
						
						
						}
						
					}
				})


			} else {
				swal("已取消", "您取消了出库操作！", "error")
			}
		})

	}


}









