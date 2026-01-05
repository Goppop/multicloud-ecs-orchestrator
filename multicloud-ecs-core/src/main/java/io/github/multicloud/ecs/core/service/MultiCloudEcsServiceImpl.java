package io.github.multicloud.ecs.core.service;

import io.github.multicloud.ecs.api.CloudEcsClient;
import io.github.multicloud.ecs.api.EcsScheduler;
import io.github.multicloud.ecs.api.MultiCloudEcsService;
import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.exception.EcsException;
import io.github.multicloud.ecs.core.registry.CloudEcsClientRegistry;
import io.github.multicloud.ecs.core.util.TenantTagInjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 多云ECS统一服务实现类
 *
 * @author guo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiCloudEcsServiceImpl implements MultiCloudEcsService {

    private final CloudEcsClientRegistry registry;
    private final EcsScheduler scheduler;
    private final TenantTagInjector tenantTagInjector;

    @Override
    public VirtualMachine createInstance(CreateInstanceRequest request) throws EcsException {
        // 1. 参数校验
        validateCreateRequest(request);

        // 2. 注入租户标签
        tenantTagInjector.inject(request);

        // 3. 调度选择云厂商
        CloudEcsClient client = scheduler.select(request);
        log.info("创建实例开始: provider={}, instanceName={}, tenantId={}, region={}",
                client.getProviderCode(), request.getInstanceName(), 
                request.getTenantId(), request.getRegion());

        try {
            // 4. 调用云厂商API创建实例
            VirtualMachine vm = client.createInstance(request);

            // 5. 补充响应信息
            if (vm.getProvider() == null) {
                vm.setProvider(client.getProviderCode());
            }
            if (vm.getTenantId() == null) {
                vm.setTenantId(request.getTenantId());
            }

            log.info("创建实例成功: provider={}, instanceId={}, instanceName={}, status={}",
                    vm.getProvider(), vm.getInstanceId(), vm.getInstanceName(), vm.getStatus());
            return vm;

        } catch (EcsException e) {
            log.error("创建实例失败: provider={}, instanceName={}, error={}",
                    client.getProviderCode(), request.getInstanceName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建实例异常: provider={}, instanceName={}",
                    client.getProviderCode(), request.getInstanceName(), e);
            throw new EcsException(client.getProviderCode(), "CREATE_FAILED", 
                    "创建实例异常: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteInstance(String providerCode, String instanceId) throws EcsException {
        log.info("删除实例开始: provider={}, instanceId={}", providerCode, instanceId);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            boolean result = client.deleteInstance(instanceId);
            log.info("删除实例完成: provider={}, instanceId={}, result={}", providerCode, instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("删除实例失败: provider={}, instanceId={}, error={}", providerCode, instanceId, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean startInstance(String providerCode, String instanceId) throws EcsException {
        log.info("启动实例开始: provider={}, instanceId={}", providerCode, instanceId);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            boolean result = client.startInstance(instanceId);
            log.info("启动实例完成: provider={}, instanceId={}, result={}", providerCode, instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("启动实例失败: provider={}, instanceId={}, error={}", providerCode, instanceId, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean stopInstance(String providerCode, String instanceId) throws EcsException {
        log.info("停止实例开始: provider={}, instanceId={}", providerCode, instanceId);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            boolean result = client.stopInstance(instanceId);
            log.info("停止实例完成: provider={}, instanceId={}, result={}", providerCode, instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("停止实例失败: provider={}, instanceId={}, error={}", providerCode, instanceId, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean restartInstance(String providerCode, String instanceId) throws EcsException {
        log.info("重启实例开始: provider={}, instanceId={}", providerCode, instanceId);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            boolean result = client.restartInstance(instanceId);
            log.info("重启实例完成: provider={}, instanceId={}, result={}", providerCode, instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("重启实例失败: provider={}, instanceId={}, error={}", providerCode, instanceId, e.getMessage());
            throw e;
        }
    }

    @Override
    public VirtualMachine getInstance(String providerCode, String instanceId) throws EcsException {
        log.debug("查询实例详情: provider={}, instanceId={}", providerCode, instanceId);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            return client.getInstance(instanceId);
        } catch (EcsException e) {
            log.error("查询实例失败: provider={}, instanceId={}, error={}", providerCode, instanceId, e.getMessage());
            throw e;
        }
    }

    @Override
    public String findInstanceIdByName(String providerCode, String instanceName) throws EcsException {
        log.debug("根据名称查找实例ID: provider={}, instanceName={}", providerCode, instanceName);
        try {
            CloudEcsClient client = registry.getClient(providerCode);
            return client.findInstanceIdByName(instanceName);
        } catch (EcsException e) {
            log.error("查找实例ID失败: provider={}, instanceName={}, error={}", 
                    providerCode, instanceName, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<String> getRegisteredProviders() {
        return registry.getRegisteredProviderCodes();
    }

    @Override
    public boolean isProviderAvailable(String providerCode) {
        return registry.getClientOptional(providerCode)
                .map(CloudEcsClient::isAvailable)
                .orElse(false);
    }

    /**
     * 校验创建请求参数
     */
    private void validateCreateRequest(CreateInstanceRequest request) throws EcsException {
        if (request == null) {
            throw EcsException.of("VALIDATION", "REQUEST_NULL", "创建请求不能为空");
        }
        if (request.getTenantId() == null || request.getTenantId().trim().isEmpty()) {
            throw EcsException.of("VALIDATION", "TENANT_ID_REQUIRED", "租户ID不能为空");
        }
        if (request.getInstanceName() == null || request.getInstanceName().trim().isEmpty()) {
            throw EcsException.of("VALIDATION", "INSTANCE_NAME_REQUIRED", "实例名称不能为空");
        }
        if (request.getRegion() == null || request.getRegion().trim().isEmpty()) {
            throw EcsException.of("VALIDATION", "REGION_REQUIRED", "区域不能为空");
        }
        // 检查镜像标识
        if (request.getImageKey() == null || request.getImageKey().trim().isEmpty()) {
            throw EcsException.of("VALIDATION", "IMAGE_REQUIRED", "镜像标识(imageKey)不能为空");
        }
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw EcsException.of("VALIDATION", "USER_ID_REQUIRED", "用户ID不能为空");
        }
        // V1版本必须指定provider
        if (scheduler.requireProvider() && 
                (request.getProvider() == null || request.getProvider().trim().isEmpty())) {
            throw EcsException.of("VALIDATION", "PROVIDER_REQUIRED", 
                    "当前调度策略(" + scheduler.getName() + ")要求必须指定云厂商代码");
        }
    }
}

