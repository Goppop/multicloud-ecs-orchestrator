package io.github.multicloud.ecs.provider.aliyun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云ECS配置属性
 *
 * @author guo
 */
@Data
@ConfigurationProperties(prefix = "multicloud.ecs.aliyun")
public class AliyunEcsProperties {

    /**
     * 是否启用
     */
    private boolean enabled = false;

    /**
     * 云厂商代码
     */
    private String providerCode = "ALIYUN";

    /**
     * 云厂商名称
     */
    private String providerName = "阿里云";

    /**
     * Region ID（如 cn-hangzhou, cn-shanghai）
     */
    private String regionId = "cn-hangzhou";

    /**
     * Access Key ID
     */
    private String accessKeyId;

    /**
     * Access Key Secret
     */
    private String accessKeySecret;

    /**
     * 默认VPC ID
     */
    private String vpcId;

    /**
     * 默认交换机ID
     */
    private String vSwitchId;

    /**
     * 默认安全组ID
     */
    private String securityGroupId;

    /**
     * 默认镜像ID（如 centos_7_9_x64_20G_alibase_20220824.vhd）
     */
    private String defaultImageId;

    /**
     * 默认实例类型（如 ecs.g7.large）
     */
    private String defaultInstanceType;

    /**
     * 默认系统盘类型（cloud_efficiency, cloud_ssd, cloud_essd）
     */
    private String defaultSystemDiskCategory = "cloud_essd";

    /**
     * 默认系统盘大小(GB)
     */
    private Integer defaultSystemDiskSize = 40;

    /**
     * 计费类型（PostPaid-按量付费, PrePaid-包年包月）
     */
    private String instanceChargeType = "PostPaid";

    /**
     * 网络计费类型（PayByTraffic-按流量, PayByBandwidth-按带宽）
     */
    private String internetChargeType = "PayByTraffic";

    /**
     * 公网带宽最大值(Mbps)，0表示不分配公网IP
     */
    private Integer internetMaxBandwidthOut = 0;

    /**
     * 客户端优先级（值越小优先级越高）
     */
    private int priority = 100;
}

