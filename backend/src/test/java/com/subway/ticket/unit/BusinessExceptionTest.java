package com.subway.ticket.unit;

import com.subway.ticket.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("业务异常单元测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("测试异常消息构造 - 单参数")
    void testBusinessException_WithMessage_Success() {
        String message = "站点不存在";

        BusinessException exception = new BusinessException(message);

        assertEquals(message, exception.getMessage());
        assertEquals("BUSINESS_ERROR", exception.getCode());
    }

    @Test
    @DisplayName("测试异常消息构造 - 双参数")
    void testBusinessException_WithCodeAndMessage_Success() {
        String code = "STATION_NOT_FOUND";
        String message = "站点不存在";

        BusinessException exception = new BusinessException(code, message);

        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("测试异常类型")
    void testBusinessException_IsRuntimeException() {
        BusinessException exception = new BusinessException("测试");

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("测试异常可以抛出和捕获")
    void testBusinessException_CanBeThrownAndCaught() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException("测试异常");
        });
    }


}
