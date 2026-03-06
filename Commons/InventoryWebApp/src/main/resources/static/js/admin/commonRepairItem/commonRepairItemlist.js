/**
 * 通用维修项(CommonRepairItem)前端操作JS
 * 功能：批量导入弹窗、搜索分页跳转、导入进度条轮询展示
 */

// 全局变量：存储进度条定时器ID，防止重复创建多个定时器
let repairItemProgressIntervalId = null;

/**
 * 打开通用维修项批量导入弹窗
 */
function batchImput() {
    $("#mybatchUpload").modal('show');
}

/**
 * 搜索/分页大小变更 - 通用维修项列表查询
 */
function searchVal() {
    var pageSize = $("#pageSize").val();
    var search = $("#serach").val(); // 保留原拼写serach，与你原代码一致（如需修正可改为search）
    window.location.href = "commonRepairItems?pageSize=" + pageSize + "&search=" + search;
}

/**
 * 分页大小变更 - 通用维修项列表查询（与searchVal逻辑一致，保留原方法名）
 */
function searchSize() {
    var pageSize = $("#pageSize").val();
    var search = $("#serach").val();
    window.location.href = "commonRepairItems?pageSize=" + pageSize + "&search=" + search;
}

/**
 * 页面初始化
 */
$(document).ready(function() {
    // 绑定导入提交按钮点击事件，轮询获取导入进度
    $('#submit').bind('click', function() {
        // 定义轮询获取进度的函数
        var eventFun = function() {
            $.ajax({
                type : 'GET',
                // 适配CommonRepairItem的进度查询接口
                url : 'commonRepairItem/uploadprocess',
                data : {},
                dataType : 'json',
                success : function(data) {
                    // 计算进度条宽度百分比
                    var progressPercent = (data.nownum / data.allnum) * 100;
                    // 更新进度条样式和属性
                    $("#proBar").attr("style", "width:" + progressPercent + '%');
                    $('#proBar').css('aria-valuenow', data.nownum + '%');
                    $('#proBar').css('aria-valuemax', data.allnum + '%');
                    // 更新进度文本提示
                    $('#proBartext').text("正在导入第" + data.nownum + "条记录，总共" + data.allnum + "条记录");
                    // 导入完成后清除定时器
                    if (data.nownum == data.allnum) {
                        window.clearInterval(repairItemProgressIntervalId);
                    }
                },
                // 增加异常处理，防止请求失败导致定时器一直运行
                error: function(xhr, status, error) {
                    console.error("获取通用维修项导入进度失败：", error);
                    window.clearInterval(repairItemProgressIntervalId);
                    $('#proBartext').text("导入进度查询失败，请刷新重试");
                }
            });
        };
        // 启动轮询（间隔100ms，与原代码保持一致）
        repairItemProgressIntervalId = window.setInterval(eventFun, 100);
    });
});