function searchVal() {

	var searchArea = $("#searchArea").val();

/*
 * if (search == null || search == "") { swal({ type : "warning", title : "",
 * text : "查询内容不能为空!!", }); return ; }
 */
	window.location.href="goodsStorages?searchArea="+searchArea;

}