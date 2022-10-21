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
		$("#repairManagementForm").validate({
			rules : {

				name : {
					required : true,
				},
				repairNum : {
					required : true,
					number:true
				},
				
			
			},
			messages : {
				
				name : {
					required : a + "请输入返修设备名称"
				},
				repairNum : {
					required : a + "请输入返修数量",
					number : a + "请输入正确的数量"
				}

			
			}
		});
		
	
		
	});


