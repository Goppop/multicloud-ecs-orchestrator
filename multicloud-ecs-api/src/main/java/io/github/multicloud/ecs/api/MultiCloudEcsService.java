package io.github.multicloud.ecs.api;

import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.exception.EcsException;

import java.util.List;

/**
 * 多云ECS统一服务接口
 * 作为业务层调用的入口，屏蔽底层调度和云厂商差异
 *
 * @author guo
 */
public interface MultiCloudEcsService {

    /**
     * 创建实例
     * 根据调度策略选择云厂商并创建实例
     *
     * @param request 创建请求
     * @return 创建结果
     * @throws EcsException 创建失败时抛出
     */
    VirtualMachine createInstance(CreateInstanceRequest request) throws EcsException;

    /**
     * 删除实例
     *
     * @param providerCode 云厂商代码
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 删除失败时抛出
     */
    boolean deleteInstance(String providerCode, String instanceId) throws EcsException;

    /**
     * 启动实例
     *
     * @param providerCode 云厂商代码
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 启动失败时抛出
     */
    boolean startInstance(String providerCode, String instanceId) throws EcsException;

    /**
     * 停止实例
     *
     * @param providerCode 云厂商代码
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 停止失败时抛出
     */
    boolean stopInstance(String providerCode, String instanceId) throws EcsException;

    /**
     * 重启实例
     *
     * @param providerCode 云厂商代码
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 重启失败时抛出
     */
    boolean restartInstance(String providerCode, String instanceId) throws EcsException;

    /**
     * 查询实例详情
     *
     * @param providerCode 云厂商代码
     * @param instanceId 云厂商实例ID
     * @return 实例详情，不存在返回null
     * @throws EcsException 查询失败时抛出
     */
    VirtualMachine getInstance(String providerCode, String instanceId) throws EcsException;

    /**
     * 根据实例名称查找实例ID
     *
     * @param providerCode 云厂商代码
     * @param instanceName 实例名称
     * @return 实例ID，未找到返回null
     * @throws EcsException 查询失败时抛出
     */
    String findInstanceIdByName(String providerCode, String instanceName) throws EcsException;

    /**
     * 获取所有已注册的云厂商代码
     *
     * @return 云厂商代码列表
     */
    List<String> getRegisteredProviders();

    /**
     * 检查指定云厂商是否可用
     *
     * @param providerCode 云厂商代码
     * @return 是否可用
     */
    boolean isProviderAvailable(String providerCode);
}

