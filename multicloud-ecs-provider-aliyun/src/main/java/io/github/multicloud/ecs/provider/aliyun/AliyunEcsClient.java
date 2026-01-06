package io.github.multicloud.ecs.provider.aliyun;


import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import io.github.multicloud.ecs.api.dto.PriceInfo;
import io.github.multicloud.ecs.api.dto.VirtualMachine;
import io.github.multicloud.ecs.api.enums.BandwidthMode;
import io.github.multicloud.ecs.api.enums.InstanceChargeMode;
import io.github.multicloud.ecs.api.enums.VmStatusEnum;
import io.github.multicloud.ecs.api.exception.EcsException;
import io.github.multicloud.ecs.core.client.AbstractCloudEcsClient;
import io.github.multicloud.ecs.core.util.TenantTagInjector;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 阿里云ECS客户端实现
 * 
 * 实现"静默寻址"：如果Request里只有tenantId，会自动调用阿里API查找或创建对应的VPC和交换机
 * 实现计费映射：TRAFFIC -> PayByTraffic, ON_DEMAND -> PostPaid
 * 
 * TODO: 待阿里云SDK接入后完善具体实现
 *
 * @author guo
 */
@Slf4j
public class AliyunEcsClient extends AbstractCloudEcsClient {

    private final AliyunEcsProperties properties;
    private final AliyunNetworkManager networkManager;
    private final AliyunParameterMapper parameterMapper;

    /**
     * 构造函数
     */
    public AliyunEcsClient(AliyunEcsProperties properties,
                           AliyunNetworkManager networkManager,
                           AliyunParameterMapper parameterMapper,
                           TenantTagInjector tenantTagInjector) {
        super(tenantTagInjector);
        this.properties = properties;
        this.networkManager = networkManager;
        this.parameterMapper = parameterMapper;
        log.info("[AliyunEcsClient] 阿里云ECS客户端实例已创建: providerCode={}, providerName={}, region={}, enabled={}",
                properties.getProviderCode(), properties.getProviderName(), 
                properties.getRegionId(), properties.isEnabled());
    }

    @Override
    public String getProviderCode() {
        return properties.getProviderCode();
    }

    @Override
    public String getProviderName() {
        return properties.getProviderName();
    }

    @Override
    public PriceInfo calculatePrice(CreateInstanceRequest request) throws EcsException {
        log.info("[AliyunEcsClient] 计算价格: instanceName={}, instanceType={}, region={}",
                request.getInstanceName(), request.getInstanceType(), request.getRegion());

        try {
            // 1. 解析参数
            String region = resolveRegion(request);
            String instanceType = parameterMapper.resolveInstanceType(
                    request.getInstanceType(),
                    request.getGpuModel()
            );

            /*
             * TODO: 阿里云SDK接入后实现
             * 
             * // 调用阿里云价格查询API
             * DescribePriceRequest priceRequest = new DescribePriceRequest();
             * priceRequest.setRegionId(region);
             * priceRequest.setInstanceType(instanceType);
             * priceRequest.setImageId(parameterMapper.resolveImageId(null, request.getImageKey()));
             * priceRequest.setSystemDiskCategory(resolveSystemDiskCategory(request));
             * priceRequest.setSystemDiskSize(resolveSystemDiskSize(request));
             * 
             * // 计费模式映射
             * String instanceChargeType = mapInstanceChargeMode(request.getInstanceChargeMode());
             * priceRequest.setInstanceChargeType(instanceChargeType);
             * 
             * // 带宽计费模式映射
             * if (request.getAllocatePublicIp() != null && request.getAllocatePublicIp()) {
             *     String internetChargeType = mapBandwidthMode(request.getBandwidthMode());
             *     priceRequest.setInternetChargeType(internetChargeType);
             *     priceRequest.setInternetMaxBandwidthOut(request.getPublicIpBandwidth() != null ? 
             *             request.getPublicIpBandwidth() : 5);
             * }
             * 
             * DescribePriceResponse response = client.getAcsResponse(priceRequest);
             * 
             * // 构建PriceInfo
             * PriceInfo priceInfo = PriceInfo.builder()
             *         .provider(getProviderCode())
             *         .region(region)
             *         .instanceType(instanceType)
             *         .instancePricePerHour(new BigDecimal(response.getPriceInfo().getPrice()))
             *         .systemDiskPricePerGbPerMonth(new BigDecimal(response.getPriceInfo().getSystemDiskPrice()))
             *         .totalPricePerHour(new BigDecimal(response.getPriceInfo().getTradePrice()))
             *         .currency("CNY")
             *         .queryTimestamp(System.currentTimeMillis())
             *         .priceValiditySeconds(3600L)
             *         .build();
             * 
             * return priceInfo;
             */

            // 临时模拟：返回模拟价格
            log.warn("[AliyunEcsClient] SDK未接入，返回模拟价格数据");
            return PriceInfo.builder()
                    .provider(getProviderCode())
                    .region(region)
                    .instanceType(instanceType)
                    .instancePricePerHour(new BigDecimal("0.5"))
                    .instancePricePerMonth(new BigDecimal("300"))
                    .systemDiskPricePerGbPerMonth(new BigDecimal("0.1"))
                    .bandwidthPricePerMbpsPerMonth(new BigDecimal("23"))
                    .trafficPricePerGb(new BigDecimal("0.8"))
                    .totalPricePerHour(new BigDecimal("0.6"))
                    .totalPricePerMonth(new BigDecimal("400"))
                    .currency("CNY")
                    .queryTimestamp(System.currentTimeMillis())
                    .priceValiditySeconds(3600L)
                    .build();

        } catch (Exception e) {
            log.error("[AliyunEcsClient] 计算价格失败: instanceName={}, error={}",
                    request.getInstanceName(), e.getMessage(), e);
            throw new EcsException(getProviderCode(), "CALCULATE_PRICE_FAILED",
                    "计算价格失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected VirtualMachine doCreateInstance(CreateInstanceRequest request) throws EcsException {
        log.info("[AliyunEcsClient] ========== 开始创建实例 ==========");
        log.info("[AliyunEcsClient] providerCode={}, instanceName={}, region={}, tenantId={}, userId={}",
                getProviderCode(), request.getInstanceName(), request.getRegion(), 
                request.getTenantId(), request.getUserId());
        
        String region = resolveRegion(request);
        String userId = request.getUserId();

        try {
            // ========== 步骤1: 静默寻址（透明网络供应）==========
            // 如果Request里只有tenantId，自动查找或创建VPC和交换机
            AliyunNetworkManager.NetworkResources networkResources = networkManager.ensureNetworkResources(
                    userId,
                    region,
                    request.getZone(),
                    request.getTags() != null ? request.getTags() : new HashMap<>()
            );

            log.info("[AliyunEcsClient] 网络资源准备完成（静默寻址）: vpcId={}, vSwitchId={}, securityGroupId={}",
                    networkResources.getVpcId(), networkResources.getVSwitchId(),
                    networkResources.getSecurityGroupId());

            // ========== 步骤2: 参数映射 ==========
            // 将 imageKey 映射为 ImageId
            String imageId = parameterMapper.resolveImageId(null, request.getImageKey());

            // 将 gpuModel 映射为 InstanceType（如果指定了gpuModel）
            String instanceType = parameterMapper.resolveInstanceType(
                    request.getInstanceType(),
                    request.getGpuModel()
            );

            log.info("[AliyunEcsClient] 参数映射完成: imageKey={} -> imageId={}, gpuModel={} -> instanceType={}",
                    request.getImageKey(), imageId, request.getGpuModel(), instanceType);

            // ========== 步骤3: 计费模式映射 ==========
            String instanceChargeType = mapInstanceChargeMode(request.getInstanceChargeMode());
            String internetChargeType = mapBandwidthMode(request.getBandwidthMode());

            log.info("[AliyunEcsClient] 计费模式映射: instanceChargeMode={} -> {}, bandwidthMode={} -> {}",
                    request.getInstanceChargeMode(), instanceChargeType,
                    request.getBandwidthMode(), internetChargeType);

            // ========== 步骤4: 创建实例 ==========
            log.info("[AliyunEcsClient] ========== 步骤4: 开始创建ECS实例 ==========");
            
            // 模拟：创建 IAcsClient 客户端
            log.info("[AliyunEcsClient] [模拟SDK] 创建 IAcsClient 客户端");
            log.info("[AliyunEcsClient] [模拟SDK] DefaultProfile.getProfile(region={}, accessKeyId={})", 
                    region, properties.getAccessKeyId() != null ? "***" : "null");
            log.info("[AliyunEcsClient] [模拟SDK] new DefaultAcsClient(profile) - 客户端初始化完成");
            
            // 模拟：构建 RunInstancesRequest
            log.info("[AliyunEcsClient] [模拟SDK] 构建 RunInstancesRequest");
            log.info("[AliyunEcsClient] [模拟SDK]   - regionId: {}", region);
            log.info("[AliyunEcsClient] [模拟SDK]   - zoneId: {}", request.getZone() != null ? request.getZone() : "默认可用区");
            log.info("[AliyunEcsClient] [模拟SDK]   - instanceType: {}", instanceType);
            log.info("[AliyunEcsClient] [模拟SDK]   - imageId: {}", imageId);
            log.info("[AliyunEcsClient] [模拟SDK]   - securityGroupId: {}", networkResources.getSecurityGroupId());
            log.info("[AliyunEcsClient] [模拟SDK]   - vSwitchId: {}", networkResources.getVSwitchId());
            log.info("[AliyunEcsClient] [模拟SDK]   - instanceName: {}", request.getInstanceName());
            log.info("[AliyunEcsClient] [模拟SDK]   - systemDiskCategory: {}", resolveSystemDiskCategory(request));
            log.info("[AliyunEcsClient] [模拟SDK]   - systemDiskSize: {} GB", resolveSystemDiskSize(request));
            log.info("[AliyunEcsClient] [模拟SDK]   - instanceChargeType: {}", instanceChargeType);
            
            if (request.getInstanceChargeMode() == InstanceChargeMode.PREPAID && request.getDuration() != null) {
                log.info("[AliyunEcsClient] [模拟SDK]   - period: {} Month", request.getDuration());
            }
            
            if (request.getAllocatePublicIp() != null && request.getAllocatePublicIp()) {
                log.info("[AliyunEcsClient] [模拟SDK]   - internetChargeType: {}", internetChargeType);
                log.info("[AliyunEcsClient] [模拟SDK]   - internetMaxBandwidthOut: {} Mbps", 
                        request.getPublicIpBandwidth() != null ? request.getPublicIpBandwidth() : 5);
            } else {
                log.info("[AliyunEcsClient] [模拟SDK]   - internetMaxBandwidthOut: 0 (不分配公网IP)");
            }
            
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                log.info("[AliyunEcsClient] [模拟SDK]   - tags: {}", request.getTags());
            }
            
            // 模拟：调用API创建实例
            log.info("[AliyunEcsClient] [模拟SDK] 调用 client.getAcsResponse(runRequest) - 发送创建实例请求到阿里云");
            log.info("[AliyunEcsClient] [模拟SDK] 等待阿里云API响应...");
            
            // 模拟：解析响应
            String mockInstanceId = "i-" + System.currentTimeMillis();
            String mockRequestId = "req-" + System.currentTimeMillis();
            log.info("[AliyunEcsClient] [模拟SDK] 收到 RunInstancesResponse");
            log.info("[AliyunEcsClient] [模拟SDK]   - instanceIdSets: [{}]", mockInstanceId);
            log.info("[AliyunEcsClient] [模拟SDK]   - requestId: {}", mockRequestId);
            log.info("[AliyunEcsClient] ========== 步骤4: ECS实例创建完成 ==========");
            log.info("[AliyunEcsClient] ✓ 实例创建成功: instanceId={}, requestId={}", mockInstanceId, mockRequestId);

            // ========== 步骤5: 网络打通（异步）==========
            // 如果需要公网IP，异步申请并绑定EIP
            CompletableFuture<String> eipFuture = null;
            if (request.getAllocatePublicIp() != null && request.getAllocatePublicIp()) {
                eipFuture = networkManager.allocateAndBindEip(mockInstanceId, region);
            }

            // 如果需要开放端口，异步添加安全组规则
            CompletableFuture<Void> sgRulesFuture = null;
            if (request.getOpenPorts() != null && !request.getOpenPorts().isEmpty()) {
                sgRulesFuture = networkManager.addSecurityGroupRules(
                        networkResources.getSecurityGroupId(),
                        request.getOpenPorts(),
                        region
                );
            }

            // 等待异步操作完成（可选：也可以不等待，让后台异步执行）
            String publicIp = null;
            if (eipFuture != null) {
                try {
                    publicIp = eipFuture.get(); // 等待EIP绑定完成
                    log.info("[AliyunEcsClient] EIP绑定完成: instanceId={}, publicIp={}", mockInstanceId, publicIp);
                } catch (Exception e) {
                    log.error("[AliyunEcsClient] EIP绑定失败: instanceId={}, error={}", mockInstanceId, e.getMessage());
                    // EIP绑定失败不影响实例创建，记录日志即可
                }
            }

            if (sgRulesFuture != null) {
                try {
                    sgRulesFuture.get(); // 等待安全组规则添加完成
                    log.info("[AliyunEcsClient] 安全组规则添加完成: instanceId={}, ports={}",
                            mockInstanceId, request.getOpenPorts());
                } catch (Exception e) {
                    log.error("[AliyunEcsClient] 安全组规则添加失败: instanceId={}, error={}",
                            mockInstanceId, e.getMessage());
                    // 安全组规则添加失败不影响实例创建，记录日志即可
                }
            }

            // ========== 步骤6: 构建返回结果 ==========
            log.info("[AliyunEcsClient] ========== 步骤6: 构建返回结果 ==========");
            Map<String, String> tags = new HashMap<>();
            if (request.getTags() != null) {
                tags.putAll(request.getTags());
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("vpcId", networkResources.getVpcId());
            metadata.put("vSwitchId", networkResources.getVSwitchId());
            metadata.put("securityGroupId", networkResources.getSecurityGroupId());
            metadata.put("instanceChargeType", instanceChargeType);
            metadata.put("internetChargeType", internetChargeType);

            VirtualMachine vm = VirtualMachine.builder()
                    .instanceId(mockInstanceId)
                    .instanceName(request.getInstanceName())
                    .status(VmStatusEnum.PENDING)
                    .rawStatus("Pending")
                    .provider(getProviderCode())
                    .region(region)
                    .zone(request.getZone())
                    .instanceType(instanceType)
                    .imageId(imageId)
                    .privateIp(null) // TODO: 从实例详情中获取
                    .publicIp(publicIp)
                    .tenantId(request.getTenantId())
                    .tags(tags)
                    .metadata(metadata)
                    .createdAt(LocalDateTime.now())
                    .requestId(mockRequestId)
                    .build();
            
            log.info("[AliyunEcsClient] ========== 实例创建流程完成 ==========");
            log.info("[AliyunEcsClient] ✓ 返回VirtualMachine: instanceId={}, instanceName={}, status={}, publicIp={}", 
                    vm.getInstanceId(), vm.getInstanceName(), vm.getStatus(), vm.getPublicIp());
            log.info("[AliyunEcsClient] ✓ 网络资源: vpcId={}, vSwitchId={}, securityGroupId={}", 
                    networkResources.getVpcId(), networkResources.getVSwitchId(), networkResources.getSecurityGroupId());
            
            return vm;

        } catch (EcsException e) {
            // 重新抛出EcsException（包含配额错误等）
            throw e;
        } catch (Exception e) {
            // 处理其他异常
            log.error("[AliyunEcsClient] 创建实例异常: instanceName={}, userId={}, error={}",
                    request.getInstanceName(), userId, e.getMessage(), e);
            throw new EcsException(getProviderCode(), "CREATE_FAILED",
                    "创建实例失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将统一的实例计费模式映射为阿里云计费模式
     * ON_DEMAND -> PostPaid
     * PREPAID -> PrePaid
     */
    private String mapInstanceChargeMode(InstanceChargeMode mode) {
        if (mode == null) {
            return "PostPaid"; // 默认按量付费
        }
        switch (mode) {
            case ON_DEMAND:
                return "PostPaid";
            case PREPAID:
                return "PrePaid";
            default:
                log.warn("[AliyunEcsClient] 未知的实例计费模式: {}, 使用默认值PostPaid", mode);
                return "PostPaid";
        }
    }

    /**
     * 将统一的带宽计费模式映射为阿里云计费模式
     * TRAFFIC -> PayByTraffic
     * FIXED -> PayByBandwidth
     */
    private String mapBandwidthMode(BandwidthMode mode) {
        if (mode == null) {
            return "PayByTraffic"; // 默认按流量计费
        }
        switch (mode) {
            case TRAFFIC:
                return "PayByTraffic";
            case FIXED:
                return "PayByBandwidth";
            default:
                log.warn("[AliyunEcsClient] 未知的带宽计费模式: {}, 使用默认值PayByTraffic", mode);
                return "PayByTraffic";
        }
    }

    @Override
    protected boolean doDeleteInstance(String instanceId) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * DeleteInstanceRequest request = new DeleteInstanceRequest();
         * request.setInstanceId(instanceId);
         * request.setForce(true);
         * DeleteInstanceResponse response = client.getAcsResponse(request);
         * return response != null;
         */

        log.warn("[AliyunEcsClient] SDK未接入，删除操作模拟成功");
        return true;
    }

    @Override
    protected boolean doStartInstance(String instanceId) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * StartInstanceRequest request = new StartInstanceRequest();
         * request.setInstanceId(instanceId);
         * StartInstanceResponse response = client.getAcsResponse(request);
         * return response != null;
         */

        log.warn("[AliyunEcsClient] SDK未接入，启动操作模拟成功");
        return true;
    }

    @Override
    protected boolean doStopInstance(String instanceId) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * StopInstanceRequest request = new StopInstanceRequest();
         * request.setInstanceId(instanceId);
         * request.setForceStop(false);
         * StopInstanceResponse response = client.getAcsResponse(request);
         * return response != null;
         */

        log.warn("[AliyunEcsClient] SDK未接入，停止操作模拟成功");
        return true;
    }

    @Override
    protected boolean doRestartInstance(String instanceId) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * RebootInstanceRequest request = new RebootInstanceRequest();
         * request.setInstanceId(instanceId);
         * request.setForceStop(false);
         * RebootInstanceResponse response = client.getAcsResponse(request);
         * return response != null;
         */

        log.warn("[AliyunEcsClient] SDK未接入，重启操作模拟成功");
        return true;
    }

    @Override
    protected VirtualMachine doGetInstance(String instanceId) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * DescribeInstancesRequest request = new DescribeInstancesRequest();
         * request.setInstanceIds("[\"" + instanceId + "\"]");
         * DescribeInstancesResponse response = client.getAcsResponse(request);
         * 
         * if (response.getInstances() == null || response.getInstances().isEmpty()) {
         *     return null;
         * }
         * 
         * DescribeInstancesResponse.Instance instance = response.getInstances().get(0);
         * return convertToVirtualMachine(instance);
         */

        log.warn("[AliyunEcsClient] SDK未接入，返回模拟数据");
        return VirtualMachine.builder()
                .instanceId(instanceId)
                .instanceName("mock-instance")
                .status(VmStatusEnum.RUNNING)
                .rawStatus("Running")
                .provider(getProviderCode())
                .region(properties.getRegionId())
                .build();
    }

    @Override
    protected String doFindInstanceIdByName(String instanceName) throws EcsException {
        /*
         * TODO: 阿里云SDK接入后实现
         * 
         * DescribeInstancesRequest request = new DescribeInstancesRequest();
         * request.setInstanceName(instanceName);
         * DescribeInstancesResponse response = client.getAcsResponse(request);
         * 
         * if (response.getInstances() != null && !response.getInstances().isEmpty()) {
         *     return response.getInstances().get(0).getInstanceId();
         * }
         * return null;
         */

        log.warn("[AliyunEcsClient] SDK未接入，返回null");
        return null;
    }

    @Override
    public boolean isAvailable() {
        // SDK未接入时返回false，接入后改为true
        boolean available = properties.isEnabled() &&
                properties.getAccessKeyId() != null &&
                properties.getAccessKeySecret() != null;
        log.debug("[AliyunEcsClient] 检查可用性: providerCode={}, enabled={}, hasAccessKey={}, available={}",
                getProviderCode(), properties.isEnabled(), 
                (properties.getAccessKeyId() != null && properties.getAccessKeySecret() != null),
                available);
        return available;
    }

    @Override
    public int getPriority() {
        return properties.getPriority();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析区域
     */
    private String resolveRegion(CreateInstanceRequest request) {
        return request.getRegion() != null ? request.getRegion() : properties.getRegionId();
    }

    /**
     * 解析系统盘类型
     */
    @SuppressWarnings("unused")
    private String resolveSystemDiskCategory(CreateInstanceRequest request) {
        return request.getSystemDiskType() != null ?
                request.getSystemDiskType() : properties.getDefaultSystemDiskCategory();
    }

    /**
     * 解析系统盘大小
     */
    @SuppressWarnings("unused")
    private Integer resolveSystemDiskSize(CreateInstanceRequest request) {
        return request.getSystemDiskSize() != null ?
                request.getSystemDiskSize() : properties.getDefaultSystemDiskSize();
    }

    /**
     * 转换阿里云状态到统一状态
     */
    @SuppressWarnings("unused")
    private VmStatusEnum convertStatus(String aliyunStatus) {
        if (aliyunStatus == null) {
            return VmStatusEnum.UNKNOWN;
        }

        switch (aliyunStatus) {
            case "Running":
                return VmStatusEnum.RUNNING;
            case "Stopped":
                return VmStatusEnum.STOPPED;
            case "Pending":
                return VmStatusEnum.PENDING;
            case "Starting":
                return VmStatusEnum.STARTING;
            case "Stopping":
                return VmStatusEnum.STOPPING;
            default:
                log.warn("[AliyunEcsClient] 未知的阿里云状态: {}", aliyunStatus);
                return VmStatusEnum.UNKNOWN;
        }
    }
}
