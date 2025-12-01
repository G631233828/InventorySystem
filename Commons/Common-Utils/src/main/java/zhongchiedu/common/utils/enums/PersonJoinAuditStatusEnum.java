package zhongchiedu.common.utils.enums;

/**
 * 人员加入审核状态枚举（提交/审核通过）
 */
public enum PersonJoinAuditStatusEnum {
    /** 申请已提交 */
    SUBMITTED(1, "申请已提交", "用户提交加入申请，等待审核"),
    /** 审核通过 */
    APPROVED(2, "审核通过", "申请审核通过，人员加入成功"),
	
	REFUSE(3, "审核拒绝", "申请审核被拒绝，请联系管理人员");

    private final Integer code;
    private final String name;
    private final String desc;

    PersonJoinAuditStatusEnum(Integer code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    // Getter方法
    public Integer getCode() { return code; }
    public String getName() { return name; }
    public String getDesc() { return desc; }

    // 根据code获取枚举
    public static PersonJoinAuditStatusEnum getByCode(Integer code) {
        for (PersonJoinAuditStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}