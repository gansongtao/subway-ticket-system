package com.subway.ticket.unit.service;

import com.subway.ticket.service.QrSignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QR签名服务单元测试")
class QrSignServiceTest {

    private QrSignService qrSignService;

    @BeforeEach
    void setUp() {
        qrSignService = new QrSignService("test-secret-key");
    }

    @Test
    @DisplayName("sign - 正常输入应返回非空签名")
    void sign_normalInput_returnsNonEmptySignature() {
        String result = qrSignService.sign("orderId=123&amount=5.0");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("sign - 空字符串输入应正常返回")
    void sign_emptyString_returnsSignature() {
        String result = qrSignService.sign("");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("sign - 相同输入两次签名应一致")
    void sign_sameInputTwice_returnsIdenticalSignatures() {
        String payload = "orderId=123&amount=5.0";
        String sig1 = qrSignService.sign(payload);
        String sig2 = qrSignService.sign(payload);
        assertEquals(sig1, sig2);
    }

    @Test
    @DisplayName("sign - 不同输入应产生不同签名")
    void sign_differentInputs_produceDifferentSignatures() {
        String sig1 = qrSignService.sign("abc");
        String sig2 = qrSignService.sign("abd");
        assertNotEquals(sig1, sig2);
    }

    @Test
    @DisplayName("sign - 中文输入应正常签名")
    void sign_chineseInput_returnsValidSignature() {
        String result = qrSignService.sign("站点=长春站");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("sign - 含特殊符号输入应正常签名")
    void sign_specialCharacters_returnsValidSignature() {
        String result = qrSignService.sign("a=1&b=2&c=3");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Base64 URL-safe 编码不应包含 '+' 或 '/'
        assertFalse(result.contains("+"));
        assertFalse(result.contains("/"));
    }

    @Test
    @DisplayName("verify - 签名匹配应返回true")
    void verify_matchingSignature_returnsTrue() {
        String data = "orderId=123:nonce=abc:exp=9999999999";
        String sign = qrSignService.sign(data);
        assertTrue(qrSignService.verify(data, sign));
    }

    @Test
    @DisplayName("verify - 签名不匹配应返回false")
    void verify_nonMatchingSignature_returnsFalse() {
        String data = "orderId=123:nonce=abc:exp=9999999999";
        String wrongSign = qrSignService.sign("different-data");
        assertFalse(qrSignService.verify(data, wrongSign));
    }

    @Test
    @DisplayName("verify - 数据被篡改应返回false")
    void verify_tamperedData_returnsFalse() {
        String originalData = "orderId=123:nonce=abc:exp=9999999999";
        String sign = qrSignService.sign(originalData);
        String tamperedData = "orderId=124:nonce=abc:exp=9999999999";
        assertFalse(qrSignService.verify(tamperedData, sign));
    }

    @Test
    @DisplayName("verify - 签名格式错误应返回false不抛异常")
    void verify_malformedSignature_returnsFalse() {
        assertFalse(qrSignService.verify("data", "!!!not-a-valid-signature!!!"));
    }

    @Test
    @DisplayName("verify - null签名应返回false")
    void verify_nullSignature_returnsFalse() {
        assertFalse(qrSignService.verify("data", null));
    }

    @Test
    @DisplayName("sign - 不同密钥实例产生不同签名")
    void sign_differentSecretKeys_produceDifferentSignatures() {
        QrSignService otherService = new QrSignService("different-secret");
        String payload = "orderId=123";
        String sig1 = qrSignService.sign(payload);
        String sig2 = otherService.sign(payload);
        assertNotEquals(sig1, sig2);
    }
}
