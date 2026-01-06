package io.github.multicloud.ecs.starter;

import io.github.multicloud.ecs.api.CloudEcsClient;
import io.github.multicloud.ecs.api.EcsScheduler;
import io.github.multicloud.ecs.core.registry.CloudEcsClientRegistry;
import io.github.multicloud.ecs.core.scheduler.FixedScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 多云ECS框架自动配置
 * 
 * 使用方式：
 * 1. 引入 multicloud-ecs-starter 依赖
 * 2. 在 application.yml 中配置 multicloud.ecs.enabled=true
 * 3. 注入 MultiCloudEcsService 即可使用
 *
 * @author guo
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MultiCloudEcsProperties.class)
@ConditionalOnProperty(prefix = "multicloud.ecs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
        "io.github.multicloud.ecs.core",
        "io.github.multicloud.ecs.provider"
})
public class MultiCloudEcsAutoConfiguration {

    private final MultiCloudEcsProperties properties;
    private final ObjectProvider<List<CloudEcsClient>> clientsProvider;
    private final CloudEcsClientRegistry registry;

    public MultiCloudEcsAutoConfiguration(
            MultiCloudEcsProperties properties,
            ObjectProvider<List<CloudEcsClient>> clientsProvider,
            CloudEcsClientRegistry registry) {
        this.properties = properties;
        this.clientsProvider = clientsProvider;
        this.registry = registry;
    }

    /**
     * 创建客户端注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public CloudEcsClientRegistry cloudEcsClientRegistry() {
        return new CloudEcsClientRegistry();
    }

    /**
     * 创建调度器（默认使用固定调度）
     */
    @Bean
    @ConditionalOnMissingBean
    public EcsScheduler ecsScheduler(CloudEcsClientRegistry registry) {
        String schedulerType = properties.getSchedulerType();
        log.info("[MultiCloudEcs] 使用调度器: {}", schedulerType);
        
        // V1 只支持 fixed 调度器
        // V2 可扩展支持 cost, availability 等调度策略
        return new FixedScheduler(registry);
    }

    // MultiCloudEcsService 由 @ComponentScan 自动发现 MultiCloudEcsServiceImpl (@Service)

    /**
     * 创建异步任务执行器
     */
    @Bean(name = "ecsAsyncExecutor")
    @ConditionalOnMissingBean(name = "ecsAsyncExecutor")
    @ConditionalOnProperty(prefix = "multicloud.ecs", name = "async-enabled", havingValue = "true", matchIfMissing = true)
    public Executor ecsAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsyncCorePoolSize());
        executor.setMaxPoolSize(properties.getAsyncMaxPoolSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ecs-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("[MultiCloudEcs] 异步执行器已创建: coreSize={}, maxSize={}",
                properties.getAsyncCorePoolSize(), properties.getAsyncMaxPoolSize());
        return executor;
    }

    /**
     * 自动注册所有 CloudEcsClient Bean
     */
    @PostConstruct
    public void autoRegisterClients() {
        log.info("[MultiCloudEcs] ========== 开始自动注册云厂商客户端 ==========");
        List<CloudEcsClient> clients = clientsProvider.getIfAvailable();
        if (clients != null && !clients.isEmpty()) {
            log.info("[MultiCloudEcs] 发现 {} 个 CloudEcsClient Bean", clients.size());
            for (CloudEcsClient client : clients) {
                String providerCode = client.getProviderCode();
                log.info("[MultiCloudEcs] 处理客户端: providerCode={}, providerName={}, class={}, available={}",
                        providerCode, client.getProviderName(), client.getClass().getSimpleName(), 
                        client.isAvailable());
                if (!registry.isRegistered(providerCode)) {
                    registry.register(client);
                    log.info("[MultiCloudEcs] ✓ 客户端已注册: providerCode={}", providerCode);
                } else {
                    log.warn("[MultiCloudEcs] ✗ 客户端已存在，跳过注册: providerCode={}", providerCode);
                }
            }
            log.info("[MultiCloudEcs] ========== 自动注册完成 ==========");
            log.info("[MultiCloudEcs] 已注册 {} 个云厂商客户端: {}",
                    registry.size(), registry.getRegisteredProviderCodes());
        } else {
            log.warn("[MultiCloudEcs] ✗ 未发现任何云厂商客户端，请检查配置");
            log.warn("[MultiCloudEcs] 请确认：1) 是否引入了provider模块 2) 是否配置了 enabled=true");
        }
    }
}

