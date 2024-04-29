$().ready(function() {
	$("#commentForm").validate();
	var a = "<i class='fa fa-times-circle'></i> ";
	$("#preStockForm").validate({
		rules : {
			"area.id" : {
				required : true,
			},
			name : {
				required : true,
			},
			entryName:{
				required : true,
				remote : {
					url : getRootPath() + "/pName/ajaxgetRepletes",
					type : "POST",
					data : {
						name : function() {
							return $("#entryName").val();
						},
						type:"true"
					},
					dataType : "json",
					dataFilter : function(data, type) {
						// var oldname = $("#oldname").val();
						// var name = $("#name").val();
						// if(oldname == name){
						// 	return true;
						// }
						var jsondata = $.parseJSON(data);
						if (jsondata.status == 206) {
							return true;
						}
						return false;
					}
				}
			},

			upload : {
				required : true
			},
			estimatedInventoryQuantity : {
				required : true
			},
			actualReceiptQuantity : {
				required : true,
				min: 1
			},
			
		},
		messages : {
			"area.id":{
				required :a+"请选择区域",
			},
			name : {
				required : a + "请输入设备名称",
			},
			entryName : {
				required : a + "请输入项目名称",
				remote : a + "不存在该项目，请先添加！"
			},
			upload : {
				required : a + "导入文件不能为空！"
			},
			estimatedInventoryQuantity : { 
				required : a + "请输入预计入库数量！"
			},
			actualReceiptQuantity : { 
				required : a + "请输入实际入库数量！",
				min : a+ "请输入正确的实际入库数量！"
			},
		},
		success: function(form) {

		},
		submitHandler:function(form){
			var idValue = $('#preStockForm input[name="id"]').first().val();
			if(typeof idValue === 'undefined'){
				//添加时判断预库存中是否有同样的预库存
			//进行ajax传值
			$.ajax({
				url: getRootPath() + "/prestock/ajaxgetRepletes",
				type: "post",
				dataType: "json",
				data: {
					name: function() {
						return $("#name").val();
					},
					areaId: function() {
						return $("#area").val();
					},
					model: function() {
						return $("#model").val();
					},
					supplierId: function() {
						return $("#number-multiple").val();
					},
					entryname: function (){
						return $("#entryName").val();
					}
				},
				success: function(msg) {
					if (msg.status == 206) {
						$("#name-error").html("当前区域/供应商下的设备名称已存在！");
						$("#nameform").addClass("has-error")

						$("#model-error").html("当前区域/供应商下此设备名称已存在该型号！");
						$("#modelform").addClass("has-error")
						return false;
					} else if (msg.status == 200) {
						$("#areaform").removeClass("has-error").addClass("has-success")
						$("#nameform").removeClass("has-error").addClass("has-success")
						$("#modelform").removeClass("has-error").addClass("has-success")
						$("#supplierform").removeClass("has-error").addClass("has-success")

						$("#name-error").html("");
						$("#area-error").html("");
						$("#model-error").html("");
						$("#number-multiple-error").html("");
						form.submit();
					}
					return false;

				}
			});
			}else{
				form.submit();
			}



		},
		foucusCleanup:true,
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




$("#estimatedWarehousingTime").fdatepicker({
	format: 'yyyy-mm-dd hh:ii',
	pickTime: true
});

$("#purchaseInvoiceDate").fdatepicker({
	format: 'yyyy-mm-dd',
	pickTime: true
});













