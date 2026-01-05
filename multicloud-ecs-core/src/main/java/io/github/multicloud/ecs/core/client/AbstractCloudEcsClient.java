package io.github.multicloud.ecs.core.client;

import io.github.multicloud.ecs.api.CloudEcsClient;
import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.PriceInfo;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.exception.EcsException;
import io.github.multicloud.ecs.core.util.TenantTagInjector;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 云厂商ECS客户端抽象基类
 * 提供通用功能：日志打印、标签注入等
 * 
 * 各云厂商实现类应继承此类，只需关注厂商特定的参数转换和API调用
 *
 * @author guo
 */
@Slf4j
public abstract class AbstractCloudEcsClient implements CloudEcsClient {

    /**
     * 租户标签注入器（子类可通过setter注入或通过构造函数传入）
     */
    protected TenantTagInjector tenantTagInjector;

    /**
     * 构造函数
     * 
     * @param tenantTagInjector 租户标签注入器
     */
    protected AbstractCloudEcsClient(TenantTagInjector tenantTagInjector) {
        this.tenantTagInjector = tenantTagInjector;
    }

    /**
     * 无参构造函数（子类可自行注入tenantTagInjector）
     */
    protected AbstractCloudEcsClient() {
    }

    /**
     * 设置租户标签注入器
     */
    public void setTenantTagInjector(TenantTagInjector tenantTagInjector) {
        this.tenantTagInjector = tenantTagInjector;
    }

    @Override
    public VirtualMachine createInstance(CreateInstanceRequest request) throws EcsException {
        // 1. 参数校验
        validateCreateRequest(request);

        // 2. 注入租户标签（通用逻辑）
        injectTenantTags(request);

        // 3. 记录日志（通用逻辑）
        logCreateInstanceStart(request);

        try {
            // 4. 调用子类实现的创建逻辑
            VirtualMachine vm = doCreateInstance(request);

            // 5. 记录成功日志
            logCreateInstanceSuccess(vm);

            return vm;
        } catch (EcsException e) {
            // 重新抛出EcsException
            logCreateInstanceError(request, e);
            throw e;
        } catch (Exception e) {
            // 包装为EcsException
            logCreateInstanceError(request, e);
            throw new EcsException(getProviderCode(), "CREATE_FAILED",
                    "创建实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的创建实例逻辑
     * 
     * @param request 创建请求（已注入标签）
     * @return 创建的虚拟机实例
     * @throws EcsException 创建失败时抛出
     */
    protected abstract VirtualMachine doCreateInstance(CreateInstanceRequest request) throws EcsException;

    /**
     * 计算实例价格（默认实现抛出未实现异常，子类应重写）
     * 
     * @param request 创建请求
     * @return 价格信息
     * @throws EcsException 查询失败时抛出
     */
    @Override
    public PriceInfo calculatePrice(CreateInstanceRequest request) throws EcsException {
        log.warn("[{}] calculatePrice方法未实现，返回null", getProviderCode());
        throw new EcsException(getProviderCode(), "NOT_IMPLEMENTED",
                "价格计算功能未实现，请重写calculatePrice方法");
    }

    /**
     * 参数校验（通用逻辑）
     */
    protected void validateCreateRequest(CreateInstanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateInstanceRequest不能为空");
        }
        if (request.getTenantId() == null || request.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("租户ID不能为空");
        }
        if (request.getImageKey() == null || request.getImageKey().trim().isEmpty()) {
            throw new IllegalArgumentException("镜像标识不能为空");
        }
        if (request.getInstanceName() == null || request.getInstanceName().trim().isEmpty()) {
            throw new IllegalArgumentException("实例名称不能为空");
        }
        if (request.getRegion() == null || request.getRegion().trim().isEmpty()) {
            throw new IllegalArgumentException("区域不能为空");
        }
    }

    /**
     * 注入租户标签（通用逻辑）
     */
    protected void injectTenantTags(CreateInstanceRequest request) {
        if (tenantTagInjector != null) {
            tenantTagInjector.inject(request);
        } else {
            // 如果没有注入器，手动注入基本标签
            Map<String, String> tags = request.getTags();
            if (tags == null) {
                tags = new HashMap<>();
                request.setTags(tags);
            }
            if (request.getTenantId() != null && !tags.containsKey(TenantTagInjector.TENANT_TAG_KEY)) {
                tags.put(TenantTagInjector.TENANT_TAG_KEY, request.getTenantId());
            }
            if (request.getUserId() != null && !tags.containsKey(TenantTagInjector.USER_TAG_KEY)) {
                tags.put(TenantTagInjector.USER_TAG_KEY, request.getUserId());
            }
            if (!tags.containsKey(TenantTagInjector.CREATED_BY_TAG_KEY)) {
                tags.put(TenantTagInjector.CREATED_BY_TAG_KEY, TenantTagInjector.CREATED_BY_TAG_VALUE);
            }
        }
    }

    /**
     * 记录创建实例开始日志
     */
    protected void logCreateInstanceStart(CreateInstanceRequest request) {
        log.info("[{}] 创建实例开始: instanceName={}, tenantId={}, userId={}, region={}, imageKey={}, gpuModel={}",
                getProviderCode(),
                request.getInstanceName(),
                request.getTenantId(),
                request.getUserId(),
                request.getRegion(),
                request.getImageKey(),
                request.getGpuModel());
    }

    /**
     * 记录创建实例成功日志
     */
    protected void logCreateInstanceSuccess(VirtualMachine vm) {
        log.info("[{}] 创建实例成功: instanceId={}, instanceName={}, status={}",
                getProviderCode(),
                vm.getInstanceId(),
                vm.getInstanceName(),
                vm.getStatus());
    }

    /**
     * 记录创建实例错误日志
     */
    protected void logCreateInstanceError(CreateInstanceRequest request, Exception e) {
        log.error("[{}] 创建实例失败: instanceName={}, tenantId={}, userId={}, error={}",
                getProviderCode(),
                request.getInstanceName(),
                request.getTenantId(),
                request.getUserId(),
                e.getMessage(),
                e);
    }

    @Override
    public boolean deleteInstance(String instanceId) throws EcsException {
        log.info("[{}] 删除实例: instanceId={}", getProviderCode(), instanceId);
        try {
            boolean result = doDeleteInstance(instanceId);
            log.info("[{}] 删除实例完成: instanceId={}, result={}", getProviderCode(), instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("[{}] 删除实例失败: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 删除实例异常: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "DELETE_FAILED",
                    "删除实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的删除实例逻辑
     */
    protected abstract boolean doDeleteInstance(String instanceId) throws EcsException;

    @Override
    public boolean startInstance(String instanceId) throws EcsException {
        log.info("[{}] 启动实例: instanceId={}", getProviderCode(), instanceId);
        try {
            boolean result = doStartInstance(instanceId);
            log.info("[{}] 启动实例完成: instanceId={}, result={}", getProviderCode(), instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("[{}] 启动实例失败: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 启动实例异常: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "START_FAILED",
                    "启动实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的启动实例逻辑
     */
    protected abstract boolean doStartInstance(String instanceId) throws EcsException;

    @Override
    public boolean stopInstance(String instanceId) throws EcsException {
        log.info("[{}] 停止实例: instanceId={}", getProviderCode(), instanceId);
        try {
            boolean result = doStopInstance(instanceId);
            log.info("[{}] 停止实例完成: instanceId={}, result={}", getProviderCode(), instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("[{}] 停止实例失败: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 停止实例异常: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "STOP_FAILED",
                    "停止实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的停止实例逻辑
     */
    protected abstract boolean doStopInstance(String instanceId) throws EcsException;

    @Override
    public boolean restartInstance(String instanceId) throws EcsException {
        log.info("[{}] 重启实例: instanceId={}", getProviderCode(), instanceId);
        try {
            boolean result = doRestartInstance(instanceId);
            log.info("[{}] 重启实例完成: instanceId={}, result={}", getProviderCode(), instanceId, result);
            return result;
        } catch (EcsException e) {
            log.error("[{}] 重启实例失败: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 重启实例异常: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "RESTART_FAILED",
                    "重启实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的重启实例逻辑
     */
    protected abstract boolean doRestartInstance(String instanceId) throws EcsException;

    @Override
    public VirtualMachine getInstance(String instanceId) throws EcsException {
        log.debug("[{}] 查询实例: instanceId={}", getProviderCode(), instanceId);
        try {
            return doGetInstance(instanceId);
        } catch (EcsException e) {
            log.error("[{}] 查询实例失败: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 查询实例异常: instanceId={}, error={}", getProviderCode(), instanceId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "GET_INSTANCE_FAILED",
                    "查询实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的查询实例逻辑
     */
    protected abstract VirtualMachine doGetInstance(String instanceId) throws EcsException;

    @Override
    public String findInstanceIdByName(String instanceName) throws EcsException {
        log.debug("[{}] 按名称查找实例: instanceName={}", getProviderCode(), instanceName);
        try {
            return doFindInstanceIdByName(instanceName);
        } catch (EcsException e) {
            log.error("[{}] 按名称查找实例失败: instanceName={}, error={}", getProviderCode(), instanceName, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[{}] 按名称查找实例异常: instanceName={}, error={}", getProviderCode(), instanceName, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "FIND_INSTANCE_FAILED",
                    "按名称查找实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 子类实现：执行实际的按名称查找实例逻辑
     */
    protected abstract String doFindInstanceIdByName(String instanceName) throws EcsException;
}

