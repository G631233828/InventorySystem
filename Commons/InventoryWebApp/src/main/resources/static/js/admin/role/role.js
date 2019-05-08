
	$("#commentForm").validate();
	var a = "<i class='fa fa-times-circle'></i> ";
	$("#roleForm").validate({
		rules : {
			roleName : {
				required : true,
				minlength : 2,
				remote : {
					url :"role/ajaxgetRepletes",
					type : "POST",
					data : {
						roleName : function() {
								return  $("#roleName").val() ;
						}
					},
					dataType : "json",
					dataFilter : function(data, type) {
						
						var hidroleName = $("#hidroleName").val();
						var roleName = $("#roleName").val();
						if(roleName == hidroleName){
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

		},
		messages : {
			roleName : {
				required : a + "请输入角色名称",
				minlength : a + "用户名必须两个字符以上",
				remote   : a + "不能添加重复的角色"
			},
		}
	});

	
	
	
	
