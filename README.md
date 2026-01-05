# CloudBridge

**面向算力场景的轻量级多云资源编排引擎**

CloudBridge 是一个基于 Java 开发的基础设施自动化框架，旨在为开发者提供一套统一、标准化的 API，用以抹平不同公有云厂商（如阿里云、AWS、腾讯云等）在计算资源（ECS/EC2）交付上的巨大差异。

通过 CloudBridge，业务系统无需关心底层云厂商的 SDK 细节及复杂的网络规划，只需通过声明式的 Request 即可实现资源的分钟级自动化交付。

## 核心特性

- **统一接口**：一套 API 管理多个云厂商，业务层与云厂商 SDK 彻底解耦
- **插件化架构**：每个云厂商独立模块，按需引入，易于扩展
- **智能调度**：支持固定路由、成本优化等多种调度策略
- **自动配置**：Spring Boot Starter，开箱即用
- **网络即服务 (NaaS)**：内置 CIDR 自动规划算法，实现租户级 VPC 环境的静默初始化与物理隔离
- **多租户支持**：自动注入租户标签，实现资源隔离和生命周期管理
- **状态最终一致性**：异步编排与幂等重试机制，确保资源交付的可靠性

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.multicloud</groupId>
    <artifactId>multicloud-ecs-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 中添加配置：

```yaml
multicloud:
  ecs:
    enabled: true                    # 启用多云ECS框架
    scheduler-type: fixed            # V1版本使用固定调度
    
    # 阿里云配置
    aliyun:
      enabled: true
      region-id: cn-hangzhou
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
```

### 3. 使用示例

```java
@Service
@RequiredArgsConstructor
public class YourBusinessService {

    @Resource
    private MultiCloudEcsService multiCloudEcsService;
    
    public VirtualMachine createInstance() {
        CreateInstanceRequest request = CreateInstanceRequest.builder()
                .provider("ALIYUN")
                .tenantId("t-001")
                .userId("u-001")
                .instanceName("web-server")
                .region("cn-hangzhou")
                .imageKey("centos-7.9")
                .instanceType("ecs.g7.large")
                .systemDiskSize(50)
                .allocatePublicIp(true)
                .publicIpBandwidth(10)
                .bandwidthMode(BandwidthMode.TRAFFIC)
                .instanceChargeMode(InstanceChargeMode.ON_DEMAND)
                .build();
        
        return multiCloudEcsService.createInstance(request);
    }
}
```

## 核心设计哲学

- **厂商无关性 (Cloud-Agnostic)**：业务层与云厂商 SDK 彻底解耦，一套代码多云运行。
- **网络即服务 (NaaS)**：内置 CIDR 自动规划算法，实现租户级 VPC 环境的静默初始化与物理隔离。
- **状态最终一致性**：针对云 API 的长耗时与不稳定性，采用异步编排与幂等重试机制，确保资源交付的可靠性。

## 核心功能模块

### 统一声明式接口

框架提供了标准化的 `CreateInstanceRequest`，将业务意图（如 GPU 型号、计费偏好）转换为厂商参数。

- **硬件抽象**：支持 `gpuModel` 映射，自动匹配各厂商最优实例规格。
- **镜像抽象**：通过 `imageKey` 机制，实现业务镜像在不同厂商间的语义统一。

### 自动寻址与隔离系统

这是本项目最核心的自动化能力。当接收到创建请求时，系统会执行以下逻辑：

1. **资源检索**：根据 `tenantId` 自动搜索目标地域的隔离网络环境。
2. **静默创建**：若环境不存在，算法会自动基于 `172.16.0.0/12` 地址池分配不冲突的 CIDR 网段，并按序创建 VPC、VSwitch 及 SecurityGroup。
3. **标签闭环**：所有网络资源自动注入归属标签，实现资源的生命周期管理。

### 计费模型标准化

框架抹平了各大厂商在计费定义上的术语差异：

- **实例维度**：统一为 `ON_DEMAND` (按量) 与 `PREPAID` (包年包月)。
- **带宽维度**：统一为 `TRAFFIC` (按流量) 与 `FIXED` (按固定带宽)。

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                     业务层 (Business Layer)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 订单系统      │  │ 算力平台      │  │ 其他业务系统  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
          ┌─────────────────▼─────────────────┐
          │    MultiCloudEcsService (统一接口)  │
          └─────────────────┬─────────────────┘
                            │
          ┌─────────────────▼─────────────────┐
          │      EcsScheduler (调度器)         │
          │  ┌──────────┐  ┌──────────┐      │
          │  │ Fixed    │  │ CostOpt  │      │
          │  └──────────┘  └──────────┘      │
          └─────────────────┬─────────────────┘
                            │
          ┌─────────────────▼─────────────────┐
          │  CloudEcsClientRegistry (注册中心) │
          └─────────────────┬─────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
│ AliyunEcsClient│  │TencentEcsClient│  │  AWSEcsClient  │
│                │  │                │  │                │
│ - NetworkMgr   │  │ - NetworkMgr   │  │ - NetworkMgr   │
│ - ParamMapper  │  │ - ParamMapper  │  │ - ParamMapper  │
└────────────────┘  └────────────────┘  └────────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
          ┌─────────────────▼─────────────────┐
          │      云厂商 SDK (Aliyun/AWS/...)   │
          └───────────────────────────────────┘
```

## 项目结构

```
multicloud-ecs-orchestrator/
├── multicloud-ecs-api/              # API模块 - 接口与DTO定义
│   ├── CloudEcsClient.java          # 云厂商客户端接口
│   ├── MultiCloudEcsService.java    # 统一服务接口
│   ├── EcsScheduler.java            # 调度器接口
│   └── dto/                         # 数据传输对象
│       ├── CreateInstanceRequest.java
│       ├── VirtualMachine.java
│       └── PriceInfo.java
├── multicloud-ecs-core/             # 核心模块 - 调度与注册逻辑
│   ├── service/                     # 统一服务实现
│   ├── scheduler/                   # 调度器实现
│   ├── registry/                    # 客户端注册中心
│   └── util/                        # 工具类
├── multicloud-ecs-starter/          # Spring Boot Starter
│   └── MultiCloudEcsAutoConfiguration.java
└── multicloud-ecs-provider-aliyun/  # 阿里云Provider
    ├── AliyunEcsClient.java
    ├── AliyunNetworkManager.java
    └── AliyunParameterMapper.java
```

### 模块说明

- **multicloud-ecs-api**: 定义统一的接口、DTO、枚举和异常类
- **multicloud-ecs-core**: 提供调度器、客户端注册、租户标签注入等核心功能
- **multicloud-ecs-starter**: Spring Boot自动配置，简化集成
- **multicloud-ecs-provider-aliyun**: 阿里云ECS实现（可扩展更多云厂商）

## 关键工作流

1. **参数预处理**：注入审计标签，验证请求合法性。
2. **网络预检**：检查租户 VPC 是否就绪，必要时执行静默寻址算法进行初始化。
3. **幂等令牌生成**：生成 `ClientToken` 以确保重试场景下资源不被重复创建。
4. **资源履行**：调用目标厂商驱动，执行实例创建。
5. **后置增强**：异步执行公网 IP 绑定、安全组端口动态放行。

## 异常与补偿体系

框架定义了完备的异常识别机制：

- **可恢复异常**：如网络抖动、厂商限流，触发指数退避重试。
- **不可恢复异常**：如库存不足、配额溢出，立即触发业务侧回调。
- **状态自愈**：通过定时轮询任务，自动同步创建中（Provisioning）实例的最终状态。

## 版本说明

- **V1.0** (当前版本): 固定调度，手动指定 provider
- **V2.0** (规划中): 成本优化调度，自动比价，支持 Spot 实例

## 未来演进

- 支持 Spot (竞价) 实例的自动抢购与成本优化策略
- 集成 Prometheus 指标，实现跨云资源用量监控
- 支持 Terraform Provider 接入
- 支持更多云厂商（腾讯云、华为云、AWS 等）

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

如有问题，请提交 [Issue](https://github.com/your-username/multicloud-ecs-orchestrator/issues)。

