
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#supplierForm").validate({
			rules : {
				name : {
					required : true,
					remote : {
						url : getRootPath() + "/supplier/ajaxgetRepletes",
						type : "POST",
						data : {
							name : function() {
								return $("#name").val();
							}
						},
						dataType : "json",
						dataFilter : function(data, type) {
							var oldname = $("#oldname").val();
							var name = $("#name").val();
							if(oldname == name){
								return true;
							}
							var jsondata = $.parseJSON(data);
							if (jsondata.status == 200) {
								return true;
							}
							return false;
						}
					}
				},
				upload : {
					required : true
				},
				"systemClassification.id" : {
					required : true
				}
			},
			messages : {
				name : {
					required : a + "请输入供应商名称",
					remote : a + "当前供应商已经存在！"
				},
				upload : {
					required : a+ "导入文件不能为空！"
				},
				"systemClassification.id" : {
					required : a+ "请选择系统分类"
				}
				
			}
		});
		
		
		
		
		
	});




















