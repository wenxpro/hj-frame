package com.wenx.handler;

import com.wenx.consts.OperationConst;
import com.wenx.v3core.error.BusinessException;
import com.wenx.v3core.error.ServiceException;
import com.wenx.v3core.consts.SortConstant;
import com.wenx.v3core.response.R;
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
     * 业务异常处理
     */
    @ResponseBody
    @ExceptionHandler(BusinessException.class)
    public R exceptionHandler(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        return R.failed(e.getMessage());
    }

    /**
     * 服务异常处理
     */
    @ResponseBody
    @ExceptionHandler(ServiceException.class)
    public R exceptionHandler(ServiceException e, HttpServletResponse response) {
        log.warn("服务异常: {}", e.getMessage());
        return R.failed(e.getMessage());
    }

    /**
     * 认证异常处理
     */
    @ResponseBody
    @ExceptionHandler(AuthenticationException.class)
    public R exceptionHandler(AuthenticationException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (e instanceof BadCredentialsException) {
            return R.failed("用户名或密码错误");
        } else if (e instanceof InsufficientAuthenticationException) {
            return R.failed("请先登录");
        }
        return R.failed("认证失败: " + e.getMessage());
    }

    /**
     * 权限不足异常处理
     */
    @ResponseBody
    @ExceptionHandler(AccessDeniedException.class)
    public R exceptionHandler(AccessDeniedException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return R.failed("权限不足，无法访问该资源");
    }

    /**
     * 数据库操作异常处理
     */
    @ResponseBody
    @ExceptionHandler(DataAccessException.class)
    public R exceptionHandler(DataAccessException e, HttpServletResponse response) {
        log.error("数据库操作异常:", e);
        if (e instanceof DuplicateKeyException) {
            return R.failed("数据已存在，请勿重复添加");
        } else if (e instanceof DataIntegrityViolationException) {
            return R.failed("数据完整性约束违反，请检查数据是否正确");
        }
        return R.failed("数据操作失败");
    }

    /**
     * 参数缺失异常处理
     */
    @ResponseBody
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R exceptionHandler(MissingServletRequestParameterException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String msg = String.format("缺少必要参数: %s", e.getParameterName());
        return R.failed(msg);
    }

    /**
     * 参数类型不匹配异常
     */
    @ResponseBody
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R exceptionHandler(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String msg = String.format("参数类型错误: %s 应该为 %s 类型", 
                e.getName(), e.getRequiredType().getSimpleName());
        return R.failed(msg);
    }

    @ResponseBody
    @ExceptionHandler(UndeclaredThrowableException.class)
    public R exceptionHandler(UndeclaredThrowableException e, HttpServletResponse response) {
        log.error("未声明的异常:", e);
        Throwable ex = e.getUndeclaredThrowable();
        if (ex != null && ex.getCause() != null) {
            return R.failed(ex.getCause().getMessage());
        }
        return R.failed(OperationConst.SYSTEM_ERROR);
    }

    @ResponseBody
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R exceptionHandler(HttpRequestMethodNotSupportedException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String msg = String.format("不支持%s请求方法，请使用%s", 
                e.getMethod(), String.join("/", e.getSupportedMethods()));
        return R.failed(msg);
    }

    @ResponseBody
    @ExceptionHandler(TypeMismatchException.class)
    public R exceptionHandler(TypeMismatchException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String msg = String.format("%s，异常原因：%s", OperationConst.ERROR_PARAM, e.getLocalizedMessage());
        return R.failed(msg);
    }

    @ResponseBody
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R exceptionHandler(HttpMessageNotReadableException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        log.warn("JSON解析异常: {}", e.getMessage());
        return R.failed("请求数据格式错误，请检查JSON格式是否正确");
    }

    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R exceptionHandler(MethodArgumentNotValidException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.failed("参数验证失败: " + message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R exceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.failed("参数约束验证失败: " + message);
    }

    /**
     * 空指针异常处理
     */
    @ResponseBody
    @ExceptionHandler(NullPointerException.class)
    public R exceptionHandler(NullPointerException e, HttpServletResponse response) {
        log.error("空指针异常:", e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return R.failed("系统内部错误，请稍后再试");
    }

    /**
     * 栈溢出异常处理
     */
    @ResponseBody
    @ExceptionHandler(StackOverflowError.class)
    public R exceptionHandler(StackOverflowError e, HttpServletResponse response) {
        log.error("栈溢出错误:", e);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return R.failed("系统处理异常，请联系管理员");
    }
}