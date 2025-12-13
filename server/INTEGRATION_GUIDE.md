# Hướng dẫn Tích hợp Server với Java GUI

Tài liệu này hướng dẫn cách kết nối Java GUI (card_gui) với Express.js backend server.

## Tổng quan Kiến trúc

```
┌─────────────────┐
│   Java GUI      │
│  (card_gui)     │
└────────┬────────┘
         │ HTTP REST API
         │
┌────────▼────────┐
│  Express Server │
│   (Node.js)     │
└────────┬────────┘
         │
┌────────▼────────┐
│    MongoDB      │
│   (Database)    │
└─────────────────┘
```

**Trước đây:**
- Java GUI ↔ JCardSim (in-memory, không persist)

**Bây giờ:**
- Java GUI ↔ Express Server ↔ MongoDB (persistent storage)

## Option 1: Thay thế hoàn toàn JCardSim

Loại bỏ JCardSim, chỉ dùng REST API để kết nối với server.

### Bước 1: Thêm HTTP Client library vào card_gui/pom.xml

```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- HTTP Client for API calls -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- JSON parsing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

### Bước 2: Tạo API Service class

Tạo file: `card_gui/src/service/ApiService.java`

```java
package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import models.CardInfo;
import models.BorrowedBook;
import models.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApiService {
    private static final String BASE_URL = "http://localhost:3000/api";
    private final OkHttpClient client;
    private final Gson gson;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ApiService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    // ==================== Card APIs ====================
    
    public boolean createCard(CardInfo cardInfo, String pin) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("studentId", cardInfo.getStudentId());
        json.addProperty("holderName", cardInfo.getHolderName());
        json.addProperty("email", cardInfo.getEmail());
        json.addProperty("department", cardInfo.getDepartment());
        json.addProperty("birthDate", cardInfo.getBirthDate());
        json.addProperty("address", cardInfo.getAddress());
        json.addProperty("pin", pin);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/cards")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public CardInfo getCard(String studentId) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/cards/" + studentId)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                JsonObject data = json.getAsJsonObject("data");
                
                CardInfo card = new CardInfo();
                card.setStudentId(data.get("studentId").getAsString());
                card.setHolderName(data.get("holderName").getAsString());
                card.setEmail(data.get("email").getAsString());
                card.setDepartment(data.get("department").getAsString());
                card.setBirthDate(data.get("birthDate").getAsString());
                card.setAddress(data.get("address").getAsString());
                card.setStatus(data.get("status").getAsString());
                card.setBalance(data.get("balance").getAsLong());
                card.setBorrowedBooks(data.get("borrowedBooks").getAsInt());
                
                return card;
            }
        }
        return null;
    }

    public boolean updateCard(String studentId, CardInfo cardInfo) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("holderName", cardInfo.getHolderName());
        json.addProperty("email", cardInfo.getEmail());
        json.addProperty("department", cardInfo.getDepartment());
        json.addProperty("birthDate", cardInfo.getBirthDate());
        json.addProperty("address", cardInfo.getAddress());

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/cards/" + studentId)
                .put(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // ==================== PIN APIs ====================
    
    public boolean verifyPin(String studentId, String pin) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("studentId", studentId);
        json.addProperty("pin", pin);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/pin/verify")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean changePin(String studentId, String oldPin, String newPin) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("studentId", studentId);
        json.addProperty("oldPin", oldPin);
        json.addProperty("newPin", newPin);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/pin/change")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public int getPinTries(String studentId) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/pin/tries/" + studentId)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                return json.get("triesRemaining").getAsInt();
            }
        }
        return 0;
    }

    // ==================== Book APIs ====================
    
    public boolean borrowBook(String studentId, String bookId, String bookName, String dueDate) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("studentId", studentId);
        json.addProperty("bookId", bookId);
        json.addProperty("bookName", bookName);
        json.addProperty("dueDate", dueDate);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/books/borrow")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public List<BorrowedBook> getBorrowedBooks(String studentId) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/books/student/" + studentId)
                .get()
                .build();

        List<BorrowedBook> books = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                // Parse data array and convert to BorrowedBook objects
                // ... (implement parsing logic)
            }
        }
        return books;
    }

    // ==================== Transaction APIs ====================
    
    public boolean createTransaction(String studentId, String type, long amount, String description) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("studentId", studentId);
        json.addProperty("type", type);
        json.addProperty("amount", amount);
        json.addProperty("description", description);

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public List<Transaction> getTransactions(String studentId) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions/student/" + studentId)
                .get()
                .build();

        List<Transaction> transactions = new ArrayList<>();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                // Parse data array and convert to Transaction objects
                // ... (implement parsing logic)
            }
        }
        return transactions;
    }
}
```

### Bước 3: Cập nhật SimulatorService

Sửa `card_gui/src/service/SimulatorService.java` để dùng `ApiService`:

```java
package service;

import models.CardInfo;
// ...

public class SimulatorService {
    private ApiService apiService;
    
    public SimulatorService() {
        this.apiService = new ApiService();
    }
    
    public boolean createCard(CardInfo cardInfo, String pin) {
        try {
            return apiService.createCard(cardInfo, pin);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public CardInfo getCardInfo(String studentId) {
        try {
            return apiService.getCard(studentId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Implement other methods similarly...
}
```

## Option 2: Hybrid - Dùng cả JCardSim và Server

Giữ nguyên JCardSim cho applet logic, nhưng sync data với server để persist.

### Workflow:
1. User tạo thẻ trong GUI
2. GUI → JCardSim (tạo applet)
3. GUI → Server (lưu vào MongoDB)
4. Khi app restart: Load từ MongoDB → Khởi tạo lại JCardSim

### Code example:

```java
public class HybridService {
    private SimulatorService simulatorService; // JCardSim
    private ApiService apiService;             // REST API
    
    public HybridService() {
        this.simulatorService = new SimulatorService();
        this.apiService = new ApiService();
    }
    
    public boolean createCard(CardInfo cardInfo, String pin) {
        // 1. Create in JCardSim
        boolean simSuccess = simulatorService.createDemoPin(pin);
        
        // 2. Save to server
        if (simSuccess) {
            try {
                apiService.createCard(cardInfo, pin);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return simSuccess;
    }
    
    public void loadCardFromServer(String studentId) {
        try {
            CardInfo card = apiService.getCard(studentId);
            if (card != null) {
                // Initialize JCardSim with card data
                simulatorService.setCardInfo(card);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## Testing Integration

### Test 1: Create Card
```java
ApiService api = new ApiService();
CardInfo card = new CardInfo(
    "2021600001",
    "Nguyen Van A",
    "test@example.com",
    "IT",
    "01/01/2000",
    "Address"
);

boolean success = api.createCard(card, "123456");
System.out.println("Create card: " + success);
```

### Test 2: Verify PIN
```java
boolean verified = api.verifyPin("2021600001", "123456");
System.out.println("PIN verified: " + verified);
```

### Test 3: Get Card Info
```java
CardInfo card = api.getCard("2021600001");
System.out.println("Card: " + card.getHolderName());
```

## Error Handling

```java
try {
    CardInfo card = apiService.getCard(studentId);
    if (card == null) {
        JOptionPane.showMessageDialog(null, 
            "Không tìm thấy thẻ", 
            "Lỗi", 
            JOptionPane.ERROR_MESSAGE);
    }
} catch (IOException e) {
    JOptionPane.showMessageDialog(null, 
        "Không thể kết nối đến server. Vui lòng kiểm tra server đang chạy.", 
        "Lỗi kết nối", 
        JOptionPane.ERROR_MESSAGE);
}
```

## Configuration

Tạo file `card_gui/src/config/AppConfig.java`:

```java
package config;

public class AppConfig {
    public static final String API_BASE_URL = "http://localhost:3000/api";
    public static final int REQUEST_TIMEOUT = 30; // seconds
    
    // For production
    // public static final String API_BASE_URL = "https://your-server.com/api";
}
```

## Next Steps

1. Chạy server: `cd server && npm run dev`
2. Test API bằng Postman
3. Implement ApiService trong Java GUI
4. Update SimulatorService để dùng ApiService
5. Test toàn bộ flow trong GUI

## Troubleshooting

**Lỗi: Connection refused**
- Kiểm tra server đang chạy: `http://localhost:3000/api/health`
- Kiểm tra firewall

**Lỗi: JSON parsing**
- Kiểm tra format response từ server
- Thêm logging để debug

**Lỗi: CORS (nếu chạy từ browser)**
- Server đã có `cors()` middleware
- Không gặp vấn đề khi call từ Java

