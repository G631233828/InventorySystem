$().ready(function() {
	$("#commentForm").validate();
	var a = "<i class='fa fa-times-circle'></i> ";
	$("#stockForm").validate({
		rules : {
			"area.id" : {
				required : true,
//				remote : {
//					url : getRootPath() + "/stock/ajaxgetRepletes",
//					type : "POST",
//					data : {
//						name : function() {
//							return $("#name").val();
//						},
//						areaId : function() {
//							return $("#area").val();
//						},
//						model : function() {
//							return $("#model").val();
//						}
//					},
//					dataType : "json",
//					dataFilter : function(data, type) {
//						var oldarea = $("#oldarea").val();
//						var area = $("#area").val();
//						if (oldarea == area) {
//							return true;
//						}
//						var jsondata = $.parseJSON(data);
//						if (jsondata.status == 206) {
//							return false;
//						}
//						return true;
//					}
//				}
			},
			name : {
				required : true,
//				remote : {
//					url : getRootPath() + "/stock/ajaxgetRepletes",
//					type : "POST",
//					data : {
//						name : function() {
//							return $("#name").val();
//						},
//						areaId : function() {
//							return $("#area").val();
//						},
//						model : function() {
//							return $("#model").val();
//						}
//					},
//					dataType : "json",
//					dataFilter : function(data, type) {
//						var oldname = $("#oldname").val();
//						var name = $("#name").val();
//						if (oldname == name) {
//							return true;
//						}
//						var jsondata = $.parseJSON(data);
//						if (jsondata.status == 206) {
//							return false;
//						}
//						return true;
//					}
//				}
			},
			
			upload : {
				required : true
			},
			 model : {

				  remote : {
						url : getRootPath() + "/stock/ajaxgetRepletes",
						type : "POST",
						data : {
							name : function() {
								return $("#name").val();
							},
							areaId : function() {
								return $("#area").val();
							},
							model : function() {
								return $("#model").val();
							}
						},
						dataType : "json",
						dataFilter : function(data, type) {
							var oldmodel = $("#oldmodel").val();
							var model = $("#model").val();
							if (oldmodel == model) {
								return true;
							}
							var jsondata = $.parseJSON(data);
							if (jsondata.status == 206) {
								return false;
							}
							return true;
						}
					}
				  
			  
			  
			  },
			 
			/*
			 * scope : { required : true },
			 */
			/*
			 * "goodsStorage.id" : { required : true },
			 */
			price : {
				number : true
			},
			/*
			 * maintenance : { required : true },
			 */
			/*
			 * "supplier.id" : { required : true },
			 */
//			"stock.id" : {
//				required : true
//			}
//			
		},
		messages : {
			"area.id":{
				required :a+"请选择区域",
				remote : a + "当前区域下设备已经存在！"
			},
			name : {
				required : a + "请输入设备名称",
				remote : a + "当前设备已经存在！"
			},
			upload : {
				required : a + "导入文件不能为空！"
			},
			 model : { 
				 remote : a + "相同型号下设备已经存在！"
			 },
			 
			/*
			 * scope : { required : a+ "请输入设备使用范围" },
			 */
			/*
			 * "goodsStorage.id" : { required : a+ "请选择存放货架位置" },
			 */
			price : {
				required : a + "请输入设备单价"
			},
			/*
			 * maintenance : { required : a+ "请输入设备维保" },
			 */
			/*
			 * "supplier.id" : { required : a+ "请选择设备供应商" },
			 */
//			"stock.id" : {
//				required : a + "请选择设备"                                                         
//			},
		
			
		},
success:function(form){

	//进行ajax传值
	$.ajax({
	　　url : getRootPath() + "/stock/ajaxgetRepletes",
	　　type : "post",
	　　dataType : "json",
	　　data: {
	　　　　name : function() {
		return $("#name").val();
		},
		areaId : function() {
			return $("#area").val();
		},
		model : function() {
			return $("#model").val();
		}
	　　},
	　　success : function(msg) {
			
		if(msg.status == 206){
			var model = $("#model").val();
			var name= $("#name").val();
			if(model ==""){
				$("#name-error").html("当前区域下的设备名称已存在！");
				$("#nameform").addClass("has-error")
				return false;
			}else{
				$("#model-error").html("当前区域下此设备名称已存在该型号！");
				$("#modelform").addClass("has-error")
				return false;
			}
		}else if(msg.status=200){
			$("#areaform").removeClass("has-error").addClass("has-success")
			$("#nameform").removeClass("has-error").addClass("has-success")
			$("#modelform").removeClass("has-error").addClass("has-success")
			$("#name-error").html("");
			$("#area-error").html("");
			$("#model-error").html("");
		}
		
		
	　　}
	});
	

},
		//submitHandler:function(form){}
		
	});


});



function getgoodsStorages() {
	var areaId = $("#area").val();
		// 需要通过ajax加载对应的菜单列表
		$.ajax({
			type : 'POST',
			url : "getStorages",
			data : "areaId=" + areaId,
			dataType : "json",
			success : function(data) {
				var sale = "<option value=''>----选择货架----</option>";
				$.each(data.data, function(index, item) {
					
					var i =item.shelflevel==""?"":"/";
					sale += "<option value=" + item.id + ">" +item.address+"-->" +item.shelfNumber
							+ i+item.shelflevel+"</option>";
				});
				$("#goodsStorage").html(sale)
			}
		});
}








