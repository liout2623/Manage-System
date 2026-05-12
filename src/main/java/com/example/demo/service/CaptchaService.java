package com.example.demo.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.IoUtil;

import com.example.demo.dto.CaptchaResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务：生成、存储、校验
 * <p>
 * 存储选型：内存（ConcurrentHashMap），配合定时任务清理过期条目。
 * 验证码有效期为 5 分钟，使用后立即删除防止重用。
 */
@org.springframework.stereotype.Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    /** 验证码有效期：5 分钟 */
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;

    /** captchaId -> CaptchaEntry */
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    /** 定时清理过期验证码 */
    private ScheduledExecutorService cleanupScheduler;

    @PostConstruct
    public void init() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "captcha-cleanup");
            t.setDaemon(true);
            return t;
        });
        // 每 60 秒清理一次过期验证码
        cleanupScheduler.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdownNow();
        }
    }

    /**
     * 生成验证码，返回 captchaId 和 Base64 图片
     */
    public CaptchaResponse generate() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 6);
        String code = captcha.getCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 将图片转为 Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImgUtil.writePng(captcha.getImage(), baos);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        IoUtil.close(baos);

        captchaStore.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis()));
        log.debug("验证码已生成: captchaId={}, code={}", captchaId, code);
        return new CaptchaResponse(captchaId, base64);
    }

    /**
     * 校验验证码，校验后立即删除（一次性使用）
     *
     * @param captchaId   验证码唯一标识
     * @param captchaCode 用户输入的验证码文本
     * @throws ResponseStatusException 校验失败时抛出
     */
    public void validate(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码标识不能为空");
        }
        if (captchaCode == null || captchaCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不能为空");
        }

        CaptchaEntry entry = captchaStore.remove(captchaId);
        if (entry == null) {
            // captchaId 不存在或已被使用
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已失效");
        }

        // 检查是否过期
        if (System.currentTimeMillis() - entry.createdAt > CAPTCHA_TTL_MS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已失效");
        }

        // 忽略大小写比较
        if (!entry.code.equalsIgnoreCase(captchaCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误");
        }
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(e -> now - e.getValue().createdAt > CAPTCHA_TTL_MS);
    }

    /** 内部存储结构 */
    private record CaptchaEntry(String code, long createdAt) {}
}
