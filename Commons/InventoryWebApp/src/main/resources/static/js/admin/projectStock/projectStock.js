
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#projectStockForm").validate({
			rules : {
				name : {
					required : true,
				},
				projectedProcurementVolume : {
					number:true
				},
				estimatedUnitPrice : {
					number:true
				},
				actualPurchaseQuantity : {
					number:true
				},
				realCostUnitPrice : {
					number:true
				},
				paymentAmount : {
					number:true
				},
	/*			num : {
					number:true,
				},*/
			
			},
			messages : {
				name : {
					required : a + "请输入设备名称"
				},
				projectedProcurementVolume : {
					number : a + "请输入正确的预计采购量"
				},
				estimatedUnitPrice : {
					number : a + "请输入正确的预计采购单价"
				},
				actualPurchaseQuantity : {
					number : a + "请输入正确的实际采购量"
				},
				realCostUnitPrice : {
					number : a + "请输入正确的实际采购单价"
				},
				paymentAmount : {
					number : a + "请输入正确的付款金额"
				},
				num : {
					number : a + "请输入正确的出库数量",
				}
			
			}
		});
		
		
//		jQuery.validator.addMethod("checkNum", function(value, element, param) {
//			if(value == ""){
//				return true;
//			}
//			return parseInt(value) <= parseInt($(param).val());
//        }, $.validator.format("出库数量不能大于实际采购数量!"));
//		
//		
//		jQuery.validator.addMethod("checkNum2", function(value, element, param) {
//			if(value == ""){
//				return true;
//			}
//			return parseInt(value) >= parseInt($(param).val());
//		}, $.validator.format("出库数量不能大于实际采购数量!"));
//		
		
	});



$(".datepicker").datepicker({
	language : "zh-CN",
	format : "yyyy-mm-dd",
	keyboardNavigation : false,
	forceParse : false,
	autoclose : true
});







