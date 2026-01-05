package io.github.multicloud.ecs.api.exception;

import lombok.Getter;

/**
 * ECS操作统一异常类
 * 封装各云厂商的异常，提供统一的异常处理
 *
 * @author guo
 */
@Getter
public class EcsException extends RuntimeException {

    /**
     * 云厂商代码
     */
    private final String providerCode;

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 云厂商原始错误信息
     */
    private final String cloudErrorMessage;

    /**
     * 请求ID（用于追踪）
     */
    private final String requestId;

    public EcsException(String message) {
        super(message);
        this.providerCode = null;
        this.errorCode = null;
        this.cloudErrorMessage = null;
        this.requestId = null;
    }

    public EcsException(String message, Throwable cause) {
        super(message, cause);
        this.providerCode = null;
        this.errorCode = null;
        this.cloudErrorMessage = null;
        this.requestId = null;
    }

    public EcsException(String providerCode, String errorCode, String message) {
        super(message);
        this.providerCode = providerCode;
        this.errorCode = errorCode;
        this.cloudErrorMessage = message;
        this.requestId = null;
    }

    public EcsException(String providerCode, String errorCode, String message, String requestId) {
        super(message);
        this.providerCode = providerCode;
        this.errorCode = errorCode;
        this.cloudErrorMessage = message;
        this.requestId = requestId;
    }

    public EcsException(String providerCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.providerCode = providerCode;
        this.errorCode = errorCode;
        this.cloudErrorMessage = message;
        this.requestId = null;
    }

    public EcsException(String providerCode, String errorCode, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.providerCode = providerCode;
        this.errorCode = errorCode;
        this.cloudErrorMessage = message;
        this.requestId = requestId;
    }

    /**
     * 创建一个通用的ECS异常
     */
    public static EcsException of(String message) {
        return new EcsException(message);
    }

    /**
     * 创建一个带云厂商信息的ECS异常
     */
    public static EcsException of(String providerCode, String errorCode, String message) {
        return new EcsException(providerCode, errorCode, message);
    }

    /**
     * 创建一个带请求ID的ECS异常
     */
    public static EcsException of(String providerCode, String errorCode, String message, String requestId) {
        return new EcsException(providerCode, errorCode, message, requestId);
    }

    /**
     * 判断是否为配额错误
     */
    public boolean isQuotaExceeded() {
        return "QUOTA_EXCEEDED".equals(errorCode) || 
               (cloudErrorMessage != null && cloudErrorMessage.contains("QuotaExceeded"));
    }

    /**
     * 判断是否为网络资源配额错误
     */
    public boolean isNetworkQuotaExceeded() {
        return isQuotaExceeded() && 
               (cloudErrorMessage != null && 
                (cloudErrorMessage.contains("Vpc") || 
                 cloudErrorMessage.contains("VSwitch") || 
                 cloudErrorMessage.contains("SecurityGroup")));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EcsException{");
        if (providerCode != null) {
            sb.append("provider='").append(providerCode).append("', ");
        }
        if (errorCode != null) {
            sb.append("errorCode='").append(errorCode).append("', ");
        }
        if (requestId != null) {
            sb.append("requestId='").append(requestId).append("', ");
        }
        sb.append("message='").append(getMessage()).append("'");
        sb.append("}");
        return sb.toString();
    }
}

