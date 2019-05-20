/**
 * 批量导入
 * 
 * @returns
 */
function batchImput() {
	$("#mybatchUpload").modal('show');

}


$(document)
		.ready(
				function() {
					$('#submit')
							.bind(
									'click',
									function() {
										var eventFun = function() {
											$
													.ajax({
														type : 'GET',
														url : 'unit/uploadprocess',
														data : {},
														dataType : 'json',
														success : function(data) {
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
				});
