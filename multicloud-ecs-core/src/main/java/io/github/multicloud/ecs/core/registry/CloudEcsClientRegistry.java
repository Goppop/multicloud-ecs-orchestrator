package io.github.multicloud.ecs.core.registry;

import io.github.multicloud.ecs.api.CloudEcsClient;
import io.github.multicloud.ecs.api.exception.EcsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云厂商ECS客户端注册中心
 * 管理所有已注册的云厂商客户端
 *
 * @author guo
 */
@Slf4j
@Component
public class CloudEcsClientRegistry {

    /**
     * 客户端注册表：providerCode -> CloudEcsClient
     */
    private final Map<String, CloudEcsClient> clients = new ConcurrentHashMap<>();

    /**
     * 注册云厂商客户端
     *
     * @param client 云厂商客户端
     */
    public void register(CloudEcsClient client) {
        if (client == null) {
            throw new IllegalArgumentException("CloudEcsClient cannot be null");
        }
        String providerCode = normalizeProviderCode(client.getProviderCode());
        CloudEcsClient existing = clients.put(providerCode, client);
        if (existing != null) {
            log.warn("云厂商客户端已被覆盖: providerCode={}, old={}, new={}",
                    providerCode, existing.getClass().getSimpleName(), client.getClass().getSimpleName());
        } else {
            log.info("注册云厂商客户端: providerCode={}, name={}, class={}",
                    providerCode, client.getProviderName(), client.getClass().getSimpleName());
        }
    }

    /**
     * 注册云厂商客户端（指定代码）
     *
     * @param providerCode 云厂商代码
     * @param client 云厂商客户端
     */
    public void register(String providerCode, CloudEcsClient client) {
        if (client == null) {
            throw new IllegalArgumentException("CloudEcsClient cannot be null");
        }
        providerCode = normalizeProviderCode(providerCode);
        CloudEcsClient existing = clients.put(providerCode, client);
        if (existing != null) {
            log.warn("云厂商客户端已被覆盖: providerCode={}, old={}, new={}",
                    providerCode, existing.getClass().getSimpleName(), client.getClass().getSimpleName());
        } else {
            log.info("注册云厂商客户端: providerCode={}, name={}, class={}",
                    providerCode, client.getProviderName(), client.getClass().getSimpleName());
        }
    }

    /**
     * 注销云厂商客户端
     *
     * @param providerCode 云厂商代码
     * @return 被注销的客户端，不存在返回null
     */
    public CloudEcsClient unregister(String providerCode) {
        providerCode = normalizeProviderCode(providerCode);
        CloudEcsClient removed = clients.remove(providerCode);
        if (removed != null) {
            log.info("注销云厂商客户端: providerCode={}", providerCode);
        }
        return removed;
    }

    /**
     * 获取云厂商客户端
     *
     * @param providerCode 云厂商代码
     * @return 云厂商客户端
     * @throws EcsException 客户端未注册时抛出
     */
    public CloudEcsClient getClient(String providerCode) throws EcsException {
        providerCode = normalizeProviderCode(providerCode);
        CloudEcsClient client = clients.get(providerCode);
        if (client == null) {
            throw EcsException.of("REGISTRY", "PROVIDER_NOT_FOUND",
                    "云厂商客户端未注册: " + providerCode + ", 已注册的厂商: " + getRegisteredProviderCodes());
        }
        return client;
    }

    /**
     * 获取云厂商客户端（可选）
     *
     * @param providerCode 云厂商代码
     * @return 云厂商客户端，不存在返回Optional.empty()
     */
    public Optional<CloudEcsClient> getClientOptional(String providerCode) {
        providerCode = normalizeProviderCode(providerCode);
        return Optional.ofNullable(clients.get(providerCode));
    }

    /**
     * 检查云厂商是否已注册
     *
     * @param providerCode 云厂商代码
     * @return 是否已注册
     */
    public boolean isRegistered(String providerCode) {
        providerCode = normalizeProviderCode(providerCode);
        return clients.containsKey(providerCode);
    }

    /**
     * 获取所有已注册的云厂商代码
     *
     * @return 云厂商代码列表
     */
    public List<String> getRegisteredProviderCodes() {
        return new ArrayList<>(clients.keySet());
    }

    /**
     * 获取所有已注册的云厂商客户端
     *
     * @return 云厂商客户端列表
     */
    public List<CloudEcsClient> getAllClients() {
        return new ArrayList<>(clients.values());
    }

    /**
     * 获取所有可用的云厂商客户端
     *
     * @return 可用的云厂商客户端列表
     */
    public List<CloudEcsClient> getAvailableClients() {
        List<CloudEcsClient> available = new ArrayList<>();
        for (CloudEcsClient client : clients.values()) {
            if (client.isAvailable()) {
                available.add(client);
            }
        }
        // 按优先级排序
        available.sort(Comparator.comparingInt(CloudEcsClient::getPriority));
        return available;
    }

    /**
     * 获取已注册客户端数量
     *
     * @return 客户端数量
     */
    public int size() {
        return clients.size();
    }

    /**
     * 清空所有注册的客户端
     */
    public void clear() {
        clients.clear();
        log.info("已清空所有云厂商客户端注册");
    }

    /**
     * 标准化云厂商代码（转大写）
     */
    private String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.trim().isEmpty()) {
            throw new IllegalArgumentException("providerCode cannot be null or empty");
        }
        return providerCode.trim().toUpperCase();
    }
}

