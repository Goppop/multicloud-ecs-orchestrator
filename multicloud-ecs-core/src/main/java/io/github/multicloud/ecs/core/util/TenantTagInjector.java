package io.github.multicloud.ecs.core.util;

import io.github.multicloud.ecs.api.dto.CreateInstanceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户标签注入器
 * 自动向资源请求中注入租户标识，确保资源归属清晰
 *
 * @author guo
 */
@Slf4j
@Component
public class TenantTagInjector {

    /**
     * 租户标签Key
     */
    public static final String TENANT_TAG_KEY = "tenantId";

    /**
     * 用户标签Key（用于资源隔离）
     */
    public static final String USER_TAG_KEY = "Owner";

    /**
     * 创建来源标签Key
     */
    public static final String CREATED_BY_TAG_KEY = "createdBy";

    /**
     * 创建来源标签值
     */
    public static final String CREATED_BY_TAG_VALUE = "multicloud-ecs";

    /**
     * 向创建请求注入租户标签
     *
     * @param request 创建请求
     */
    public void inject(CreateInstanceRequest request) {
        if (request == null) {
            return;
        }

        // 获取或创建tags
        Map<String, String> tags = request.getTags();
        if (tags == null) {
            tags = new HashMap<>();
            request.setTags(tags);
        }

        // 注入租户标签
        String tenantId = request.getTenantId();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            String existingTenantId = tags.get(TENANT_TAG_KEY);
            if (existingTenantId == null || existingTenantId.isEmpty()) {
                tags.put(TENANT_TAG_KEY, tenantId);
                log.debug("注入租户标签: {}={}", TENANT_TAG_KEY, tenantId);
            } else if (!existingTenantId.equals(tenantId)) {
                // 如果已有tenantId但与请求中不一致，以请求为准并警告
                log.warn("租户标签冲突: 标签中的tenantId={}, 请求中的tenantId={}, 将使用请求中的值",
                        existingTenantId, tenantId);
                tags.put(TENANT_TAG_KEY, tenantId);
            }
        }

        // 注入用户标签（用于资源隔离，特别是VPC查找）
        String userId = request.getUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            String existingUserId = tags.get(USER_TAG_KEY);
            if (existingUserId == null || existingUserId.isEmpty()) {
                tags.put(USER_TAG_KEY, userId);
                log.debug("注入用户标签: {}={}", USER_TAG_KEY, userId);
            } else if (!existingUserId.equals(userId)) {
                log.warn("用户标签冲突: 标签中的userId={}, 请求中的userId={}, 将使用请求中的值",
                        existingUserId, userId);
                tags.put(USER_TAG_KEY, userId);
            }
        }

        // 注入创建来源标签
        if (!tags.containsKey(CREATED_BY_TAG_KEY)) {
            tags.put(CREATED_BY_TAG_KEY, CREATED_BY_TAG_VALUE);
        }
    }

    /**
     * 从tags中提取租户ID
     *
     * @param tags 标签
     * @return 租户ID，不存在返回null
     */
    public String extractTenantId(Map<String, String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.get(TENANT_TAG_KEY);
    }

    /**
     * 从tags中提取用户ID
     *
     * @param tags 标签
     * @return 用户ID，不存在返回null
     */
    public String extractUserId(Map<String, String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.get(USER_TAG_KEY);
    }

    /**
     * 判断资源是否由本系统创建
     *
     * @param tags 标签
     * @return 是否由本系统创建
     */
    public boolean isCreatedByUs(Map<String, String> tags) {
        if (tags == null) {
            return false;
        }
        return CREATED_BY_TAG_VALUE.equals(tags.get(CREATED_BY_TAG_KEY));
    }
}

