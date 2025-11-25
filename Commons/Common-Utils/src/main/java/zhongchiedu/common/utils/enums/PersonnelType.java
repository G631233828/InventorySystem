package zhongchiedu.common.utils.enums;
import java.util.Arrays;
import java.util.Optional;

/**
 * 人员类型枚举
 * 1: 施工队
 * 2: 调度人员
 */
public enum PersonnelType {

    /**
     *维修人员（值为1）
     */
    CONSTRUCTION_TEAM(1, "维修人员"),

    /**
     * 调度人员（值为2）
     */
    DISPATCHER(2, "调度人员");

    /**
     * 人员类型编码（数据库存储用）
     */
    private final Integer code;

    /**
     * 人员类型名称（页面展示用）
     */
    private final String name;

    /**
     * 私有构造函数（枚举的构造函数必须是private）
     * @param code 编码
     * @param name 名称
     */
    PersonnelType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    // ------------------- 常用方法 -------------------

    /**
     * 获取编码（用于数据库存储、接口传输）
     * @return 人员类型编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取名称（用于页面展示、日志打印）
     * @return 人员类型名称
     */
    public String getName() {
        return name;
    }

    /**
     * 根据编码反向查找枚举（常用！）
     * @param code 人员类型编码
     * @return 对应的枚举对象（Optional包装，避免空指针）
     */
    public static Optional<PersonnelType> getByCode(Integer code) {
        // 遍历所有枚举常量，匹配编码
        return Arrays.stream(PersonnelType.values())
                .filter(type -> type.getCode().equals(code))
                .findFirst();
    }

    /**
     * 重写toString方法（默认返回名称，方便打印）
     * @return 人员类型名称
     */
    @Override
    public String toString() {
        return this.name;
    }
}