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
     * Note: 
     * - PIN is not sent to server - it should be set on the card (applet) only
     * - Only studentId and holderName are required, other fields are optional
     */
    public CardInfo createCard(String studentId, String holderName) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("studentId", studentId);
        body.addProperty("holderName", holderName);
        // email, department, birthDate, address are NOT sent to server - stored on card (applet) via AES encryption
        // PIN is NOT sent to server - it should be set on card (applet) only for security
        
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
     * Pagination result wrapper
     */
    public static class PaginationResult {
        private List<CardInfo> cards;
        private int page;
        private int limit;
        private int total;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrev;
        
        public PaginationResult(List<CardInfo> cards, int page, int limit, int total, int totalPages, boolean hasNext, boolean hasPrev) {
            this.cards = cards;
            this.page = page;
            this.limit = limit;
            this.total = total;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrev = hasPrev;
        }
        
        public List<CardInfo> getCards() { return cards; }
        public int getPage() { return page; }
        public int getLimit() { return limit; }
        public int getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
        public boolean hasNext() { return hasNext; }
        public boolean hasPrev() { return hasPrev; }
    }
    
    /**
     * Get all cards with pagination
     */
    public List<CardInfo> getAllCards(int page, int limit) throws IOException {
        PaginationResult result = getAllCardsWithPagination(page, limit);
        return result.getCards();
    }
    
    /**
     * Get all cards with pagination info
     */
    public PaginationResult getAllCardsWithPagination(int page, int limit) throws IOException {
        String queryParams = "page=" + page + "&limit=" + limit;
        System.out.println("[CardApiService] Fetching cards from server: /cards?" + queryParams);
        
        ApiClient.ApiResponse response = apiClient.get("/cards", queryParams);
        
        System.out.println("[CardApiService] Response status: " + response.getStatusCode() + ", success: " + response.isSuccess());
        
        if (!response.isSuccess()) {
            String errorMsg = response.getMessage() != null ? response.getMessage() : "Unknown error";
            System.err.println("[CardApiService] Failed to get cards: " + errorMsg);
            throw new IOException("Failed to get cards: " + errorMsg);
        }
        
        if (response.getData() == null) {
            System.err.println("[CardApiService] Response data is null");
            throw new IOException("Response data is null");
        }
        
        List<CardInfo> cards = new ArrayList<>();
        int total = 0;
        int totalPages = 1;
        boolean hasNext = false;
        boolean hasPrev = false;
        
        try {
            if (!response.getData().has("data")) {
                System.err.println("[CardApiService] Response does not have 'data' field");
                System.err.println("[CardApiService] Response keys: " + response.getData().keySet());
                return new PaginationResult(cards, page, limit, 0, 1, false, false);
            }
            
            // Parse pagination info
            if (response.getData().has("pagination")) {
                JsonObject pagination = response.getData().getAsJsonObject("pagination");
                if (pagination.has("total")) {
                    total = pagination.get("total").getAsInt();
                }
                if (pagination.has("totalPages")) {
                    totalPages = pagination.get("totalPages").getAsInt();
                }
                if (pagination.has("hasNext")) {
                    hasNext = pagination.get("hasNext").getAsBoolean();
                }
                if (pagination.has("hasPrev")) {
                    hasPrev = pagination.get("hasPrev").getAsBoolean();
                }
            }
            
            JsonArray dataArray = response.getData().getAsJsonArray("data");
            System.out.println("[CardApiService] Found " + dataArray.size() + " cards in response (total: " + total + ", pages: " + totalPages + ")");
            
            for (JsonElement element : dataArray) {
                try {
                    CardInfo card = parseCardFromJson(element.getAsJsonObject());
                    cards.add(card);
                } catch (Exception e) {
                    System.err.println("[CardApiService] Error parsing card: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("[CardApiService] Successfully parsed " + cards.size() + " cards");
        } catch (Exception e) {
            System.err.println("[CardApiService] Error parsing response: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error parsing cards response: " + e.getMessage(), e);
        }
        
        return new PaginationResult(cards, page, limit, total, totalPages, hasNext, hasPrev);
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
     * Update RSA public key for a card
     */
    public boolean updateRSAPublicKey(String studentId, String publicKeyPEM) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("rsaPublicKey", publicKeyPEM);
        
        ApiClient.ApiResponse response = apiClient.put("/cards/" + studentId + "/rsa-key", body);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to update RSA public key: " + response.getMessage());
        }
        
        return true;
    }
    
    /**
     * Parse CardInfo from JSON
     */
    private CardInfo parseCardFromJson(JsonObject json) {
        try {
            CardInfo card = new CardInfo();
            
            // Required fields
            if (json.has("studentId") && !json.get("studentId").isJsonNull()) {
                card.setStudentId(json.get("studentId").getAsString());
            }
            
            // Optional fields with safe parsing
            if (json.has("holderName") && !json.get("holderName").isJsonNull()) {
                card.setHolderName(json.get("holderName").getAsString());
            }
            
            if (json.has("email") && !json.get("email").isJsonNull()) {
                card.setEmail(json.get("email").getAsString());
            }
            
            if (json.has("department") && !json.get("department").isJsonNull()) {
                card.setDepartment(json.get("department").getAsString());
            }
            
            if (json.has("birthDate") && !json.get("birthDate").isJsonNull()) {
                card.setBirthDate(json.get("birthDate").getAsString());
            }
            
            if (json.has("address") && !json.get("address").isJsonNull()) {
                card.setAddress(json.get("address").getAsString());
            }
            
            if (json.has("status") && !json.get("status").isJsonNull()) {
                card.setStatus(json.get("status").getAsString());
            } else {
                card.setStatus("Hoạt động");
            }
            
            if (json.has("balance") && !json.get("balance").isJsonNull()) {
                card.setBalance(json.get("balance").getAsLong());
            }
            
            // CardInfo model uses setBorrowedBooks()
            if (json.has("borrowedBooksCount") && !json.get("borrowedBooksCount").isJsonNull()) {
                card.setBorrowedBooks(json.get("borrowedBooksCount").getAsInt());
            }
            
            // RSA public key
            if (json.has("rsaPublicKey") && !json.get("rsaPublicKey").isJsonNull()) {
                card.setRsaPublicKey(json.get("rsaPublicKey").getAsString());
            }
            
            System.out.println("[CardApiService] Parsed card: " + card.getStudentId() + " - " + card.getHolderName());
            return card;
        } catch (Exception e) {
            System.err.println("[CardApiService] Error parsing card JSON: " + e.getMessage());
            System.err.println("[CardApiService] JSON: " + json.toString());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse card from JSON: " + e.getMessage(), e);
        }
    }
}

