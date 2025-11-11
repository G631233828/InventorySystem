$().ready(function() {
	// 假设出库数量和实际出库数量的输入框的 name 属性分别为 "stockQuantity" 和 "actualOutbound"
var stockQuantityElement = $("[name='estimatedIssueQuantity']");
var actualOutboundElement = $("[name='actualIssueQuantity']");

// 定义一个工具函数：安全转换为数字（空值/非数字默认返回0）
function toNumber(value) {
    // 处理空字符串或null
    if (value === null || value === undefined || value.trim() === '') {
        return 0;
    }
    // 尝试转换为数字，失败则返回0
    const num = parseFloat(value);
    return isNaN(num) ? 0 : num;
}

// 定义验证规则：lessThanRemaining（支持小数，处理空值/NaN）
jQuery.validator.addMethod("lessThanRemaining", function(value, element) {
    // 转换输入值为数字（当前验证的输入框的值）
    const currentValue = toNumber(value);
    
    // 转换出库数量和实际出库数量（处理空值和非数字）
    const stockQuantity = toNumber(stockQuantityElement.val());
    const actualOutbound = toNumber(actualOutboundElement.val());

    // 计算剩余数量（保留小数精度，避免浮点数计算误差）
    const remaining = Number((stockQuantity - actualOutbound).toFixed(6)); // 临时保留6位小数减少误差

    // 验证规则：当前值必须 <= 剩余数量，且当前值必须为非负数
    return currentValue >= 0 && currentValue <= remaining;
}, function(params, element) {
    // 动态生成错误信息，显示实际剩余数量（保留2位小数）
    const stockQuantity = toNumber(stockQuantityElement.val());
    const actualOutbound = toNumber(actualOutboundElement.val());
    const remaining = Number((stockQuantity - actualOutbound).toFixed(2));
    return `输入的数字必须小于等于剩余数量（${remaining}）`;
});


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
			accepter: {
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
						},
						pickId: function(){
							return  $('input[name="id"]').val();
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
			accepter: {
				required: a + "领料人不能为空",
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
				console.log(data)
				
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




//根据选择的项目来获取项目经理 项目助理
function getpname() {
	$.ajax({
		dataType: "json",
		type: "POST",
		url: getRootPath() + "/pName/ajaxgetPname",
		data: "id=" + $("#number-multiple1").val(),
		success: function(data) {
 		$("#projectManager").html('');
			if (data.status == 200) {
				var pname = data.data;
			var nameArray = pname.pm.split("/");
            // 遍历数组生成下拉选项
            $.each(nameArray, function(index, value){
                $("#projectManager").append(
                    $("<option></option>").val(value).text(value)
                );
            });
				
				//$("#projectManager").val(pname.pm);
				$("#projectAssistant").val(pname.assistant);
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






$("#pickUpTime").fdatepicker({
	format: 'yyyy-mm-dd hh:ii',
	pickTime: true
});














