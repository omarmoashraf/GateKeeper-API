package com.omar.gatekeeper.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitingService {
    private record Window(long startTime, int count) {}
    private final ConcurrentHashMap<String , Window> requestMap = new ConcurrentHashMap<>();
    @Value("${rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${rate-limit.window-size-ms:60000}")
    private long windowSizeMs;



    public boolean isRequestAllowed(String ip) {
        long now = System.currentTimeMillis();

        Window currentWindow = requestMap.compute(ip, (key, existingWindow) -> {
            if (existingWindow == null || (now - existingWindow.startTime()) > windowSizeMs) {
                return new Window(now, 1);
            }
            return new Window(existingWindow.startTime(), existingWindow.count() + 1);
        });

        return currentWindow.count() <= maxRequests;
    }



    }

