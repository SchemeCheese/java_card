package api;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Base64;

/**
 * API Service for Authentication operations
 */
public class AuthApiService {
    private final ApiClient apiClient;
    
    public AuthApiService() {
        // Create own ApiClient - token is shared via static field in ApiClient
        this.apiClient = new ApiClient();
    }
    
    /**
     * Constructor with shared ApiClient
     */
    public AuthApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Login với RSA signature
     * Client đã verify RSA ở client-side, giờ gửi lên server để verify lại và lấy token
     * 
     * @param studentId Student ID
     * @param challenge Challenge bytes (16 bytes)
     * @param signature RSA signature (128 bytes)
     * @return JWT token nếu thành công, null nếu thất bại
     */
    public String login(String studentId, byte[] challenge, byte[] signature) throws IOException {
        try {
            // Encode challenge và signature thành Base64
            String challengeBase64 = Base64.getEncoder().encodeToString(challenge);
            String signatureBase64 = Base64.getEncoder().encodeToString(signature);
            
            // Build request body
            JsonObject body = new JsonObject();
            body.addProperty("studentId", studentId);
            body.addProperty("challenge", challengeBase64);
            body.addProperty("signature", signatureBase64);
            
            System.out.println("[AuthApiService] Calling login API for studentId: " + studentId);
            
            // Gọi API login (không cần token vì đây là login endpoint)
            ApiClient.ApiResponse response = apiClient.post("/auth/login", body);
            
            System.out.println("[AuthApiService] Response status: " + response.getStatusCode() + ", success: " + response.isSuccess());
            System.out.println("[AuthApiService] Response data: " + (response.getData() != null ? response.getData().toString() : "null"));
            
            if (!response.isSuccess()) {
                System.err.println("[AuthApiService] Login failed: " + response.getMessage());
                return null;
            }
            
            // Extract token từ response - getData() trả về toàn bộ JSON response
            JsonObject responseData = response.getData();
            if (responseData != null) {
                // Check if response has nested "data" object (standard API format)
                if (responseData.has("data") && responseData.get("data").isJsonObject()) {
                    JsonObject data = responseData.getAsJsonObject("data");
                    if (data.has("token")) {
                        String token = data.get("token").getAsString();
                        System.out.println("[AuthApiService] Login successful, token received from data.token");
                        apiClient.setAuthToken(token);
                        return token;
                    }
                }
                // Fallback: check if token is directly in response (alternative format)
                if (responseData.has("token")) {
                    String token = responseData.get("token").getAsString();
                    System.out.println("[AuthApiService] Login successful, token received from root.token");
                    apiClient.setAuthToken(token);
                    return token;
                }
            }
            
            System.err.println("[AuthApiService] Token not found in response");
            return null;
            
        } catch (Exception e) {
            System.err.println("[AuthApiService] Login error: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to login: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get ApiClient instance (để share token)
     */
    public ApiClient getApiClient() {
        return apiClient;
    }
}









