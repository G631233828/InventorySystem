var flag = true;
$().ready(function() {

		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#stockStatisticsForm").validate({
			
			submitHandler:function(form){
				if (flag) {
					flag = false;
				// 校验通过后通过ajax的方式提交
				$.ajax({
					dataType:"json",
					type:"POST",
					url: getRootPath() + "/stockStatistics/in",
					data:$("#stockStatisticsForm").serialize(),
					success:function(data){
						if(data.status == 200){
							flag = true;
							var  newInventory = data.data.newNum;
							var  remainingNum = data.data.remainingNum;
							var id = $("#stockId").val();
							if(newInventory > 5){
								$("#inventory_"+id).css("color","green");
							}else if(newInventory ==0){
								$("#inventory_"+id).css("color","red");
							}else{
								$("#inventory_"+id).css("color","blue");
							}
							if(remainingNum > 5){
								$("#remainingNum_"+id).css("color","green");
							}else if(remainingNum ==0){
								$("#remainingNum_"+id).css("color","red");
							}else{
								$("#remainingNum_"+id).css("color","blue");
							}
							
							$("#inventory_"+id).text(newInventory);
							$("#remainingNum_"+id).text(remainingNum);
							$("#mystockStatistics").modal('hide');
							
							// 判断是否已存在，如果已存在则直接显示
							jqueryAlert({
							    'icon'    : getRootPath() +'/plugs/alert/img/right.png',
							    'content' : data.msg,
							    'closeTime' : 2000,
							})
						}else{
							flag = true;
							// 判断是否已存在，如果已存在则直接显示
						 jqueryAlert({
							    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
							    'content' : data.msg,
							    'closeTime' : 2000,
							})
						}
						
					}
				});
			}},
			
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
















