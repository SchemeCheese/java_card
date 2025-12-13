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
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            books.add(parseBookFromJson(element.getAsJsonObject()));
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
        book.setBookId(json.has("bookId") ? json.get("bookId").getAsString() : "");
        book.setTitle(json.has("title") ? json.get("title").getAsString() : "");
        book.setAuthor(json.has("author") ? json.get("author").getAsString() : "");
        book.setIsbn(json.has("isbn") ? json.get("isbn").getAsString() : "");
        book.setPublisher(json.has("publisher") ? json.get("publisher").getAsString() : "");
        book.setPublishYear(json.has("publishYear") ? json.get("publishYear").getAsInt() : 0);
        book.setCategory(json.has("category") ? json.get("category").getAsString() : "");
        book.setDescription(json.has("description") ? json.get("description").getAsString() : "");
        book.setTotalCopies(json.has("totalCopies") ? json.get("totalCopies").getAsInt() : 0);
        book.setAvailableCopies(json.has("availableCopies") ? json.get("availableCopies").getAsInt() : 0);
        book.setStatus(json.has("status") ? json.get("status").getAsString() : "Có sẵn");
        book.setLocation(json.has("location") ? json.get("location").getAsString() : "");
        return book;
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

