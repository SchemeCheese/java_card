package config;

/**
 * API Configuration - Centralized API URL management
 * 
 * Để thay đổi server URL:
 * 1. Sửa DEFAULT_API_URL trong file này
 * 2. Hoặc set environment variable: API_BASE_URL
 * 3. Hoặc set system property: -DAPI_BASE_URL=http://your-domain.com
 */
public class ApiConfig {
    
    /**
     * Default API URL - THAY ĐỔI URL SERVER TẠI ĐÂY
     */
    private static final String DEFAULT_API_URL = "https://467a6ba3bc82.ngrok-free.app";
    
    /**
     * Ngrok URL (for development/testing)
     * Uncomment và sử dụng khi cần test với ngrok
     */
    // private static final String DEFAULT_API_URL = "https://467a6ba3bc82.ngrok-free.app";
    
    /**
     * Production URL
     * Uncomment khi deploy production
     */
    // private static final String DEFAULT_API_URL = "https://api.yourdomain.com";
    
    /**
     * Get API base URL
     * Priority: System Property > Environment Variable > Default
     */
    public static String getApiBaseUrl() {
        // 1. Check system property (-DAPI_BASE_URL=...)
        String systemProperty = System.getProperty("API_BASE_URL");
        if (systemProperty != null && !systemProperty.isEmpty()) {
            System.out.println("[ApiConfig] Using API URL from system property: " + systemProperty);
            return systemProperty;
        }
        
        // 2. Check environment variable
        String envVariable = System.getenv("API_BASE_URL");
        if (envVariable != null && !envVariable.isEmpty()) {
            System.out.println("[ApiConfig] Using API URL from environment: " + envVariable);
            return envVariable;
        }
        
        // 3. Use default
        System.out.println("[ApiConfig] Using default API URL: " + DEFAULT_API_URL);
        return DEFAULT_API_URL;
    }
    
    /**
     * Get full endpoint URL
     */
    public static String getPaymentEndpoint() {
        return getApiBaseUrl() + "/api/payment";
    }
    
    /**
     * Get SSE stream URL for payment
     */
    public static String getPaymentStreamUrl(String paymentId) {
        return getApiBaseUrl() + "/api/payment/" + paymentId + "/stream";
    }
    
    private ApiConfig() {
        // Prevent instantiation
    }
}
