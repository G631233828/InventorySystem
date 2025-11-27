package zhongchiedu.common.utils.enums;

/**
 * 维修工单状态枚举
 */
public enum RepairStatus {

    /**
     * 待处理
     */
    PENDING(1, "待处理", "bg-yellow-100 text-yellow-800"),

    /**
     * 已分配
     */
    ASSIGNED(2, "已分配", "bg-blue-100 text-blue-800"),

    /**
     * 处理中
     */
    PROCESSING(3, "处理中", "bg-blue-500 text-white"),

    /**
     * 已完成
     */
    COMPLETED(4, "已完成", "bg-green-100 text-green-800"),

    /**
     * 已取消
     */
    CANCELLED(5, "已取消", "bg-gray-100 text-gray-800");

    private final int code;
    private final String description;
    private final String cssClass;

    RepairStatus(int code, String description, String cssClass) {
        this.code = code;
        this.description = description;
        this.cssClass = cssClass;
    }

    /**
     * 根据状态编码获取枚举实例
     *
     * @param code 状态编码
     * @return 对应的 RepairStatus 枚举，如果找不到则返回 null
     */
    public static RepairStatus fromCode(int code) {
        for (RepairStatus status : RepairStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null; // 或者可以抛出一个异常，视业务需求而定
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getCssClass() {
        return cssClass;
    }
}