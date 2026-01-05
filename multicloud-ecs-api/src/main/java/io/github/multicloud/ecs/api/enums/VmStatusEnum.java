package io.github.multicloud.ecs.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 虚拟机统一状态枚举
 * 屏蔽各云厂商的状态差异，提供标准化的状态值
 *
 * @author guo
 */
@Getter
@AllArgsConstructor
public enum VmStatusEnum {

    /**
     * 创建中/启动中
     */
    PENDING("PENDING", "创建中"),

    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 已停止
     */
    STOPPED("STOPPED", "已停止"),

    /**
     * 启动中
     */
    STARTING("STARTING", "启动中"),

    /**
     * 停止中
     */
    STOPPING("STOPPING", "停止中"),

    /**
     * 重启中
     */
    REBOOTING("REBOOTING", "重启中"),

    /**
     * 删除中
     */
    DELETING("DELETING", "删除中"),

    /**
     * 已删除
     */
    DELETED("DELETED", "已删除"),

    /**
     * 错误状态
     */
    ERROR("ERROR", "错误"),

    /**
     * 未知状态
     */
    UNKNOWN("UNKNOWN", "未知");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据状态码获取枚举
     */
    public static VmStatusEnum fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (VmStatusEnum status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为终态（不会再变化的状态）
     */
    public boolean isFinalState() {
        return this == RUNNING || this == STOPPED || this == DELETED || this == ERROR;
    }

    /**
     * 判断是否为可操作状态
     */
    public boolean isOperableState() {
        return this == RUNNING || this == STOPPED;
    }
}

