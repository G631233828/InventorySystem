$().ready(function() {



	 // 假设出库数量和实际出库数量的输入框的 name 属性分别为 "stockQuantity" 和 "actualOutbound"
    var stockQuantityElement = $("[name='estimatedIssueQuantity']");
    var actualOutboundElement = $("[name='actualIssueQuantity']");

    // 定义一个新的验证规则：lessThanRemaining
    jQuery.validator.addMethod("lessThanRemaining", function(value, element) {
        // 确保出库数量和实际出库数量都已经输入且为数字
        var stockQuantity = parseFloat(stockQuantityElement.val());
        var actualOutbound = parseFloat(actualOutboundElement.val());

        if (isNaN(stockQuantity) || isNaN(actualOutbound)) {
            return false; // 如果任何一个不是数字，验证失败
        }

        // 计算剩余数量
        var remaining = stockQuantity - actualOutbound;

        // 验证输入的数字是否小于剩余数量
        return value <= remaining;
    }, "输入的数字必须小于剩余数量。");
	



	$("#commentForm").validate();
	var a = "<i class='fa fa-times-circle'></i> ";
	$("#pickUpApplicationForm").validate({
		rules: {
			"area.id": {
				required: true,
			},
			"pickUpApplication.id": {
				required: true,
			},
			estimatedIssueQuantity: {
				required: true,
				remote: {
					url: getRootPath() + "/checkStockNum",
					type: "POST",
					data: {
						num: function() {
							return $("#estimatedIssueQuantity").val();
						},
						stockId: function() {
							return $("#number-multiple").val();
						}
					},
					dataType: "json",
					dataFilter: function(data, type) {
						var jsondata = $.parseJSON(data);
						if (jsondata.status == 200) {
							return true;
						}
						if (jsondata.status == 400) {
							jqueryAlert({
								'icon': getRootPath() + '/plugs/alert/img/error.png',
								'content': jsondata.msg,
								'closeTime': 5000,
							})
							return false;
						}

						return false;
					}
				}
			},

		},
		messages: {
			"area.id": {
				required: a + "请选择区域",
			},
			"pickUpApplication.id": {
				required: a + "请选择预出库设备",
			},
			estimatedIssueQuantity: {
				required: a + "请输入预计出库数量！",
				remote: a + "当前出库数量有误，请检查库存！"
			},
		},
		foucusCleanup: true,
	});
	
	
	
	

	
	
	
	$("#pickUpApplicationAddForm").validate({
	
		rules: {
			"area.id": {
				required: true,
			},
			"pickUpApplication.id": {
				required: true,
			},
			num: {
				required: true,
                lessThanRemaining: true // 使用自定义验证规则
			},

		},
		messages: {
			"area.id": {
				required: a + "请选择区域",
			},
			"pickUpApplication.id": {
				required: a + "请选择预出库设备",
			},
			num: {
				required: a + "请输入出库数量！",
				lessThanRemaining: "输入的数字必须小于剩余数量"
			},
		},
		foucusCleanup: true,
		submitHandler: function(form) {
		  $('#submit').prop('disabled', true);
			$.ajax({
				dataType: "json",
				type: "POST",
				url: getRootPath() + "/pickUpApplicationAdd",
				data: $("#pickUpApplicationAddForm").serialize(),
				success: function(data) {
					if (data.status == 200) {
						// 判断是否已存在，如果已存在则直接显示
						jqueryAlert({
							'icon': getRootPath() + '/plugs/alert/img/right.png',
							'content': data.msg,
							'closeTime': 2000,
						})

						setTimeout(function() {
							window.location.href = getRootPath() + "/pickUpApplications";
						}, 2000);

					} else {
						// 判断是否已存在，如果已存在则直接显示
						jqueryAlert({
							'icon': getRootPath() + '/plugs/alert/img/error.png',
							'content': data.msg,
							'closeTime': 2000,
						})
					}


				}
			});
			setTimeout(function() {
                $('#submit').prop('disabled', false);
            }, 13000); // 3000 毫秒后启用按钮
		}

	});

});












function toPickPage() {
	window.location.href = getRootPath() + "/pickUpApplications";
}


function getStocks() {
	var areaId = $("#area").val();
	// 需要通过ajax加载对应的菜单列表
	$.ajax({
		type: 'POST',
		url: getRootPath() + "/getStocks",
		data: "areaId=" + areaId,
		dataType: "json",
		success: function(data) {
			if (data.status == 200) {
				var options = [], _options;
				$.each(data.data, function(index, item) {
					var model = item.model != "" ? "-" + item.model : "";
					var option = '<option value="' + item.id + '">' + item.name + model + '</option>';
					options.push(option);
				});
				_options = options.join('');
				$('#number-multiple')[0].innerHTML = _options;
				$("#number-multiple").selectpicker('refresh');
				$('#number-multiple').selectpicker('render');
			} else {
				jqueryAlert({
					'icon': getRootPath() + '/plugs/alert/img/error.png',
					'content': data.msg,
					'closeTime': 5000,
				})
			}


		}
	});
}

function getModel() {
	var name = $("#number-multiple option:selected").text();
	var model = name.substring(name.indexOf("-") + 1, name.length);
	$("#model").val(model)


}







$("#pickUpTime").fdatepicker({
	format: 'yyyy-mm-dd hh:ii',
	pickTime: true
});














