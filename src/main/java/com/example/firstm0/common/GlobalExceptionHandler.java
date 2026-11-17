package com.example.firstm0.common;

import com.example.firstm0.auth.TokenInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>@RestControllerAdvice 的作用：把“所有 Controller 抛出的异常”集中到一处处理，
 * 而不是在每个接口里各写一遍 try/catch。这样错误响应格式统一，业务代码也更干净。
 *
 * <p>处理两类问题：
 * <ul>
 *   <li>参数校验失败（@Valid 触发）→ 400 + 具体是哪个字段错了</li>
 *   <li>请求体不是合法 JSON → 400 + 提示格式错误</li>
 * </ul>
 *
 * <p>注意：这里只返回“能安全给用户看”的信息，绝不返回堆栈、类名、SQL 等内部细节。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @Valid 校验失败，例如 message 为空或超过 2000 字。
     *
     * <p>@ResponseStatus 把 HTTP 状态码设为 400（Bad Request）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        // 把所有字段的错误提示拼成一句话，例如“消息不能为空”
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        // 记日志时带上“哪些字段错了”，但不要把异常堆栈直接丢给前端
        log.warn("参数校验失败: {}", message);

        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    /**
     * 处理请求体不是合法 JSON 的情况，例如 body 为空或少了引号。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败: {}", exception.getMessage());

        return new ErrorResponse("INVALID_REQUEST_BODY", "请求体格式不正确，应为 JSON");
    }

    /**
     * 处理会话不存在：请求里带了 conversationId，但数据库查不到。
     *
     * <p>这是“客户端给错了资源 id”，属于 404（Not Found），不是服务端故障，
     * 所以不要返回 500。
     */
    @ExceptionHandler(ConversationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleConversationNotFound(ConversationNotFoundException exception) {
        log.warn("会话不存在: {}", exception.getMessage());

        return new ErrorResponse("CONVERSATION_NOT_FOUND", exception.getMessage());
    }

    /**
     * 用户名已被占用：409（Conflict）。
     */
    @ExceptionHandler(UsernameTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUsernameTaken(UsernameTakenException exception) {
        log.info("注册失败，用户名重复: {}", exception.getMessage());

        return new ErrorResponse("USERNAME_TAKEN", exception.getMessage());
    }

    /**
     * 登录失败：401（Unauthorized）。
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException exception) {
        log.info("登录失败: {}", exception.getMessage());

        return new ErrorResponse("INVALID_CREDENTIALS", exception.getMessage());
    }

    /**
     * token 无效或过期：401，前端收到后应清掉本地 token 并跳回登录页。
     */
    @ExceptionHandler(TokenInvalidException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleTokenInvalid(TokenInvalidException exception) {
        log.debug("token 校验失败: {}", exception.getMessage());

        return new ErrorResponse("TOKEN_INVALID", exception.getMessage());
    }
}
