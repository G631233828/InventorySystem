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


/**
 * 查找设备信息
 * @param o
 * @returns
 */
function showStockStatistics(o) {
	$(".stockval").val('');
	$(".stockval").text('');
	
	$.ajax({
		type : 'POST',
		url : 'stockStatistics/findStock',
		data : "id=" + o ,
		dataType : 'json',
		success : function(data) {
			if (data.status == 200) {
				var datas = data.data;
				var name = datas.name;
				var inventory = datas.inventory;
				var stockid = datas.id;
				
				$("#stockId").val(stockid);
				$("#stockName").text(name);
				$("#loadinventory").text(inventory);
				
			}
		}
	})
	$("#mystockStatistics").modal('show');
}

function showSupplier(o) {
	$(".supplier2").text('');
	$.ajax({
		type : 'POST',
		url : 'stock/getSupplier',
		data : "id=" + o ,
		dataType : 'json',
		success : function(data) {
			if (data.status == 200) {
				var name = data.data.name;
				var systemClassification = data.data.systemClassification.name;
				var categorys='';
				$.each(data.data.categorys,function(key,value){
					categorys+=value.name+",  ";
				})
				var brand = data.data.brand.name;
				var contact = data.data.contact;
				var contactNumber = data.data.contactNumber;
				var qq = data.data.qq;
				var wechat = data.data.wechat;
				var email = data.data.email;
				var address = data.data.address;
				var introducer = data.data.introducer;
				var productQuality = data.data.productQuality;
				var supplyPeriod = data.data.supplyPeriod;
				var payMent = data.data.payMent;
				var afterSaleService = data.data.afterSaleService;
				
				$("#supplier_name").text(name);
				$("#supplier_systemClassification").text(systemClassification);
				$("#supplier_categorys").text(categorys);
				$("#supplier_brand").text(brand);
				$("#supplier_contact").text(contact);
				$("#supplier_contactNumber").text(contactNumber);
				$("#supplier_qq").text(qq);
				$("#supplier_wechat").text(wechat);
				$("#supplier_email").text(email);
				$("#supplier_address").text(address);
				$("#supplier_introducer").text(introducer);
				$("#supplier_productQuality").text(productQuality);
				$("#supplier_").text(systemClassification);
				$("#supplier_supplyPeriod").text(supplyPeriod);
				$("#supplier_payMent").text(payMent);
				$("#supplier_afterSaleService").text(afterSaleService);
				
			}
		}
	})
	
	
	$("#mysupplier").modal('show');
	
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
	window.location.href="stocks?pageSize="+pageSize+"&search="+search;

}
function searchSize() {
	
	var pageSize = $("#pageSize").val();
	var search = $("#serach").val();
	window.location.href="stocks?pageSize="+pageSize+"&search="+search;
	
}

function selectColumn(o) {

	var td = "#ch_" + o;
	var th = "." + o;
	var flag = $(td).is(':checked');
	flag = flag == false ? true : false;
	$(td).prop("checked", flag);

	$.ajax({
		type : 'GET',
		url : 'stock/columns',
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

$(document).ready( function() {
					if ($("#upload").val() != "") {

						$('#submit')
								.bind(
										'click',
										function() {
											var eventFun = function() {
												$
														.ajax({
															type : 'GET',
															url : 'stock/uploadprocess',
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





















