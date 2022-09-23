$(".datepicker").datepicker({
	language: "zh-CN",
	format: "yyyy-mm-dd",
	keyboardNavigation: false,
	forceParse: false,
	autoclose: true
});


$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#inventoryTransferForm").validate({
			rules : {

				name : {
					required : true,
				},
				transferQuantity : {
					required : true,
					number:true
				},
				
			
			},
			messages : {
				
				name : {
					required : a + "请输入设备名称"
				},
				transferQuantity : {
					required : a + "请输入流转库存数量",
					number : a+"请输入正确的库存流转数量"
				},
			
			}
		});
		
	
		
	});


