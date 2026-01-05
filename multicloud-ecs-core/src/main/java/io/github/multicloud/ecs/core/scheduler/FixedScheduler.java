package io.github.multicloud.ecs.core.scheduler;

import io.github.multicloud.ecs.api.CloudEcsClient;
import io.github.multicloud.ecs.api.EcsScheduler;
import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.exception.EcsException;
import io.github.multicloud.ecs.core.registry.CloudEcsClientRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 固定调度器（V1版本）
 * 根据请求中指定的provider直接路由到对应的客户端
 *
 * @author guo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FixedScheduler implements EcsScheduler {

    private final CloudEcsClientRegistry registry;

    @Override
    public CloudEcsClient select(CreateInstanceRequest request) throws EcsException {
        // 1. 校验provider是否指定
        String provider = request.getProvider();
        if (provider == null || provider.trim().isEmpty()) {
            throw EcsException.of("SCHEDULER", "PROVIDER_REQUIRED",
                    "V1版本必须指定云厂商代码(provider字段)");
        }

        // 2. 获取对应的客户端
        CloudEcsClient client = registry.getClient(provider);

        // 3. 检查客户端是否可用
        if (!client.isAvailable()) {
            throw EcsException.of("SCHEDULER", "PROVIDER_UNAVAILABLE",
                    "云厂商客户端不可用: " + provider);
        }

        log.debug("FixedScheduler 选择云厂商: provider={}, clientClass={}",
                provider, client.getClass().getSimpleName());

        return client;
    }

    @Override
    public String getName() {
        return "FixedScheduler";
    }

    @Override
    public String getDescription() {
        return "固定调度器 - 根据指定的provider直接路由";
    }

    @Override
    public boolean requireProvider() {
        return true;
    }
}

