package com.subway.ticket.unit.web;

import com.subway.ticket.exception.BusinessException;
import com.subway.ticket.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("全局异常处理器单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleBusiness - 带code的BusinessException应返回400含错误码")
    void handleBusiness_withCode_returns400WithCodeAndMessage() {
        BusinessException ex = new BusinessException("CUSTOM_ERROR", "自定义业务错误");

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CUSTOM_ERROR", response.getBody().code);
        assertEquals("自定义业务错误", response.getBody().message);
    }

    @Test
    @DisplayName("handleBusiness - 仅有消息的BusinessException应返回400含默认错误码")
    void handleBusiness_messageOnly_returns400WithDefaultCode() {
        BusinessException ex = new BusinessException("业务处理失败");

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BUSINESS_ERROR", response.getBody().code);
        assertEquals("业务处理失败", response.getBody().message);
    }

    @Test
    @DisplayName("handleValidation - 单字段校验错误应返回400")
    void handleValidation_singleFieldError_returns400WithFieldMessage() {
        // 构造 MethodArgumentNotValidException
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "createOrderReq");
        bindingResult.addError(new FieldError("createOrderReq", "from", "出发站不能为空"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().code);
        assertEquals("出发站不能为空", response.getBody().message);
    }

    @Test
    @DisplayName("handleValidation - 多字段校验错误应返回400含所有错误")
    void handleValidation_multipleFieldErrors_returns400WithAllMessages() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "createOrderReq");
        bindingResult.addError(new FieldError("createOrderReq", "from", "出发站不能为空"));
        bindingResult.addError(new FieldError("createOrderReq", "to", "到达站不能为空"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().code);
        // 消息应包含两个字段的错误
        assertTrue(response.getBody().message.contains("出发站不能为空"));
        assertTrue(response.getBody().message.contains("到达站不能为空"));
    }

    @Test
    @DisplayName("handle - 通用Exception应返回500含内部错误")
    void handle_genericException_returns500WithInternalError() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handle(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().code);
        assertEquals("系统内部错误", response.getBody().message);
        // 不应泄露原始异常消息
        assertNotEquals("Unexpected error", response.getBody().message);
    }

    @Test
    @DisplayName("handle - NullPointerException应返回500不泄露堆栈")
    void handle_nullPointerException_returns500WithoutStackInfo() {
        NullPointerException ex = new NullPointerException("null pointer detail");

        ResponseEntity<GlobalExceptionHandler.ErrorBody> response = handler.handle(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().code);
        // 不应泄露内部细节
        assertFalse(response.getBody().message.contains("NullPointerException"));
        assertFalse(response.getBody().message.contains("null pointer"));
    }
}
