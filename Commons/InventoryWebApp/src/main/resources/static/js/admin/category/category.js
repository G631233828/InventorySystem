
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#categoryForm").validate({
			rules : {
				name : {
					required : true,
					minlength : 2,
					remote : {
						url : getRootPath() + "/category/ajaxgetRepletes",
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
					}/*,
					upload : {
						required : true
					}*/
				},
			},
			messages : {
				name : {
					required : a + "请输入类目名称",
					minlength : a + "类目名称长度至少是2个",
					remote : a + "当前类目已经存在！"
				}/*,
				upload : {
					required : a+ "导入文件不能为空！"
				}
				*/
			}
		});
	});

