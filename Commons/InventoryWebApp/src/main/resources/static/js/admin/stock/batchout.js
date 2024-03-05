//data : $("#BatchOutstockStatisticsForm").serialize(),
//批量出库提交
function batchFormSubmit() {
var tp = $("#type").val();
alert(tp)
if(tp == "1"){
$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/stockStatistics/batchOut",
		data: $("#BatchOutstockStatisticsForm").serialize(),
		success: function(data) {
			if (data.status == 200) {
				$.each(data.data, function(index, item) {
					var newInventory = item.newNum;
					var remainingNum = item.remainingNum;
					if (newInventory > 5) {
						$("#inventory_" + item.id).css("color", "green");
					} else if (newInventory == 0) {
						$("#inventory_" + item.id).css("color", "red");
					} else {
						$("#inventory_" + item.id).css("color", "blue");
					}
					if (remainingNum > 5) {
						$("#remainingNum_" + item.id).css("color", "green");
					} else if (remainingNum == 0) {
						$("#remainingNum_" + item.id).css("color", "red");
					} else {
						$("#remainingNum_" + item.id).css("color", "blue");
					}

					$("#inventory_" + item.id).text(newInventory);
					$("#remainingNum_" + item.id).text(remainingNum);
				});
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})

				$("#mystockbatchout").modal('hide');
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
			}


		}
	});
}else if(tp =="2"){
$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/pickUpApplication/batchOut",
		data: $("#BatchOutstockStatisticsForm").serialize(),
		success: function(data) {
			if (data.status == 200) {
			
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/right.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				window.location.href="pickUpApplications";
			
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
			}


		}
	});

}
	


}


//校验库存
function setStockNum(o) {

	var val = $("#stocknum_" + o).val();

	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/stock/checkNum",
		data: "id=" + o + "&num=" + val,
		success: function(data) {

			if (data.status == 200) {


			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 2000,
				})
				$("#stocknum_" + o).val('')

			}

		}
	});


}








function getprojectNames2() {
	// 需要通过ajax加载对应的菜单列表
	$.ajax({
		type: 'POST',
		url: getRootPath() + "/stock/getProjectNames",
		dataType: "json",
		success: function(data) {
			var projectNames = '';
			$.each(data.data, function(index, item) {
				projectNames += "<li><a href='#'" + "onclick=setbatchProjectName('"
					+ item + "')>" + item + "</a></li>"
			});
			$("#batchmenu").html(projectNames)
		}
	});
}

//
//
// // 检查库存是否足够
// jQuery.validator.checkNum("checkNum", function(value, element) {
// var oldVal = $("#"+element).val();
// if(value > oldVal){
// return false ;
// }
// return true ;
// }, "出库数量不能大于当前库存数量");
//

