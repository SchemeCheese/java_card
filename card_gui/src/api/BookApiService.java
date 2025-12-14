package api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import models.BorrowedBook;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * API Service for Book borrowing operations
 */
public class BookApiService {
    private final ApiClient apiClient;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
    public BookApiService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Borrow a book
     */
    public BorrowedBook borrowBook(String studentId, String bookId, String bookName, Date dueDate) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("studentId", studentId);
        body.addProperty("bookId", bookId);
        body.addProperty("bookName", bookName);
        body.addProperty("dueDate", new SimpleDateFormat("yyyy-MM-dd").format(dueDate));
        
        ApiClient.ApiResponse response = apiClient.post("/books/borrow", body);
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to borrow book: " + response.getMessage());
        }
        
        return parseBorrowedBookFromJson(response.getData().getAsJsonObject("data"));
    }
    
    /**
     * Return a book
     */
    public BorrowedBook returnBook(int borrowId) throws IOException {
        ApiClient.ApiResponse response = apiClient.patch("/books/return/" + borrowId, new JsonObject());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to return book: " + response.getMessage());
        }
        
        return parseBorrowedBookFromJson(response.getData().getAsJsonObject("data").getAsJsonObject("borrowedBook"));
    }
    
    /**
     * Get borrowed books by student with pagination
     */
    public List<BorrowedBook> getBorrowedBooksByStudent(String studentId, String status, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        if (status != null && !status.isEmpty()) {
            queryParams.append("&status=").append(status);
        }
        
        ApiClient.ApiResponse response = apiClient.get("/books/student/" + studentId, queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get borrowed books: " + response.getMessage());
        }
        
        List<BorrowedBook> books = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            books.add(parseBorrowedBookFromJson(element.getAsJsonObject()));
        }
        
        return books;
    }
    
    /**
     * Get all borrowed books with pagination
     */
    public List<BorrowedBook> getAllBorrowedBooks(String status, int page, int limit) throws IOException {
        StringBuilder queryParams = new StringBuilder("page=" + page + "&limit=" + limit);
        if (status != null && !status.isEmpty()) {
            queryParams.append("&status=").append(status);
        }
        
        ApiClient.ApiResponse response = apiClient.get("/books", queryParams.toString());
        
        if (!response.isSuccess()) {
            throw new IOException("Failed to get all borrowed books: " + response.getMessage());
        }
        
        List<BorrowedBook> books = new ArrayList<>();
        JsonArray dataArray = response.getData().getAsJsonArray("data");
        
        for (JsonElement element : dataArray) {
            books.add(parseBorrowedBookFromJson(element.getAsJsonObject()));
        }
        
        return books;
    }
    
    /**
     * Delete borrowed book record
     */
    public boolean deleteBorrowedBook(int borrowId) throws IOException {
        ApiClient.ApiResponse response = apiClient.delete("/books/" + borrowId);
        return response.isSuccess();
    }
    
    /**
     * Parse BorrowedBook from JSON
     */
    private BorrowedBook parseBorrowedBookFromJson(JsonObject json) {
        try {
            int id = json.has("id") ? json.get("id").getAsInt() : 0;
            String bookId = json.has("bookId") ? json.get("bookId").getAsString() : "";
            String bookName = json.has("bookName") ? json.get("bookName").getAsString() : "";
            String borrowDate = json.has("borrowDate") ? formatDate(json.get("borrowDate").getAsString()) : "";
            String dueDate = json.has("dueDate") ? formatDate(json.get("dueDate").getAsString()) : "";
            String status = json.has("status") ? json.get("status").getAsString() : "Đang mượn";
            int overdueDays = json.has("overdueDays") ? json.get("overdueDays").getAsInt() : 0;
            
            return new BorrowedBook(id, bookId, bookName, borrowDate, dueDate, status, overdueDays);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

