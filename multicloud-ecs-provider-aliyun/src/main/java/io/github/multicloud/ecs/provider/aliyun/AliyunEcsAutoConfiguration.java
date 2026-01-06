package io.github.multicloud.ecs.provider.aliyun;

import io.github.multicloud.ecs.core.util.TenantTagInjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    private AliyunEcsProperties properties;

    @Resource
    private TenantTagInjector tenantTagInjector;

    /**
     * 创建阿里云网络资源管理器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunNetworkManager aliyunNetworkManager() {
        log.info("[AliyunEcsAutoConfiguration] 创建阿里云网络资源管理器Bean");
        return new AliyunNetworkManager(properties);
    }

    /**
     * 创建阿里云参数映射器Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunParameterMapper aliyunParameterMapper() {
        log.info("[AliyunEcsAutoConfiguration] 创建阿里云参数映射器Bean");
        return new AliyunParameterMapper(properties);
    }

    /**
     * 创建阿里云ECS客户端Bean
     * 
     * 注意：客户端会自动注册到 CloudEcsClientRegistry
     * 由 MultiCloudEcsAutoConfiguration.autoRegisterClients() 方法自动处理
     */
    @Bean
    @ConditionalOnMissingBean
    public AliyunEcsClient aliyunEcsClient(AliyunNetworkManager networkManager, 
                                           AliyunParameterMapper parameterMapper) {
        log.info("[AliyunEcsAutoConfiguration] 创建阿里云ECS客户端Bean: providerCode={}, providerName={}, region={}",
                properties.getProviderCode(), properties.getProviderName(), properties.getRegionId());
        AliyunEcsClient client = new AliyunEcsClient(properties, networkManager, parameterMapper, tenantTagInjector);
        log.info("[AliyunEcsAutoConfiguration] 阿里云ECS客户端Bean创建完成，等待自动注册到Registry");
        return client;
    }
}

