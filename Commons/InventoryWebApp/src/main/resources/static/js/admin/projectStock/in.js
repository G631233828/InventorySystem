
$().ready(function() {

		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#stockStatisticsForm").validate({
			submitHandler:function(form){
				// 校验通过后通过ajax的方式提交
				$.ajax({
					dataType:"json",
					type:"POST",
					url: getRootPath() + "/projectStockStatistics/in",
					data:$("#stockStatisticsForm").serialize(),
					success:function(data){
						if(data.status == 200){
							var  newInventory = data.data.newNum;
							var  newactualPurchaseQuantity = data.data.actualPurchaseQuantity;
							var id = $("#projectStockId").val();
							if(newInventory > 5){
								$("#inventory_"+id).css("color","green");
							}else if(newInventory ==0){
								$("#inventory_"+id).css("color","red");
							}else{
								$("#inventory_"+id).css("color","blue");
							}
							$("#inventory_"+id).text(newInventory);
							$("#actualPurchaseQuantity_"+id).text(newactualPurchaseQuantity);
							$("#myprojectStockStatistics").modal('hide');
							
							// 判断是否已存在，如果已存在则直接显示
							jqueryAlert({
							    'icon'    : getRootPath() +'/plugs/alert/img/right.png',
							    'content' : data.msg,
							    'closeTime' : 2000,
							})
						}else{
							// 判断是否已存在，如果已存在则直接显示
						 jqueryAlert({
							    'icon'    : getRootPath() +'/plugs/alert/img/error.png',
							    'content' : data.msg,
							    'closeTime' : 2000,
							})
						}
						
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
















