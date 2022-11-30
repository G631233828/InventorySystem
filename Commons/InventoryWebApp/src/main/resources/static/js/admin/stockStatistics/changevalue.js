function change(o,o2) {
var o1 = $("#" + o2 + "_" + o).text();
	$("#" + o2 + "_" + o).html(`<input name="` + o2 + `" id="` + o2 + `id_` + o + `" value="` + o1 + `" onkeydown="return dosubmit(event)"  onblur="return hideinput('` + o + `','` + o1 + `','` + o2 + `')">`)

}

function hideinput(o, o1, o2) {
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








