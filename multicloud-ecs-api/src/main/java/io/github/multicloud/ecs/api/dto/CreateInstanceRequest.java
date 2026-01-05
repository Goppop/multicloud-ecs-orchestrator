package io.github.multicloud.ecs.api.dto;

import io.github.multicloud.ecs.api.enums.BandwidthMode;
import io.github.multicloud.ecs.api.enums.InstanceChargeMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

/**
 * 创建实例请求DTO
 * 统一各云厂商的创建请求参数
 *
 * @author guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstanceRequest {

    /**
     * 云厂商代码（V1必填，V2可空用于智能调度）
     * 如：ALIYUN, SCC, TENCENT
     */
    private String provider;

    /**
     * 区域/Region
     * 如：cn-hangzhou, cn-shanghai
     */
    @NotBlank(message = "区域不能为空")
    private String region;

    /**
     * 可用区/Zone（可选）
     * 如：cn-hangzhou-g
     */
    private String zone;

    /**
     * 租户ID（必填，用于资源隔离和成本分摊）
     */
    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    /**
     * 用户ID（必填，用于租户隔离的唯一标识）
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 实例名称
     */
    @NotBlank(message = "实例名称不能为空")
    private String instanceName;

    /**
     * 规格类型（可选，如果指定则按规格创建）
     * 如：ecs.g7.large, sl.medium.2
     */
    private String instanceType;

    /**
     * 业务镜像标识（必填，需在驱动层映射为云厂商 ImageId）
     * 如：centos-7.9, ubuntu-20.04, pytorch-1.12
     */
    @NotBlank(message = "镜像标识不能为空")
    private String imageKey;

    /**
     * GPU型号需求（可选，用于AI算力平台）
     * 如：A100, V100, T4
     */
    private String gpuModel;

    /**
     * CPU核数（当不指定instanceType时使用）
     */
    @Positive(message = "CPU核数必须大于0")
    private Integer cpu;

    /**
     * 内存大小(GB)（当不指定instanceType时使用）
     */
    @Positive(message = "内存大小必须大于0")
    private Integer memory;

    /**
     * 系统盘大小(GB)
     */
    @NotNull(message = "系统盘大小不能为空")
    @Positive(message = "系统盘大小必须大于0")
    private Integer systemDiskSize;

    /**
     * 系统盘类型（可选）
     * 如：cloud_ssd, cloud_efficiency
     */
    private String systemDiskType;

    /**
     * 是否分配公网IP
     */
    @Builder.Default
    private Boolean allocatePublicIp = false;

    /**
     * 需要开放的端口列表（用于AI算力平台）
     * 驱动层会自动为实例所属安全组添加入站规则
     */
    private List<Integer> openPorts;

    /**
     * 公网带宽(Mbps)，allocatePublicIp为true时有效
     */
    private Integer publicIpBandwidth;

    /**
     * 带宽计费模式（allocatePublicIp为true时有效）
     * TRAFFIC: 按流量计费
     * FIXED: 固定带宽计费
     */
    @Builder.Default
    private BandwidthMode bandwidthMode = BandwidthMode.FIXED;

    /**
     * 密码（明文，由Provider负责加密）
     */
    private String password;

    /**
     * 密钥对名称（可选，与密码二选一）
     */
    private String keyPairName;

    /**
     * 实例计费模式
     * ON_DEMAND: 按需付费（按小时/秒计费）
     * PREPAID: 预付费（包年包月）
     */
    @Builder.Default
    private InstanceChargeMode instanceChargeMode = InstanceChargeMode.ON_DEMAND;

    /**
     * 时长（预付费模式时使用，单位：月）
     * 如：1（1个月）、3（3个月）、12（1年）
     */
    private Integer duration;

    /**
     * 数量（默认1）
     */
    @Builder.Default
    private Integer quantity = 1;

    /**
     * 描述/备注
     */
    private String description;

    /**
     * 标签（框架会自动注入tenantId）
     */
    private Map<String, String> tags;

    /**
     * 扩展参数（用于厂商特有参数）
     * 如移动云的 vmType, 阿里云的 spotStrategy 等
     */
    private Map<String, Object> extensions;

    /**
     * 获取扩展参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(String key, Class<T> type) {
        if (extensions == null || !extensions.containsKey(key)) {
            return null;
        }
        Object value = extensions.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取扩展参数（带默认值）
     */
    public <T> T getExtension(String key, Class<T> type, T defaultValue) {
        T value = getExtension(key, type);
        return value != null ? value : defaultValue;
    }
}

