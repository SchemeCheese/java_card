package api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Base HTTP client for API calls
 */
public class ApiClient {
    private static final String BASE_URL = "http://localhost:3000/api";
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public ApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * GET request
     */
    public ApiResponse get(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .get()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * GET request with query parameters
     */
    public ApiResponse get(String endpoint, String queryParams) throws IOException {
        String url = BASE_URL + endpoint;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * POST request
     */
    public ApiResponse post(String endpoint, Object body) throws IOException {
        String jsonBody = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * PUT request
     */
    public ApiResponse put(String endpoint, Object body) throws IOException {
        String jsonBody = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .put(requestBody)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * PATCH request
     */
    public ApiResponse patch(String endpoint, Object body) throws IOException {
        String jsonBody = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
        );
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .patch(requestBody)
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * DELETE request
     */
    public ApiResponse delete(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .delete()
                .build();
        
        return executeRequest(request);
    }
    
    /**
     * Execute HTTP request and parse response
     */
    private ApiResponse executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setStatusCode(response.code());
            apiResponse.setSuccess(response.isSuccessful());
            
            if (!responseBody.isEmpty()) {
                try {
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    apiResponse.setData(json);
                    apiResponse.setMessage(json.has("message") ? json.get("message").getAsString() : "");
                } catch (Exception e) {
                    apiResponse.setRawResponse(responseBody);
                }
            }
            
            return apiResponse;
        }
    }
    
    /**
     * Check if server is available
     */
    public boolean isServerAvailable() {
        try {
            System.out.println("[ApiClient] Checking server availability at " + BASE_URL + "/health");
            ApiResponse response = get("/health");
            boolean available = response.isSuccess();
            System.out.println("[ApiClient] Server available: " + available + " (status: " + response.getStatusCode() + ")");
            return available;
        } catch (Exception e) {
            System.err.println("[ApiClient] Server not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * API Response wrapper
     */
    public static class ApiResponse {
        private int statusCode;
        private boolean success;
        private JsonObject data;
        private String message;
        private String rawResponse;
        
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public JsonObject getData() { return data; }
        public void setData(JsonObject data) { this.data = data; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    }
}

