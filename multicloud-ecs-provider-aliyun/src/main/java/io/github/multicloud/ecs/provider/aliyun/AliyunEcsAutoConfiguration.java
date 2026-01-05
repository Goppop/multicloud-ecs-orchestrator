package io.github.multicloud.ecs.provider.aliyun;

import io.github.multicloud.ecs.core.registry.CloudEcsClientRegistry;
import io.github.multicloud.ecs.core.util.TenantTagInjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 阿里云ECS自动配置
 * 
 * 当配置 multicloud.ecs.aliyun.enabled=true 时自动启用
 *
 * @author guo
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AliyunEcsProperties.class)
@ConditionalOnProperty(prefix = "multicloud.ecs.aliyun", name = "enabled", havingValue = "true")
public class AliyunEcsAutoConfiguration {

    @Resource
    private CloudEcsClientRegistry registry;

    @Resource
    private AliyunEcsProperties properties;

    @Resource
    private TenantTagInjector tenantTagInjector;

    /**
     * 创建阿里云网络资源管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunNetworkManager aliyunNetworkManager() {
        return new AliyunNetworkManager(properties);
    }

    /**
     * 创建阿里云参数映射器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunParameterMapper aliyunParameterMapper() {
        return new AliyunParameterMapper(properties);
    }

    /**
     * 创建阿里云ECS客户端Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunEcsClient aliyunEcsClient(AliyunNetworkManager networkManager, 
                                           AliyunParameterMapper parameterMapper) {
        return new AliyunEcsClient(properties, networkManager, parameterMapper, tenantTagInjector);
    }

    /**
     * 启动时自动注册到Registry
     */
    @PostConstruct
    public void registerClient() {
        if (properties.isEnabled()) {
            AliyunNetworkManager networkManager = new AliyunNetworkManager(properties);
            AliyunParameterMapper parameterMapper = new AliyunParameterMapper(properties);
            AliyunEcsClient client = new AliyunEcsClient(properties, networkManager, parameterMapper, tenantTagInjector);
            registry.register(client);
            log.info("[AliyunEcsAutoConfiguration] 阿里云ECS客户端已注册: providerCode={}, region={}",
                    properties.getProviderCode(), properties.getRegionId());
        }
    }
}

