package io.github.multicloud.ecs.api.dto;

import io.github.multicloud.ecs.api.enums.VmStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 虚拟机响应DTO
 * 统一各云厂商的虚拟机信息
 *
 * @author guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualMachine {

    /**
     * 云厂商实例ID
     */
    private String instanceId;

    /**
     * 实例名称
     */
    private String instanceName;

    /**
     * 统一状态
     */
    private VmStatusEnum status;

    /**
     * 云厂商原始状态值（用于调试）
     */
    private String rawStatus;

    /**
     * 云厂商代码
     */
    private String provider;

    /**
     * 区域
     */
    private String region;

    /**
     * 可用区
     */
    private String zone;

    /**
     * 规格类型
     */
    private String instanceType;

    /**
     * 镜像ID
     */
    private String imageId;

    /**
     * CPU核数
     */
    private Integer cpu;

    /**
     * 内存大小(GB)
     */
    private Integer memory;

    /**
     * 内网IP
     */
    private String privateIp;

    /**
     * 公网IP
     */
    private String publicIp;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 到期时间（包年包月实例）
     */
    private LocalDateTime expiredAt;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 标签
     */
    private Map<String, String> tags;

    /**
     * 元数据（存放云厂商特有信息）
     * 如：vpcId, subnetId, securityGroupIds 等
     */
    private Map<String, Object> metadata;

    /**
     * 错误信息（创建失败时）
     */
    private String errorMessage;

    /**
     * 请求ID（用于追踪）
     */
    private String requestId;

    /**
     * 任务ID（异步操作时）
     */
    private String taskId;

    /**
     * 判断是否创建成功
     */
    public boolean isSuccess() {
        return status != VmStatusEnum.ERROR && errorMessage == null;
    }

    /**
     * 判断是否处于终态
     */
    public boolean isFinalState() {
        return status != null && status.isFinalState();
    }

    /**
     * 判断是否正在运行
     */
    public boolean isRunning() {
        return status == VmStatusEnum.RUNNING;
    }

    /**
     * 获取元数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 创建一个错误响应
     */
    public static VirtualMachine error(String provider, String errorMessage) {
        return VirtualMachine.builder()
                .provider(provider)
                .status(VmStatusEnum.ERROR)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建一个错误响应（带请求ID）
     */
    public static VirtualMachine error(String provider, String errorMessage, String requestId) {
        return VirtualMachine.builder()
                .provider(provider)
                .status(VmStatusEnum.ERROR)
                .errorMessage(errorMessage)
                .requestId(requestId)
                .build();
    }
}

