
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#unitForm").validate({
			rules : {
				name : {
					required : true,
					remote : {
						url : getRootPath() + "/unit/ajaxgetRepletes",
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
					},
					upload : {
						required : true
					}
				},
			},
			messages : {
				name : {
					required : a + "请输入单位名称",
					remote : a + "当前单位名称已经存在！"
				},
				upload : {
					required : a+ "导入文件不能为空！"
				}
				
			}
		});
	});

