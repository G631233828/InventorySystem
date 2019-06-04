
function showColumn() {
	$("#mycolumn").modal('show');
}







function searchVal() {

	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();

/*	if (search == null || search == "") {
		swal({
			type : "warning",
			title : "",
			text : "查询内容不能为空!!",
		});
		return ;
	}*/
	window.location.href="stockStatisticss?pageSize="+pageSize+"&search="+search;

}
function searchSize() {
	
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	window.location.href="stockStatisticss?pageSize="+pageSize+"&search="+search;
	
}

function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type : 'GET',
		url : 'stockStatistics/columns',
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





$(".datepicker").datepicker({
    language: "zh-CN",
    format: "yyyy-mm-dd",
    keyboardNavigation : false,
	forceParse : false,
	autoclose : true
});


//$("#data_1 .input-group.date").datepicker({
//	keyboardNavigation : false,
//	forceParse : false,
//	autoclose : true
//});
