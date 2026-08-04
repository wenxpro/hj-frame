package com.wenx.v3core.error;

import com.wenx.v3core.response.IErrorCode;
import lombok.Getter;

/**
 * 统一错误码（D8）
 *
 * <p>分区编码规则：{分区段号}{序号}，段号 * 1000 + 序号
 * <pre>
 * 0    成功
 * 1xxx 认证（AUTH）
 * 2xxx 参数（PARAM）
 * 3xxx 权限（PERMISSION）
 * 4xxx 业务（BUSINESS）
 * 5xxx 系统（SYSTEM）
 * </pre>
 * -1 为历史兼容的通用失败码（无分区，仅老代码/兜底使用）。
 *
 * @author wenx
 */
@Getter
public enum ErrorCode implements IErrorCode {

    /**
     * 成功（兼容旧值）
     */
    SUCCESS(0L, "执行成功"),

    // ============ 认证 AUTH（1xxx） ============
    AUTH_BAD_CREDENTIALS(1001L, "用户名或密码错误"),
    AUTH_UNAUTHORIZED(1002L, "请先登录"),
    AUTH_TOKEN_EXPIRED(1003L, "登录已过期，请重新登录"),
    AUTH_TOKEN_INVALID(1004L, "登录凭证无效"),
    AUTH_REFRESH_FAILED(1005L, "刷新凭证失败，请重新登录"),

    // ============ 参数 PARAM（2xxx） ============
    PARAM_ERROR(2001L, "请求参数错误"),
    PARAM_MISSING(2002L, "缺少必要参数"),
    PARAM_TYPE(2003L, "参数类型错误"),
    PARAM_VALIDATION(2004L, "参数校验失败"),
    PARAM_FORMAT(2005L, "请求数据格式错误"),

    // ============ 权限 PERMISSION（3xxx） ============
    PERMISSION_DENIED(3001L, "权限不足，无法访问该资源"),
    PERMISSION_FORBIDDEN(3002L, "该操作被禁止"),

    // ============ 业务 BUSINESS（4xxx） ============
    BUSINESS_ERROR(4000L, "操作失败"),
    DUPLICATE_KEY(4001L, "数据已存在，请勿重复添加"),
    DATA_INTEGRITY(4002L, "数据完整性约束违反，请检查数据是否正确"),
    NOT_FOUND(4003L, "数据不存在"),
    DATA_PERMISSION_DENIED(4004L, "数据权限校验未通过"),

    // ============ 系统 SYSTEM（5xxx） ============
    SYSTEM_ERROR(5000L, "系统内部错误，请稍后再试"),
    DB_ERROR(5001L, "数据操作失败"),
    SERVICE_UNAVAILABLE(5002L, "服务暂不可用，请稍后再试"),

    /**
     * 历史兼容：通用失败（-1），新代码请使用分区错误码
     */
    FAILED(-1L, "操作失败");

    private final long code;
    private final String msg;

    ErrorCode(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ErrorCode fromCode(long code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return FAILED;
    }
}
