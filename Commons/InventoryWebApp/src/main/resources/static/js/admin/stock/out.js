
$().ready(function() {

		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#OutstockStatisticsForm").validate({
			submitHandler:function(form){
				// 校验通过后通过ajax的方式提交
				$.ajax({
					dataType:"json",
					type:"POST",
					url: getRootPath() + "/stockStatistics/out",
					data:$("#OutstockStatisticsForm").serialize(),
					success:function(data){
						if(data.status == 200){
							var  newInventory = data.data.newNum;
							var id = $("#stockIdOut").val();
							if(newInventory > 5){
								$("#inventory_"+id).css("color","green");
							}else if(newInventory ==0){
								$("#inventory_"+id).css("color","red");
							}else{
								$("#inventory_"+id).css("color","blue");
							}
							
							$("#inventory_"+id).text(newInventory);
							$("#mystockStatistics2").modal('hide');
							
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
					required : a + "请输入出库数量！",
					number : a + "请输入一个合法的数字！",
					digits : a + "请输入整数！"
				}
			}
			
		});


})




//
//
//// 检查库存是否足够
//jQuery.validator.checkNum("checkNum", function(value, element) {
//	var oldVal = $("#"+element).val();
//	if(value > oldVal){
//		return false ;
//	} 
//	return true ;
//}, "出库数量不能大于当前库存数量");
//











