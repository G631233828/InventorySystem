/**
 * 批量导入
 * 
 * @returns
 */
function batchImput() {
	$("#mybatchUpload").modal('show');

}
function showColumn() {
	$("#mycolumn").modal('show');

}





function searchVal() {
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	window.location.href="suppliers?pageSize="+pageSize+"&search="+search;

}
function searchSize() {
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	window.location.href="suppliers?pageSize="+pageSize+"&search="+search;
	
}






function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "."+o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type : 'GET',
		url : 'supplier/columns',
		data :  "column=" + o+"&flag="+flag,
		dataType : 'json',
		success : function(data) {
				if(data.status == 200){
					//$(td).attr("checked":flag);
					if(flag){
						$(th).show();
					}else{
						$(th).hide();
					}
				}
			}
		})
		
	

}

$(document)
		.ready(
				function() {
					if ($("#upload").val() != "") {

						$('#submit')
								.bind(
										'click',
										function() {
											var eventFun = function() {
												$
														.ajax({
															type : 'GET',
															url : 'supplier/uploadprocess',
															data : {},
															dataType : 'json',
															success : function(
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
