package io.github.multicloud.ecs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 价格信息DTO
 * 统一各云厂商的价格查询返回结果
 *
 * @author guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceInfo {

    /**
     * 云厂商代码
     */
    private String provider;

    /**
     * 区域
     */
    private String region;

    /**
     * 实例规格
     */
    private String instanceType;

    /**
     * 实例单价（按小时，单位：元）
     */
    private BigDecimal instancePricePerHour;

    /**
     * 实例单价（按月，单位：元，仅预付费模式有效）
     */
    private BigDecimal instancePricePerMonth;

    /**
     * 系统盘单价（按GB/月，单位：元）
     */
    private BigDecimal systemDiskPricePerGbPerMonth;

    /**
     * 公网带宽单价（按Mbps/月，单位：元，仅固定带宽模式有效）
     */
    private BigDecimal bandwidthPricePerMbpsPerMonth;

    /**
     * 公网流量单价（按GB，单位：元，仅按流量计费模式有效）
     */
    private BigDecimal trafficPricePerGb;

    /**
     * 总价（按小时，单位：元）
     * 包含实例、系统盘、带宽等所有费用
     */
    private BigDecimal totalPricePerHour;

    /**
     * 总价（按月，单位：元）
     * 包含实例、系统盘、带宽等所有费用
     */
    private BigDecimal totalPricePerMonth;

    /**
     * 货币单位（默认：CNY）
     */
    @Builder.Default
    private String currency = "CNY";

    /**
     * 价格查询时间戳
     */
    private Long queryTimestamp;

    /**
     * 价格有效期（秒）
     */
    private Long priceValiditySeconds;

    /**
     * 扩展信息（厂商特有信息）
     */
    private java.util.Map<String, Object> metadata;
}

