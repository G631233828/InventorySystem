
$().ready(function() {
		console.log("newCustomer.js已启用")
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#newCustomerForm").validate({
			rules : {
				name : {
					required : true,
					remote : {
						url : getRootPath() + "/newCustomer/ajaxgetRepletes",
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
			},
			messages : {
				name : {
					required : a + "请输入客户名称",
					remote : a + "当前客户已经存在！"
				}
				
			}
		});
	});

