package api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.Transaction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * API Service for Transaction operations
 */
public class TransactionApiService {
    private final ApiClient apiClient;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
    public TransactionApiService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Constructor with shared ApiClient
     */
    public TransactionApiService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    /**
     * Create a transaction (simplified - server calculates balance)
     */
    public Transaction createTransaction(String studentId, String type, long amount, String description) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("studentId", studentId);
        body.addProperty("type", type);
        body.addProperty("amount", amount);
        body.addProperty("description", description != null ? description : "");
        
        ApiClient.ApiResponse response = apiClient.post("/transactions", body);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to create transaction: " + response.getMessage());
        }
        
        return parseTransactionFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Create a transaction (with balance info - for compatibility)
     */
    public Transaction createTransaction(String studentId, String type, long amount, 
                                       long balanceBefore, long balanceAfter, String description) throws IOException {
        // Just call the simplified version - server will calculate balance
        return createTransaction(studentId, type, amount, description);
    }
    
    /**
     * Get transactions by student with pagination
     */
    public List<Transaction> getTransactionsByStudent(String studentId, String type, String status,
                                                     Date startDate, Date endDate, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        if (type != null && !type.isEmpty()) {
            queryParams.append("&type=").append(type);
        }
        if (status != null && !status.isEmpty()) {
            queryParams.append("&status=").append(status);
        }
        if (startDate != null) {
            queryParams.append("&startDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        }
        if (endDate != null) {
            queryParams.append("&endDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(endDate));
        }
        
        ApiClient.ApiResponse response = apiClient.get("/transactions/student/" + studentId, queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get transactions: " + response.getMessage());
        }
        
        List<Transaction> transactions = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            transactions.add(parseTransactionFromJson(element.getAsJsonObject()));
        }
        
        return transactions;
    }
    
    /**
     * Get all transactions with pagination
     */
    public List<Transaction> getAllTransactions(String type, String status, Date startDate, 
                                               Date endDate, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        if (type != null && !type.isEmpty()) {
            queryParams.append("&type=").append(type);
        }
        if (status != null && !status.isEmpty()) {
            queryParams.append("&status=").append(status);
        }
        if (startDate != null) {
            queryParams.append("&startDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        }
        if (endDate != null) {
            queryParams.append("&endDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(endDate));
        }
        
        ApiClient.ApiResponse response = apiClient.get("/transactions", queryParams.toString());
        
        if (!response.isSuccess()) {
            System.err.println("[TransactionAPI] Failed: " + response.getMessage());
            throw new IOException("Failed to get all transactions: " + response.getMessage());
        }
        
        List<Transaction> transactions = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        
        for (int i = 0; i < dataArray.size(); i++) {
            Transaction tx = parseTransactionFromJson(dataArray.get(i).getAsJsonObject());
            transactions.add(tx);
            
            // Log first and last transaction
            if (i == 0 || i == dataArray.size() - 1) {
                System.out.println("[TransactionAPI] #" + (i + 1) + ": " + 
                    tx.getType() + " - " + tx.getAmount() + "đ - " + tx.getDate());
            }
        }
        
        System.out.println("[TransactionAPI] Parsed " + transactions.size() + " transactions");
        
        return transactions;
    }
    
    /**
     * Get transaction statistics
     */
    public JsonObject getTransactionStats(String studentId, Date startDate, Date endDate) throws IOException {
        StringBuilder queryParams = new StringBuilder();
        if (startDate != null) {
            queryParams.append("startDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(startDate));
        }
        if (endDate != null) {
            if (queryParams.length() > 0) queryParams.append("&");
            queryParams.append("endDate=").append(new SimpleDateFormat("yyyy-MM-dd").format(endDate));
        }
        
        String endpoint = "/transactions/stats" + (studentId != null ? "/" + studentId : "");
        ApiClient.ApiResponse response = apiClient.get(endpoint, queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get transaction stats: " + response.getMessage());
        }
        
        return response.getData().getAsJsonObject("data");
    }
    
    /**
     * Parse Transaction from JSON
     */
    private Transaction parseTransactionFromJson(JsonObject json) {
        try {
            // Try multiple date field names
            String date = "";
            if (json.has("createdAt")) {
                date = formatDateTime(json.get("createdAt").getAsString());
            } else if (json.has("created_at")) {
                date = formatDateTime(json.get("created_at").getAsString());
            } else if (json.has("date")) {
                date = json.get("date").getAsString();
            }
            
            // If still no date, use today's date
            if (date.isEmpty()) {
                date = dateFormat.format(new Date());
            }
            
            String type = json.has("type") ? json.get("type").getAsString() : "";
            String description = json.has("description") ? json.get("description").getAsString() : type;
            long amount = json.has("amount") ? json.get("amount").getAsLong() : 0;
            String status = json.has("status") ? json.get("status").getAsString() : "Thành công";

            // Ensure 'Trả phạt' is always negative
            if ("Trả phạt".equalsIgnoreCase(type) && amount > 0) {
                amount = -amount;
            }
            return new Transaction(date, type, amount, status);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Format date string from ISO format to dd/MM/yyyy HH:mm:ss (UTC+7)
     */
    private String formatDateTime(String isoDate) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoDate);
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            outputFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // UTC+7
            return outputFormat.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat isoFormat2 = new SimpleDateFormat("yyyy-MM-dd");
                isoFormat2.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                Date date = isoFormat2.parse(isoDate);
                
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                outputFormat.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // UTC+7
                return outputFormat.format(date);
            } catch (Exception e2) {
                return isoDate;
            }
        }
    }
    
    /**
     * Format date string from ISO format to dd/MM/yyyy
     */
    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = isoFormat.parse(isoDate);
            return dateFormat.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat isoFormat2 = new SimpleDateFormat("yyyy-MM-dd");
                Date date = isoFormat2.parse(isoDate);
                return dateFormat.format(date);
            } catch (Exception e2) {
                return isoDate;
            }
        }
    }
}

