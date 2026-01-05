package io.github.multicloud.ecs.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多云ECS框架全局配置
 *
 * @author guo
 */
@Data
@ConfigurationProperties(prefix = "multicloud.ecs")
public class MultiCloudEcsProperties {

    /**
     * 是否启用多云ECS框架
     */
    private boolean enabled = true;

    /**
     * 调度策略：fixed(固定路由), cost(成本优化), availability(可用性优先)
     */
    private String schedulerType = "fixed";

    /**
     * 默认云厂商代码（fixed调度策略时使用）
     */
    private String defaultProvider;

    /**
     * 操作超时时间（秒）
     */
    private int operationTimeout = 300;

    /**
     * 创建实例轮询间隔（毫秒）
     */
    private long pollingInterval = 5000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 是否启用操作审计日志
     */
    private boolean auditEnabled = true;

    /**
     * 是否启用异步操作
     */
    private boolean asyncEnabled = true;

    /**
     * 异步线程池核心大小
     */
    private int asyncCorePoolSize = 5;

    /**
     * 异步线程池最大大小
     */
    private int asyncMaxPoolSize = 20;
}

