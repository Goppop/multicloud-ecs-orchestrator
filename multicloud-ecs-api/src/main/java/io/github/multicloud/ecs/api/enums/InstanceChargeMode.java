package io.github.multicloud.ecs.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实例计费模式枚举
 * 统一各云厂商的实例计费方式
 *
 * @author guo
 */
@Getter
@AllArgsConstructor
public enum InstanceChargeMode {

    /**
     * 按需付费（按小时/秒计费，随用随付）
     */
    ON_DEMAND("ON_DEMAND", "按需付费"),

    /**
     * 预付费（包年包月，提前付费）
     */
    PREPAID("PREPAID", "预付费");

    /**
     * 计费模式代码
     */
    private final String code;

    /**
     * 计费模式描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     */
    public static InstanceChargeMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (InstanceChargeMode mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return null;
    }
}

