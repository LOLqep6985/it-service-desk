package com.itservice.desk.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itservice.desk.mapper.UserMapper;
import com.itservice.desk.model.User;
import com.itservice.desk.model.UserRole;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * 入口二维码（管理后台「系统信息」页）。
 * 三端入口地址由请求的 Host 动态拼出（同机部署，扫码即达），
 * 生成 PNG 返回，前端直接 <img> 引用；需管理员/调度员登录。
 */
@RestController
public class QrController {

    private static final int SIZE = 240;

    private final UserMapper userMapper;

    public QrController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping(value = "/api/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@RequestParam("path") String path,
                     @RequestHeader(value = "X-Token", required = false) String token,
                     HttpServletRequest request) throws Exception {
        User user = resolveUser(token);
        if (user == null || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.DISPATCHER)) {
            // 未授权不给码：返回 1x1 透明图占位，避免 <img> 挂死（真实语义由前端 401 提示兜底）
            return new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A,
                    (byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D, (byte) 0x49,
                    (byte) 0x48, (byte) 0x44, (byte) 0x52, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x08, (byte) 0x06, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x15, (byte) 0xC4, (byte) 0x89, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x0A, (byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54,
                    (byte) 0x78, (byte) 0x9C, (byte) 0x63, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                    (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x0D, (byte) 0x0A, (byte) 0x2D, (byte) 0xB4,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E,
                    (byte) 0x44, (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82};
        }

        // 白名单：只允许三个前端入口，防止把任意 URL 变成扫码钓鱼跳板
        String safePath;
        switch (path) {
            case "/report.html": safePath = "/report.html"; break;
            case "/today.html": safePath = "/today.html"; break;
            case "/login.html": safePath = "/login.html"; break;
            default: safePath = "/login.html";
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String url = scheme + "://" + host + (port == 80 || port == 443 ? "" : ":" + port) + safePath;

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, SIZE, SIZE, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private User resolveUser(String token) {
        if (token == null || !token.startsWith("token-")) {
            return null;
        }
        try {
            Long userId = Long.valueOf(token.substring("token-".length()));
            return userMapper.selectById(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
