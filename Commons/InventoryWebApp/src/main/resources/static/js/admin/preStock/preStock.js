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














