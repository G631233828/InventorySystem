function change(o,o2,o3) {
		var s=$("#confirm_" + o).text();
		if( s == '已核对' ){
			return;
		}else{
			var o1 = $("#" + o2 + "_" + o).text();
			$("#" + o2 + "_" + o).html(`<input name="` + o2 + `" id="` + o2 + `id_` + o + `" value="` + o1 + `" onkeydown="return dosubmit(event)"  onblur="return hideinput('` + o + `','` + o1 + `','` + o2 + `')">`)
		}

}

function changeyinhang(o,o2,o3) {
		var o1 = $("#" + o2 + "_" + o).text();
		$("#" + o2 + "_" + o).html(`<input name="` + o2 + `" id="` + o2 + `id_` + o + `" value="` + o1 + `" onkeydown="return dosubmit(event)"  onblur="return hideinput('` + o + `','` + o1 + `','` + o2 + `')">`)

}


function changeDate(o,o2,o3) {
	var s=$("#confirm_" + o).text();
	if( s == '已核对'){return;}else{
		var o1 = $("#" + o2 + "_" + o).text();
		$("#" + o2 + "_" + o).html(`
	<div class="col-sm-12 form-inline">
	<input  class="form-control datepicker2" readonly="readonly" name="` + o2 + `" id="` + o2 + `id_` + o + `" value="` + o1 + `" onkeydown="return dosubmit(event)" >
	<button class="btn btn-default btn-sm" onclick="return hideinputDate('` + o + `','` + o1 + `','` + o2 + `')"> <i class="entypo-search"> </i> 确定 </button>
	</div>
	`)
		$(".datepicker2").datepicker({
			language: "zh-CN",
			format: "yyyy-mm-dd",
			keyboardNavigation: false,
			forceParse: false,
			autoclose: true
		});
	}
}

function hideinput(o, o1, o2) {

	var v = $("#" + o2 + "id_" + o).val();
	if (v != o1) {
	var heji = parseFloat($("#heji").text());
	var newheji =Number(heji)+Number(v)-Number(o1);
	$("#heji").text(newheji.toFixed(2));
	
		//ajax提交数据库
		$.ajax({
			dataType: "json",
			type: "POST",
			url: getRootPath() + "/stockStatistics/batchEditStockStatistics",
			data: o2 + "=" + v + "&stockid=" + o,
			success: function(data) {
				if (data.status == 200) {
					jqueryAlert({
						'icon': getRootPath() + '/plugs/alert/img/right.png',
						'content': data.msg,
						'closeTime': 1000,
					})

					$("#" + o2 + "_" + o).text(v);
				}
			}
		});
	}

	$("#" + o2 + "_" + o).text(v);

}
function hideinputDate(o, o1, o2) {

	var v = $("#" + o2 + "id_" + o).val();
	if (v != o1) {

		//ajax提交数据库
		$.ajax({
			dataType: "json",
			type: "POST",
			url: getRootPath() + "/stockStatistics/batchEditStockStatistics",
			data: o2 + "=" + v + "&stockid=" + o,
			success: function(data) {
				if (data.status == 200) {
					jqueryAlert({
						'icon': getRootPath() + '/plugs/alert/img/right.png',
						'content': data.msg,
						'closeTime': 1000,
					})

					$("#" + o2 + "_" + o).text(v);
				}
			}
		});
	}

	$("#" + o2 + "_" + o).text(v);

}

function dosubmit(e){

	if(e.keyCode ==13){
$("#serach").focus();
		
	}
	return;
}








