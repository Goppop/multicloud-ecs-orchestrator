package io.github.multicloud.ecs.test.controller;

import io.github.multicloud.ecs.api.MultiCloudEcsService;
import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.enums.BandwidthMode;
import io.github.multicloud.ecs.api.enums.InstanceChargeMode;
import io.github.multicloud.ecs.api.exception.EcsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ECS 测试控制器
 * 提供 REST API 来测试多云ECS框架的功能
 *
 * @author guo
 */
@Slf4j
@RestController
@RequestMapping("/api/ecs")
public class EcsTestController {

    @Resource
    private MultiCloudEcsService multiCloudEcsService;

    /**
     * 创建实例
     */
    @PostMapping("/instances")
    public ResponseEntity<Map<String, Object>> createInstance(@RequestBody CreateInstanceRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("收到创建实例请求: {}", request);
            VirtualMachine vm = multiCloudEcsService.createInstance(request);
            result.put("success", true);
            result.put("data", vm);
            result.put("message", "实例创建成功");
            return ResponseEntity.ok(result);
        } catch (EcsException e) {
            log.error("创建实例失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("provider", e.getProviderCode());
            result.put("code", e.getErrorCode());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("创建实例异常", e);
            result.put("success", false);
            result.put("error", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 查询实例详情
     */
    @GetMapping("/instances/{providerCode}/{instanceId}")
    public ResponseEntity<Map<String, Object>> getInstance(
            @PathVariable String providerCode,
            @PathVariable String instanceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            VirtualMachine vm = multiCloudEcsService.getInstance(providerCode, instanceId);
            result.put("success", true);
            result.put("data", vm);
            return ResponseEntity.ok(result);
        } catch (EcsException e) {
            log.error("查询实例失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 删除实例
     */
    @DeleteMapping("/instances/{providerCode}/{instanceId}")
    public ResponseEntity<Map<String, Object>> deleteInstance(
            @PathVariable String providerCode,
            @PathVariable String instanceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = multiCloudEcsService.deleteInstance(providerCode, instanceId);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
            return ResponseEntity.ok(result);
        } catch (EcsException e) {
            log.error("删除实例失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取已注册的云厂商列表
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        Map<String, Object> result = new HashMap<>();
        List<String> providers = multiCloudEcsService.getRegisteredProviders();
        result.put("success", true);
        result.put("data", providers);
        result.put("count", providers.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 快速创建测试实例（使用默认参数）
     */
    @PostMapping("/instances/quick")
    public ResponseEntity<Map<String, Object>> quickCreateInstance(
            @RequestParam(required = false, defaultValue = "ALIYUN") String provider,
            @RequestParam(required = false, defaultValue = "test-instance") String instanceName,
            @RequestParam(required = false, defaultValue = "cn-hangzhou") String region) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建创建请求
            CreateInstanceRequest request = CreateInstanceRequest.builder()
                    .provider(provider)
                    .region(region)
                    .tenantId("test-tenant-001")
                    .userId("test-user-001")
                    .instanceName(instanceName)
                    .imageKey("centos-7.9")
                    .instanceType("ecs.g7.large")
                    .systemDiskSize(40)
                    .systemDiskType("cloud_essd")
                    .allocatePublicIp(false)
                    .instanceChargeMode(InstanceChargeMode.ON_DEMAND)
                    .bandwidthMode(BandwidthMode.FIXED)
                    .description("测试实例")
                    .build();

            log.info("快速创建实例: {}", request);
            VirtualMachine vm = multiCloudEcsService.createInstance(request);
            result.put("success", true);
            result.put("data", vm);
            result.put("message", "实例创建成功");
            return ResponseEntity.ok(result);
        } catch (EcsException e) {
            log.error("快速创建实例失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("provider", e.getProviderCode());
            result.put("code", e.getErrorCode());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("快速创建实例异常", e);
            result.put("success", false);
            result.put("error", "系统异常: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

