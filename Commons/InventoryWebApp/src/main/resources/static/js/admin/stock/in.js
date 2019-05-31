
$().ready(function() {

		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#stockStatisticsForm").validate({
			
			submitHandler:function(form){
				alert(1)
				// 校验通过后通过ajax的方式提交
				$.ajax({
					dataType:"json",
					type:"POST",
					url: getRootPath() + "/stockStatistics/in",
					data:$("#stockStatisticsForm").serialize(),
					success:function(data){
						//TODO 
						alert(data.status)
					}
				});
			},
			
			rules : {
				num : {
					required : true,
					number : true,
					digits : true
					}
			},
			messages : {
				num : {
					required : a + "请输入入库数量！",
					number : a + "请输入一个合法的数字！",
					digits : a + "请输入整数！"
				}
			}
			
		});


})
















