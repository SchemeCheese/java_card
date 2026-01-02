package api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import config.ApiConfig;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API Service để xử lý VietQR payment
 */
public class PaymentApiService {
    private static final String PAYMENT_ENDPOINT = ApiConfig.getPaymentEndpoint();
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public PaymentApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Tạo payment mới và nhận QR code
     * 
     * @param amount Số tiền thanh toán
     * @param description Mô tả giao dịch
     * @param cardId ID thẻ để nạp tiền (optional)
     * @return PaymentResponse chứa paymentId, orderId, qrCode URL, etc.
     */
    public PaymentResponse createPayment(long amount, String description, String cardId) throws IOException {
        JsonObject metadata = new JsonObject();
        if (cardId != null && !cardId.isEmpty()) {
            metadata.addProperty("cardId", cardId);
        }
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("amount", amount);
        requestBody.addProperty("description", description);
        requestBody.add("metadata", metadata);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            requestBody.toString()
        );
        
        Request request = new Request.Builder()
                .url(PAYMENT_ENDPOINT + "/create")
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " - " + responseBody);
            }
            
            ApiResponse apiResponse = gson.fromJson(responseBody, ApiResponse.class);
            
            if (!apiResponse.success) {
                throw new IOException("API error: " + apiResponse.message);
            }
            
            return gson.fromJson(gson.toJson(apiResponse.data), PaymentResponse.class);
        }
    }
    
    /**
     * Lấy thông tin payment theo ID (để polling status)
     * 
     * @param paymentId ID của payment
     * @return PaymentStatusResponse chứa status, amount, qrCode, etc.
     */
    public PaymentStatusResponse getPaymentStatus(String paymentId) throws IOException {
        Request request = new Request.Builder()
                .url(PAYMENT_ENDPOINT + "/" + paymentId)
                .get()
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " - " + responseBody);
            }
            
            ApiResponse apiResponse = gson.fromJson(responseBody, ApiResponse.class);
            
            if (!apiResponse.success) {
                throw new IOException("API error: " + apiResponse.message);
            }
            
            return gson.fromJson(gson.toJson(apiResponse.data), PaymentStatusResponse.class);
        }
    }
    
    /**
     * Response khi tạo payment
     */
    public static class PaymentResponse {
        public String paymentId;
        public String orderId;
        public long amount;
        public String qrCode; // URL của QR code image
        public BankInfo bankInfo;
        public String transferContent;
        public String expiredAt;
    }
    
    /**
     * Response khi check payment status
     */
    public static class PaymentStatusResponse {
        public String id;
        public String orderId;
        public long amount;
        public String currency;
        public String status; // PENDING, SUCCESS, FAILED, EXPIRED, CANCELLED
        public String qrCode;
        public BankInfo bankInfo;
        public String description;
        public String expiredAt;
        public String paidAt;
        public String createdAt;
        public String updatedAt;
    }
    
    /**
     * Thông tin ngân hàng
     */
    public static class BankInfo {
        public String bankCode;
        public String accountNumber;
        public String accountName;
    }
    
    /**
     * Generic API response wrapper
     */
    private static class ApiResponse {
        public boolean success;
        public String message;
        public Object data;
    }
}
