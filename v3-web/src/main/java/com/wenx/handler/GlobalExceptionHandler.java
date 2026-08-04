package com.wenx.handler;

import com.wenx.consts.OperationConst;
import com.wenx.v3core.error.BusinessException;
import com.wenx.v3core.error.ErrorCode;
import com.wenx.v3core.error.ServiceException;
import com.wenx.v3core.consts.SortConstant;
import com.wenx.v3core.response.R;
import com.wenx.v3secure.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.stream.Collectors;

/**
 * @author wenx
 * @date 2021-03-16
 */
@Slf4j
@ControllerAdvice
@Order(SortConstant.GLOBAL_ERROR_ORDER)
public class GlobalExceptionHandler {

    /**
     * 业务异常处理（D8：携带统一错误码）
     */
    @ResponseBody
    @ExceptionHandler(BusinessException.class)
    public R exceptionHandler(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, msg={}", e.getErrorCode().getCode(), e.getMessage());
        return R.failed(e.getErrorCode(), e.getMessage());
    }

    /**
     * 服务异常处理（D8：归为系统错误码）
     */
    @ResponseBody
    @ExceptionHandler(ServiceException.class)
    public R exceptionHandler(ServiceException e, HttpServletResponse response) {
        log.warn("服务异常: {}", e.getMessage());
        return R.failed(ErrorCode.SYSTEM_ERROR, e.getMessage());
    }

    /**
     * 认证异常处理（D8：AUTH 分区）
     */
    @ResponseBody
    @ExceptionHandler(AuthenticationException.class)
    public R exceptionHandler(AuthenticationException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (e instanceof BadCredentialsException) {
            return R.failed(ErrorCode.AUTH_BAD_CREDENTIALS);
        } else if (e instanceof InsufficientAuthenticationException) {
            return R.failed(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return R.failed(ErrorCode.AUTH_UNAUTHORIZED, "认证失败: " + e.getMessage());
    }

    /**
     * 权限不足异常处理（D8：PERMISSION 分区）
     */
    @ResponseBody
    @ExceptionHandler(AccessDeniedException.class)
    public R exceptionHandler(AccessDeniedException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return R.failed(ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 框架权限校验异常（SecurityAspect @RequiresPermissions/@RequiresRoles，HTTP 403）
     */
    @ResponseBody
    @ExceptionHandler(UnauthorizedException.class)
    public R exceptionHandler(UnauthorizedException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return R.failed(ErrorCode.PERMISSION_DENIED, e.getMessage());
    }

    /**
     * 数据库操作异常处理（D8：DUPLICATE/DATA_INTEGRITY/DB_ERROR）
     */
    @ResponseBody
    @ExceptionHandler(DataAccessException.class)
    public R exceptionHandler(DataAccessException e, HttpServletResponse response) {
        log.error("数据库操作异常:", e);
        if (e instanceof DuplicateKeyException) {
            return R.failed(ErrorCode.DUPLICATE_KEY);
        } else if (e instanceof DataIntegrityViolationException) {
            return R.failed(ErrorCode.DATA_INTEGRITY);
        }
        return R.failed(ErrorCode.DB_ERROR);
    }

    /**
     * 参数缺失异常处理（D8：PARAM 分区）
     */
    @ResponseBody
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R exceptionHandler(MissingServletRequestParameterException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String msg = String.format("缺少必要参数: %s", e.getParameterName());
        return R.failed(ErrorCode.PARAM_MISSING, msg);
    }

    /**
     * 参数类型不匹配异常（D8：PARAM_TYPE）
     */
    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R exceptionHandler(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String msg = String.format("参数类型错误: %s 应该为 %s 类型", 
                e.getName(), e.getRequiredType().getSimpleName());
        return R.failed(ErrorCode.PARAM_TYPE, msg);
    }

    @ResponseBody
    @ExceptionHandler(UndeclaredThrowableException.class)
    public R exceptionHandler(UndeclaredThrowableException e, HttpServletResponse response) {
        log.error("未声明的异常:", e);
        Throwable ex = e.getUndeclaredThrowable();
        if (ex != null && ex.getCause() != null) {
            return R.failed(ErrorCode.SYSTEM_ERROR, ex.getCause().getMessage());
        }
        return R.failed(ErrorCode.SYSTEM_ERROR, OperationConst.SYSTEM_ERROR);
    }

    @ResponseBody
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R exceptionHandler(HttpRequestMethodNotSupportedException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String msg = String.format("不支持%s请求方法，请使用%s", 
                e.getMethod(), String.join("/", e.getSupportedMethods()));
        return R.failed(ErrorCode.PARAM_ERROR, msg);
    }

    @ResponseBody
    @ExceptionHandler(TypeMismatchException.class)
    public R exceptionHandler(TypeMismatchException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String msg = String.format("%s，异常原因：%s", OperationConst.ERROR_PARAM, e.getLocalizedMessage());
        return R.failed(ErrorCode.PARAM_TYPE, msg);
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R exceptionHandler(HttpMessageNotReadableException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        log.warn("JSON解析异常: {}", e.getMessage());
        return R.failed(ErrorCode.PARAM_FORMAT);
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R exceptionHandler(MethodArgumentNotValidException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.failed(ErrorCode.PARAM_VALIDATION, "参数验证失败: " + message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R exceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.failed(ErrorCode.PARAM_VALIDATION, "参数约束验证失败: " + message);
    }

    /**
     * 空指针异常处理（D8：SYSTEM_ERROR）
     */
    @ResponseBody
    @ExceptionHandler(NullPointerException.class)
    public R exceptionHandler(NullPointerException e, HttpServletResponse response) {
        log.error("空指针异常:", e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return R.failed(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 栈溢出异常处理（D8：SYSTEM_ERROR）
     */
    @ResponseBody
    @ExceptionHandler(StackOverflowError.class)
    public R exceptionHandler(StackOverflowError e, HttpServletResponse response) {
        log.error("栈溢出错误:", e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return R.failed(ErrorCode.SYSTEM_ERROR, "系统处理异常，请联系管理员");
    }
}