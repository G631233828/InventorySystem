$(".datepicker").datepicker({
	language: "zh-CN",
	format: "yyyy-mm-dd",
	keyboardNavigation: false,
	forceParse: false,
	autoclose: true
});

function showColumn() {
	$("#mycolumn").modal('show');
}

/**
 * 批量导入
 * 
 * @returns
 */
function batchImput() {
	$("#mybatchUpload").modal('show');

}

function searchVal() {
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();


	window.location.href = "inventoryTransfers?pageSize=" + pageSize + "&search="
		+ search + "&start=" + start + "&end=" + end;
}


function deleteInventoryTransfer(o) {
$.ajax({
		type : 'POST',
		url : getRootPath() + "/inventoryTransfer/deleteInSession",
		dataType : "json",
		data: "id=" + o,
		success : function(data) {
			$("#stock_"+data.data).html('');
		}
	});
}








function toExport() {


	var search = $("#serach").val();
	var start = $("#start").val();
	var end = $("#end").val();
	jqueryAlert({
		'icon': getRootPath() + '/plugs/alert/img/right.png',
		'content': "正在导出请稍等...",
		'closeTime': 5000,
	})
	window.location.href = "inventoryTransfer/export?search=" + search + "&start=" + start + "&end=" + end ;

}



function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type: 'GET',
		url: 'inventoryTransfer/columns',
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