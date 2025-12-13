package api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.CardInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * API Service for Card operations
 */
public class CardApiService {
    private final ApiClient apiClient;
    
    public CardApiService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Create a new card
     */
    public CardInfo createCard(String studentId, String holderName, String email,
                               String department, String birthDate, String address, String pin) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("studentId", studentId);
        body.addProperty("holderName", holderName);
        body.addProperty("email", email);
        body.addProperty("department", department);
        body.addProperty("birthDate", birthDate);
        body.addProperty("address", address);
        body.addProperty("pin", pin);
        
        ApiClient.ApiResponse response = apiClient.post("/cards", body);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to create card: " + response.getMessage());
        }
        
        return parseCardFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Get card by student ID
     */
    public CardInfo getCard(String studentId) throws IOException {
        ApiClient.ApiResponse response = apiClient.get("/cards/" + studentId);
        
        if (!response.isSuccess()) {
            if (response.getStatusCode() == 404) {
                return null;
            }
            throw new IOException("Failed to get card: " + response.getMessage());
        }
        
        return parseCardFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Get all cards with pagination
     */
    public List<CardInfo> getAllCards(int page, int limit) throws IOException {
        String queryParams = "page=" + page + "&limit=" + limit;
        ApiClient.ApiResponse response = apiClient.get("/cards", queryParams);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get cards: " + response.getMessage());
        }
        
        List<CardInfo> cards = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            cards.add(parseCardFromJson(element.getAsJsonObject()));
        }
        
        return cards;
    }
    
    /**
     * Update card information
     */
    public CardInfo updateCard(String studentId, JsonObject updates) throws IOException {
        ApiClient.ApiResponse response = apiClient.put("/cards/" + studentId, updates);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to update card: " + response.getMessage());
        }
        
        return parseCardFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Delete card
     */
    public boolean deleteCard(String studentId) throws IOException {
        ApiClient.ApiResponse response = apiClient.delete("/cards/" + studentId);
        return response.isSuccess();
    }
    
    /**
     * Update card balance
     */
    public long updateBalance(String studentId, long amount) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("amount", amount);
        
        ApiClient.ApiResponse response = apiClient.put("/cards/" + studentId + "/balance", body);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to update balance: " + response.getMessage());
        }
        
        return response.getData().getAsJsonObject("data").get("balance").getAsLong();
    }
    
    /**
     * Parse CardInfo from JSON
     */
    private CardInfo parseCardFromJson(JsonObject json) {
        CardInfo card = new CardInfo();
        card.setStudentId(json.get("studentId").getAsString());
        card.setHolderName(json.has("holderName") ? json.get("holderName").getAsString() : "");
        card.setEmail(json.has("email") ? json.get("email").getAsString() : "");
        card.setDepartment(json.has("department") ? json.get("department").getAsString() : "");
        card.setBirthDate(json.has("birthDate") ? json.get("birthDate").getAsString() : "");
        card.setAddress(json.has("address") ? json.get("address").getAsString() : "");
        card.setStatus(json.has("status") ? json.get("status").getAsString() : "Hoạt động");
        card.setBalance(json.has("balance") ? json.get("balance").getAsLong() : 0);
        // CardInfo model uses setBorrowedBooks()
        if (json.has("borrowedBooksCount")) {
            card.setBorrowedBooks(json.get("borrowedBooksCount").getAsInt());
        }
        return card;
    }
}

