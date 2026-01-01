package api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * API Service for Book Inventory operations
 */
public class BookInventoryApiService {
    private final ApiClient apiClient;
    
    public BookInventoryApiService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Get all books with pagination and filters
     */
    public List<BookInfo> getAllBooks(String category, String status, String search, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        if (category != null && !category.isEmpty()) {
            queryParams.append("&category=").append(category);
        }
        if (status != null && !status.isEmpty()) {
            queryParams.append("&status=").append(status);
        }
        if (search != null && !search.isEmpty()) {
            queryParams.append("&search=").append(search);
        }
        
        ApiClient.ApiResponse response = apiClient.get("/library/books", queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get books: " + response.getMessage());
        }
        
        List<BookInfo> books = new ArrayList<>();
        
        // Handle null or empty data
        JsonObject responseData = response.getData();
        if (responseData == null) {
            System.out.println("[BookInventoryApi] No response data");
            return books;
        }
        
        // Check if "data" field exists and is an array
        if (!responseData.has("data")) {
            System.out.println("[BookInventoryApi] Response has no 'data' field");
            return books;
        }
        
        JsonElement dataElement = responseData.get("data");
        if (dataElement == null || dataElement.isJsonNull()) {
            System.out.println("[BookInventoryApi] 'data' field is null");
            return books;
        }
        
        if (!dataElement.isJsonArray()) {
            System.out.println("[BookInventoryApi] 'data' is not an array: " + dataElement.getClass().getSimpleName());
            return books;
        }
        
        JsonArray dataArray = dataElement.getAsJsonArray();
        System.out.println("[BookInventoryApi] Found " + dataArray.size() + " books");
        
        for (JsonElement element : dataArray) {
            try {
                books.add(parseBookFromJson(element.getAsJsonObject()));
            } catch (Exception e) {
                System.err.println("[BookInventoryApi] Error parsing book: " + e.getMessage());
            }
        }
        
        return books;
    }
    
    /**
     * Search books
     */
    public List<BookInfo> searchBooks(String query, String category, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        queryParams.append("&query=").append(query);
        if (category != null && !category.isEmpty()) {
            queryParams.append("&category=").append(category);
        }
        
        ApiClient.ApiResponse response = apiClient.get("/library/books/search", queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to search books: " + response.getMessage());
        }
        
        List<BookInfo> books = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            books.add(parseBookFromJson(element.getAsJsonObject()));
        }
        
        return books;
    }
    
    /**
     * Get book by ID
     */
    public BookInfo getBookById(String bookId) throws IOException {
        ApiClient.ApiResponse response = apiClient.get("/library/books/" + bookId);
        
        if (!response.isSuccess()) {
            if (response.getStatusCode() == 404) {
                return null;
            }
            throw new IOException("Failed to get book: " + response.getMessage());
        }
        
        return parseBookFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Create a new book
     */
    public BookInfo createBook(JsonObject bookData) throws IOException {
        ApiClient.ApiResponse response = apiClient.post("/library/books", bookData);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to create book: " + response.getMessage());
        }
        
        return parseBookFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Update book
     */
    public BookInfo updateBook(String bookId, JsonObject updates) throws IOException {
        ApiClient.ApiResponse response = apiClient.put("/library/books/" + bookId, updates);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to update book: " + response.getMessage());
        }
        
        return parseBookFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Delete book
     */
    public boolean deleteBook(String bookId) throws IOException {
        ApiClient.ApiResponse response = apiClient.delete("/library/books/" + bookId);
        return response.isSuccess();
    }
    
    /**
     * Get all categories
     */
    public List<String> getCategories() throws IOException {
        ApiClient.ApiResponse response = apiClient.get("/library/books/categories");
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get categories: " + response.getMessage());
        }
        
        List<String> categories = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            categories.add(element.getAsString());
        }
        
        return categories;
    }
    
    /**
     * Parse BookInfo from JSON
     */
    private BookInfo parseBookFromJson(JsonObject json) {
        BookInfo book = new BookInfo();
        book.setBookId(getStringOrDefault(json, "bookId", ""));
        book.setTitle(getStringOrDefault(json, "title", ""));
        book.setAuthor(getStringOrDefault(json, "author", ""));
        book.setIsbn(getStringOrDefault(json, "isbn", ""));
        book.setPublisher(getStringOrDefault(json, "publisher", ""));
        book.setPublishYear(getIntOrDefault(json, "publishYear", 0));
        book.setCategory(getStringOrDefault(json, "category", ""));
        book.setDescription(getStringOrDefault(json, "description", ""));
        book.setTotalCopies(getIntOrDefault(json, "totalCopies", 0));
        book.setAvailableCopies(getIntOrDefault(json, "availableCopies", 0));
        book.setStatus(getStringOrDefault(json, "status", "Có sẵn"));
        book.setLocation(getStringOrDefault(json, "location", ""));
        return book;
    }
    
    /**
     * Safely get string from JSON, handling null values
     */
    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (!json.has(key)) return defaultValue;
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) return defaultValue;
        return element.getAsString();
    }
    
    /**
     * Safely get int from JSON, handling null values
     */
    private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
        if (!json.has(key)) return defaultValue;
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) return defaultValue;
        return element.getAsInt();
    }
    
    /**
     * Book Info model
     */
    public static class BookInfo {
        private String bookId;
        private String title;
        private String author;
        private String isbn;
        private String publisher;
        private int publishYear;
        private String category;
        private String description;
        private int totalCopies;
        private int availableCopies;
        private String status;
        private String location;
        
        // Getters and Setters
        public String getBookId() { return bookId; }
        public void setBookId(String bookId) { this.bookId = bookId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        
        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }
        
        public int getPublishYear() { return publishYear; }
        public void setPublishYear(int publishYear) { this.publishYear = publishYear; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getTotalCopies() { return totalCopies; }
        public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
        
        public int getAvailableCopies() { return availableCopies; }
        public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }
}

