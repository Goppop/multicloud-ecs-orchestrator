package io.github.multicloud.ecs.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 云厂商枚举
 *
 * @author guo
 */
@Getter
@AllArgsConstructor
public enum ProviderEnum {

    /**
     * 阿里云
     */
    ALIYUN("ALIYUN", "阿里云"),

    /**
     * 腾讯云
     */
    TENCENT("TENCENT", "腾讯云"),

    /**
     * 华为云
     */
    HUAWEI("HUAWEI", "华为云"),

    /**
     * AWS
     */
    AWS("AWS", "亚马逊云"),

    /**
     * 移动云（苏州）
     */
    SCC("SCC", "苏州移动云"),

    /**
     * 移动云（通用）
     */
    MOBILECLOUD("MOBILECLOUD", "移动云");

    /**
     * 云厂商代码
     */
    private final String code;

    /**
     * 云厂商名称
     */
    private final String name;

    /**
     * 根据代码获取枚举
     */
    public static ProviderEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProviderEnum provider : values()) {
            if (provider.getCode().equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 判断代码是否有效
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}

