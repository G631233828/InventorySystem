function searchVal() {
    var pageSize = $("#pageSize").val();
    var search = $("#search").val();
    window.location.href = "signTasks?pageSize=" + pageSize + "&search=" + search;
}

function searchSize() {
    searchVal();
}