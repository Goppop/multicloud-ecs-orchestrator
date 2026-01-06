package io.github.multicloud.ecs.test.runner;

import io.github.multicloud.ecs.api.MultiCloudEcsService;
import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.enums.BandwidthMode;
import io.github.multicloud.ecs.api.enums.InstanceChargeMode;
import io.github.multicloud.ecs.api.exception.EcsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * ECS 测试运行器
 * 应用启动后自动执行测试用例
 *
 * @author guo
 */
@Slf4j
@Component
public class EcsTestRunner implements CommandLineRunner {

    @Resource
    private MultiCloudEcsService multiCloudEcsService;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 多云ECS框架测试开始 ==========");

        // 1. 测试获取已注册的云厂商
        testGetProviders();

        // 2. 测试创建实例（注释掉，避免实际创建实例产生费用）
        // testCreateInstance();

        log.info("========== 多云ECS框架测试完成 ==========");
        log.info("提示: 可以通过 REST API 进行测试:");
        log.info("  GET  http://localhost:8080/api/ecs/providers - 获取云厂商列表");
        log.info("  POST http://localhost:8080/api/ecs/instances/quick - 快速创建实例");
        log.info("  POST http://localhost:8080/api/ecs/instances - 创建实例（JSON）");
    }

    /**
     * 测试获取已注册的云厂商
     */
    private void testGetProviders() {
        log.info("--- 测试1: 获取已注册的云厂商 ---");
        try {
            List<String> providers = multiCloudEcsService.getRegisteredProviders();
            log.info("已注册的云厂商数量: {}", providers.size());
            for (String provider : providers) {
                boolean available = multiCloudEcsService.isProviderAvailable(provider);
                log.info("  云厂商: {}, 可用性: {}", provider, available ? "可用" : "不可用");
            }
        } catch (Exception e) {
            log.error("获取云厂商列表失败", e);
        }
    }

    /**
     * 测试创建实例
     * 注意: 此方法会实际创建云实例，可能产生费用，请谨慎使用
     */
    private void testCreateInstance() {
        log.info("--- 测试2: 创建实例 ---");
        try {
            // 构建创建请求
            CreateInstanceRequest request = CreateInstanceRequest.builder()
                    .provider("ALIYUN")
                    .region("cn-hangzhou")
                    .tenantId("test-tenant-001")
                    .userId("test-user-001")
                    .instanceName("test-instance-" + System.currentTimeMillis())
                    .imageKey("centos-7.9")
                    .instanceType("ecs.g7.large")
                    .systemDiskSize(40)
                    .systemDiskType("cloud_essd")
                    .allocatePublicIp(false)
                    .instanceChargeMode(InstanceChargeMode.ON_DEMAND)
                    .bandwidthMode(BandwidthMode.FIXED)
                    .description("测试实例")
                    .build();

            log.info("创建实例请求: {}", request);
            VirtualMachine vm = multiCloudEcsService.createInstance(request);
            log.info("实例创建成功!");
            log.info("  实例ID: {}", vm.getInstanceId());
            log.info("  实例名称: {}", vm.getInstanceName());
            log.info("  云厂商: {}", vm.getProvider());
            log.info("  状态: {}", vm.getStatus());
            log.info("  区域: {}", vm.getRegion());

        } catch (EcsException e) {
            log.error("创建实例失败: provider={}, code={}, message={}",
                    e.getProviderCode(), e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("创建实例异常", e);
        }
    }
}

