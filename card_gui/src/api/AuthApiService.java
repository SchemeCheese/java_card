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
        this.apiClient = new ApiClient();
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
            
            // Gọi API login (không cần token vì đây là login endpoint)
            ApiClient.ApiResponse response = apiClient.post("/auth/login", body);
            
            if (!response.isSuccess()) {
                System.err.println("[AuthApiService] Login failed: " + response.getMessage());
                return null;
            }
            
            // Extract token từ response
            if (response.getData() != null && response.getData().has("data")) {
                JsonObject data = response.getData().getAsJsonObject("data");
                if (data.has("token")) {
                    String token = data.get("token").getAsString();
                    System.out.println("[AuthApiService] Login successful, token received");
                    
                    // Set token vào ApiClient để dùng cho các request sau
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

