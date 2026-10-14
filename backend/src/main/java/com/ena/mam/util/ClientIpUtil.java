package com.ena.mam.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtil {

    // Private constructor to prevent instantiation of utility class
    private ClientIpUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}