
$().ready(function() {
		$("#commentForm").validate();
		var a = "<i class='fa fa-times-circle'></i> ";
		$("#productForm").validate({
			rules : {
				name : {
					required : true,
					minlength : 2
				},
				types : {
					required : true,
				},
			},
			messages : {
				name : {
					required : a + "请输入产品名称",
					minlength : a + "产品名称长度至少是2个"
				},
				types : {
					required : a + "至少选择一个分类,缺少分类可从案例分类中添加",
				},
				
			}
		});
	});

	// 手机号码验证
	jQuery.validator.addMethod("isMobile", function(value, element) {
	    var length = value.length;
	    var mobile = /^(13[0-9]{9})|(18[0-9]{9})|(14[0-9]{9})|(17[0-9]{9})|(15[0-9]{9})$/;
	    return this.optional(element) || (length == 11 && mobile.test(value));
	}, "请正确填写您的手机号码");









