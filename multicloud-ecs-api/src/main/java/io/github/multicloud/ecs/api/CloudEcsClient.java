package io.github.multicloud.ecs.api;

import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.PriceInfo;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.exception.EcsException;

/**
 * 云厂商ECS客户端接口
 * 各云厂商的Provider需要实现此接口
 *
 * @author guo
 */
public interface CloudEcsClient {

    /**
     * 获取云厂商代码
     * 用于注册和路由
     *
     * @return 云厂商代码，如 ALIYUN, SCC, TENCENT
     */
    String getProviderCode();

    /**
     * 获取云厂商名称
     *
     * @return 云厂商名称，如 阿里云, 苏州移动云
     */
    String getProviderName();

    /**
     * 计算实例价格
     * 用于商城结算系统在创建实例前获取报价
     *
     * @param request 创建请求
     * @return 价格信息
     * @throws EcsException 查询失败时抛出
     */
    PriceInfo calculatePrice(CreateInstanceRequest request) throws EcsException;

    /**
     * 创建实例
     *
     * @param request 创建请求
     * @return 创建结果（可能是创建中状态）
     * @throws EcsException 创建失败时抛出
     */
    VirtualMachine createInstance(CreateInstanceRequest request) throws EcsException;

    /**
     * 删除实例
     *
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 删除失败时抛出
     */
    boolean deleteInstance(String instanceId) throws EcsException;

    /**
     * 启动实例
     *
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 启动失败时抛出
     */
    boolean startInstance(String instanceId) throws EcsException;

    /**
     * 停止实例
     *
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 停止失败时抛出
     */
    boolean stopInstance(String instanceId) throws EcsException;

    /**
     * 重启实例
     *
     * @param instanceId 云厂商实例ID
     * @return 是否成功
     * @throws EcsException 重启失败时抛出
     */
    boolean restartInstance(String instanceId) throws EcsException;

    /**
     * 查询实例详情
     *
     * @param instanceId 云厂商实例ID
     * @return 实例详情，不存在返回null
     * @throws EcsException 查询失败时抛出
     */
    VirtualMachine getInstance(String instanceId) throws EcsException;

    /**
     * 根据实例名称查找实例ID
     * 用于将业务ID转换为云厂商实例ID
     *
     * @param instanceName 实例名称
     * @return 实例ID，未找到返回null
     * @throws EcsException 查询失败时抛出
     */
    String findInstanceIdByName(String instanceName) throws EcsException;

    /**
     * 检查客户端是否可用
     * 用于健康检查
     *
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取客户端优先级
     * 用于智能调度时的排序（值越小优先级越高）
     *
     * @return 优先级，默认100
     */
    default int getPriority() {
        return 100;
    }
}

