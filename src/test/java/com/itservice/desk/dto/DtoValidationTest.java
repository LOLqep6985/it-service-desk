package com.itservice.desk.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 参数校验回归测试（待办一）。
 *
 * <h2>修复的缺陷</h2>
 * 修复前 DTO 无任何校验注解，实测 {@code POST /api/reports/anonymous}
 * 传入空的 {@code reporterName} 与 {@code deviceName} 仍返回 code=0 并落库空串：
 * <pre>
 * {"code":0,...,"reporterName":"","deviceName":"","problemDesc":""}
 * </pre>
 * 根因：数据库只有 {@code NOT NULL} 约束，而空串不等于 NULL，拦不住；
 * Controller 也没有 {@code @Valid}。
 *
 * <h2>本测试验证</h2>
 * 直接使用 JSR-380 Validator 校验 DTO，断言"必填项为空必须产生约束违规"。
 * 不启动 Spring 容器，速度快且能精确定位到字段。
 */
class DtoValidationTest {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    private static AnonymousReportRequest validReport() {
        AnonymousReportRequest r = new AnonymousReportRequest();
        r.setDeptToken("some-token");
        r.setReporterName("张三");
        r.setDeviceName("打印机");
        r.setProblemDesc("无法连接");
        return r;
    }

    private static boolean hasViolationOn(Object target, String field) {
        Set<? extends ConstraintViolation<?>> vs = VALIDATOR.validate(target);
        return vs.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    @DisplayName("基线：合法报修请求应无任何校验错误")
    void validReportPasses() {
        assertTrue(VALIDATOR.validate(validReport()).isEmpty(),
                "完整填写的请求不应被拦，实际=" + VALIDATOR.validate(validReport()));
    }

    @Test
    @DisplayName("核心回归：reporterName 为空串必须被拒（修复前会落库空串）")
    void blankReporterNameRejected() {
        AnonymousReportRequest r = validReport();
        r.setReporterName("");
        assertTrue(hasViolationOn(r, "reporterName"), "空串必须触发约束违规");

        r.setReporterName("   ");
        assertTrue(hasViolationOn(r, "reporterName"), "纯空白也必须触发（NotBlank 而非 NotNull）");

        r.setReporterName(null);
        assertTrue(hasViolationOn(r, "reporterName"), "null 必须触发");
    }

    @Test
    @DisplayName("核心回归：deviceName 为空串必须被拒（修复前会落库空串）")
    void blankDeviceNameRejected() {
        AnonymousReportRequest r = validReport();
        r.setDeviceName("");
        assertTrue(hasViolationOn(r, "deviceName"));

        r.setDeviceName("  ");
        assertTrue(hasViolationOn(r, "deviceName"));
    }

    @Test
    @DisplayName("边界值：超长字段被拒，恰好等于上限则通过")
    void lengthBoundaries() {
        AnonymousReportRequest r = validReport();

        r.setReporterName("a".repeat(50));
        assertFalse(hasViolationOn(r, "reporterName"), "50 字符应通过（等于上限）");

        r.setReporterName("a".repeat(51));
        assertTrue(hasViolationOn(r, "reporterName"), "51 字符必须被拒");

        r = validReport();
        r.setDeviceName("b".repeat(100));
        assertFalse(hasViolationOn(r, "deviceName"), "100 字符应通过");

        r.setDeviceName("b".repeat(101));
        assertTrue(hasViolationOn(r, "deviceName"), "101 字符必须被拒");
    }

    @Test
    @DisplayName("可选字段：problemDesc 允许为空，但超长被拒")
    void optionalFieldRules() {
        AnonymousReportRequest r = validReport();
        r.setProblemDesc(null);
        assertTrue(VALIDATOR.validate(r).isEmpty(), "问题描述允许不填");

        r.setProblemDesc("c".repeat(501));
        assertTrue(hasViolationOn(r, "problemDesc"), "超过 500 字符必须被拒");
    }

    @Test
    @DisplayName("签字校验：signerName 为空必须被拒（否则 signed_by 写空，无法追责）")
    void blankSignerNameRejected() {
        SignRequest s = new SignRequest();
        s.setDeptToken("t");
        s.setSignerName("");
        assertTrue(hasViolationOn(s, "signerName"));

        s.setSignerName(null);
        assertTrue(hasViolationOn(s, "signerName"));

        s.setSignerName("护士长王五");
        assertTrue(VALIDATOR.validate(s).isEmpty(), "合法签字人应通过");
    }

    @Test
    @DisplayName("校验消息可读：错误文案应能直接展示给用户")
    void messagesAreUserReadable() {
        AnonymousReportRequest r = validReport();
        r.setReporterName("");
        Set<ConstraintViolation<AnonymousReportRequest>> vs = VALIDATOR.validate(r);

        assertEquals(1, vs.size());
        String msg = vs.iterator().next().getMessage();
        assertEquals("报修人姓名不能为空", msg);
    }
}
