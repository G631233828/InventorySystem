$().ready(function() {
	$("#commentForm").validate();
	var a = "<i class='fa fa-times-circle'></i> ";
	$("#goodsStorageForm").validate({
		rules : {
			companys : {
				required : true,
			},
			address : {
				required : true,
				minlength : 2,
			},
			shelfNumber : {
				required : true,
				minlength : 1
				remote : {
					url : getRootPath() + "/goodsStorage/ajaxgetRepletes",
					type : "POST",
					data : {
						"companys.id" : function() {
							return $("#companys").val();
						},
						address : function() {
							return $("#address").val();
						},
						shelfNumber : function() {
							return $("#shelfNumber").val();
						},
						shelflevel : function() {
							return $("#shelflevel").val();
						},
					},
					dataType : "json",
					dataFilter : function(data, type) {
						var oldshelflevel = $("#oldshelflevel").val();
						var shelflevel = $("#shelflevel").val();
						if(oldshelflevel == shelflevel){
							return true;
						}
						var jsondata = $.parseJSON(data);
						alert(jsondata.status)
						if (jsondata.status == 200) {
							return true;
						}
						return false;
					}
				}
			},
			shelflevel : {
				required : true,
				remote : {
					url : getRootPath() + "/goodsStorage/ajaxgetRepletes",
					type : "POST",
					data : {
						"companys.id" : function() {
							return $("#companys").val();
						},
						address : function() {
							return $("#address").val();
						},
						shelfNumber : function() {
							return $("#shelfNumber").val();
						},
						shelflevel : function() {
							return $("#shelflevel").val();
						},
					},
					dataType : "json",
					dataFilter : function(data, type) {
						var oldshelflevel = $("#oldshelflevel").val();
						var shelflevel = $("#shelflevel").val();
						if(oldshelflevel == shelflevel){
							return true;
						}
						var jsondata = $.parseJSON(data);
						alert(jsondata.status)
						if (jsondata.status == 200) {
							return true;
						}
						return false;
					}
				}
			},
		},
		messages : {
			"companys.id" : {
				required : a + "请选择货架所属企业",
			},
			address : {
				required : a + "请输入货架地址",
				minlength : a + "货架地址长度至少是2个"
			},
			shelfNumber : {
				required : a + "请输入货架编号",
				minlength : a + "货架编号长度至少是2个",
				remote : a + "货架层在当前企业中已经存在！"
			},
			shelflevel : {
				required : a + "请输入货架层",
				remote : a + "货架层在当前企业中已经存在！"
			}

		}
	});
});



$("#shelfNumber").keyup(function(){
	$("#goodsStorageForm").validate().element($("#shelflevel"))
})

function checkshelf(){
	$("#goodsStorageForm").validate().element($("#shelflevel"))
}

$("#shelflevel").keyup(function(){
	$("#goodsStorageForm").validate().element($("#shelflevel"))
})




