package com.omar.gatekeeper.interceptor;

import com.omar.gatekeeper.service.RateLimitingService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.stream.Collectors;
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${gatekeeper.whitelist.ips:}")
    private Set<String> whitelistedIps;

    private final RateLimitingService rateLimitingService;

    public RateLimitInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIP(request);


        if(whitelistedIps.contains(clientIp)) {
            return true;
        }

        if (rateLimitingService.isRequestAllowed(clientIp)) {
            log.debug("Allowed request from IP: " + clientIp);
            return true;
        }


        log.warn("BLOCKED request from IP: {} trying to access: {} {}", clientIp, request.getMethod(), request.getRequestURI());        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429 Code
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}");

        return false;
    }

    private String getClientIP(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isEmpty()){
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostConstruct
    private void trimWhitelistedIps() {
        if(whitelistedIps != null) {
            whitelistedIps = whitelistedIps.stream().map(String::trim).collect(Collectors.toSet());
        }
    }
}