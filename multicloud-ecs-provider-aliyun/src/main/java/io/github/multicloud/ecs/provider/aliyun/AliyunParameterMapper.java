package io.github.multicloud.ecs.provider.aliyun;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云参数映射器
 * 负责将业务参数映射为阿里云SDK参数
 *
 * @author guo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunParameterMapper {

    private final AliyunEcsProperties properties;

    /**
     * 镜像Key到ImageId的映射表
     * TODO: 从配置文件或数据库加载
     */
    private static final Map<String, String> IMAGE_KEY_MAPPING = new HashMap<>();
    static {
        IMAGE_KEY_MAPPING.put("centos-7.9", "centos_7_9_x64_20G_alibase_20220824.vhd");
        IMAGE_KEY_MAPPING.put("ubuntu-20.04", "ubuntu_20_04_x64_20G_alibase_20220824.vhd");
        IMAGE_KEY_MAPPING.put("pytorch-1.12", "pytorch_1_12_cuda11_3_ubuntu20_04");
        IMAGE_KEY_MAPPING.put("tensorflow-2.8", "tensorflow_2_8_cuda11_2_ubuntu20_04");
    }

    /**
     * GPU型号到实例类型的映射表
     * TODO: 从配置文件或数据库加载
     */
    private static final Map<String, String> GPU_MODEL_MAPPING = new HashMap<>();
    static {
        GPU_MODEL_MAPPING.put("A100", "ecs.gn7i-c8g1.2xlarge");  // A100 GPU
        GPU_MODEL_MAPPING.put("V100", "ecs.gn6i-c4g1.xlarge");   // V100 GPU
        GPU_MODEL_MAPPING.put("T4", "ecs.gn6i-c4g1.xlarge");     // T4 GPU
    }

    /**
     * 将业务镜像标识映射为阿里云ImageId
     *
     * @param imageKey 业务镜像标识
     * @return 阿里云ImageId
     * @throws IllegalArgumentException 如果imageKey未找到映射
     */
    public String mapImageKeyToImageId(String imageKey) {
        if (imageKey == null || imageKey.trim().isEmpty()) {
            throw new IllegalArgumentException("镜像标识不能为空");
        }

        String imageId = IMAGE_KEY_MAPPING.get(imageKey.toLowerCase());
        if (imageId == null) {
            // 如果未找到映射，尝试使用默认镜像或直接使用imageKey（可能是完整的ImageId）
            log.warn("[AliyunParameterMapper] 未找到镜像映射: imageKey={}, 使用默认镜像", imageKey);
            return properties.getDefaultImageId() != null ? 
                   properties.getDefaultImageId() : imageKey;
        }

        log.debug("[AliyunParameterMapper] 镜像映射: imageKey={} -> imageId={}", imageKey, imageId);
        return imageId;
    }

    /**
     * 将GPU型号映射为阿里云实例类型
     *
     * @param gpuModel GPU型号（如 A100, V100, T4）
     * @return 阿里云实例类型
     * @throws IllegalArgumentException 如果gpuModel未找到映射
     */
    public String mapGpuModelToInstanceType(String gpuModel) {
        if (gpuModel == null || gpuModel.trim().isEmpty()) {
            return null;
        }

        String instanceType = GPU_MODEL_MAPPING.get(gpuModel.toUpperCase());
        if (instanceType == null) {
            log.warn("[AliyunParameterMapper] 未找到GPU型号映射: gpuModel={}, 使用默认实例类型", gpuModel);
            return properties.getDefaultInstanceType();
        }

        log.debug("[AliyunParameterMapper] GPU型号映射: gpuModel={} -> instanceType={}", gpuModel, instanceType);
        return instanceType;
    }

    /**
     * 解析实例类型（优先使用gpuModel映射，其次使用instanceType，最后使用默认值）
     */
    public String resolveInstanceType(String instanceType, String gpuModel) {
        // 1. 如果指定了instanceType，直接使用
        if (instanceType != null && !instanceType.trim().isEmpty()) {
            return instanceType;
        }

        // 2. 如果指定了gpuModel，映射为instanceType
        if (gpuModel != null && !gpuModel.trim().isEmpty()) {
            String mappedType = mapGpuModelToInstanceType(gpuModel);
            if (mappedType != null) {
                return mappedType;
            }
        }

        // 3. 使用默认实例类型
        return properties.getDefaultInstanceType();
    }

    /**
     * 解析镜像ID（优先使用imageKey映射，其次使用imageId，最后使用默认值）
     */
    public String resolveImageId(String imageId, String imageKey) {
        // 1. 如果指定了imageKey，映射为imageId
        if (imageKey != null && !imageKey.trim().isEmpty()) {
            return mapImageKeyToImageId(imageKey);
        }

        // 2. 如果指定了imageId，直接使用
        if (imageId != null && !imageId.trim().isEmpty()) {
            return imageId;
        }

        // 3. 使用默认镜像ID
        return properties.getDefaultImageId();
    }
}

