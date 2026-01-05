package io.github.multicloud.ecs.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 带宽计费模式枚举
 * 统一各云厂商的带宽计费方式
 *
 * @author guo
 */
@Getter
@AllArgsConstructor
public enum BandwidthMode {

    /**
     * 按流量计费（按实际使用流量付费）
     */
    TRAFFIC("TRAFFIC", "按流量计费"),

    /**
     * 固定带宽计费（按固定带宽大小付费）
     */
    FIXED("FIXED", "固定带宽计费");

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
    public static BandwidthMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BandwidthMode mode : values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return null;
    }
}

