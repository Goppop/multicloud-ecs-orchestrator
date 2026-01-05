package io.github.multicloud.ecs.api;

import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.exception.EcsException;

/**
 * ECS调度器接口
 * 负责根据请求选择合适的云厂商客户端
 *
 * V1: FixedScheduler - 根据request.provider固定路由
 * V2: CostOptimizedScheduler - 自动比价选择最优云厂商（预留）
 *
 * @author guo
 */
public interface EcsScheduler {

    /**
     * 根据请求选择云厂商客户端
     *
     * @param request 创建请求
     * @return 选中的云厂商客户端
     * @throws EcsException 无法选择时抛出（如provider未注册）
     */
    CloudEcsClient select(CreateInstanceRequest request) throws EcsException;

    /**
     * 获取调度器名称
     *
     * @return 调度器名称，如 FixedScheduler, CostOptimizedScheduler
     */
    String getName();

    /**
     * 获取调度器描述
     *
     * @return 调度器描述
     */
    default String getDescription() {
        return getName();
    }

    /**
     * 是否需要指定provider
     * V1返回true，V2返回false
     *
     * @return 是否需要指定provider
     */
    default boolean requireProvider() {
        return true;
    }
}

