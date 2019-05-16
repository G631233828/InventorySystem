
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#systemClassificationForm").validate({
			rules : {
				name : {
					required : true,
					minlength : 2,
					remote : {
						url : getRootPath() + "/systemClassification/ajaxgetRepletes",
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
					required : a + "请输入系统分类名称",
					minlength : a + "系统分类名称长度至少是2个",
					remote : a + "当前系统分类已经存在！"
				},
				
			}
		});
		
		
		
		var options = [], _options;
		/* <![CDATA[ */
			 var data = [[${categorys}]]
			 /*]]>*/
			  for (var i = 0; i < data.length; i++) {
			    var option = '<option value="' + data[i].id + '">' + data[i].name + '</option>';
			    options.push(option);
			  }
			  _options = options.join('');
			  $('#number-multiple')[0].innerHTML = _options;
					var a = /* <![CDATA[ */ [[${selectCategorys}]] /*]]>*/
					     $("#number-multiple").selectpicker('val',a);
						  $("#number-multiple").selectpicker('refresh');
					  $('#number-multiple').selectpicker('render')
				  
			  
		
		
		
		
		
		
	});




















