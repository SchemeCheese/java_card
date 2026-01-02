package api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import config.ApiConfig;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SSE Client để nhận real-time payment updates từ server
 */
public class SSEClient {
    
    private final OkHttpClient client;
    private final Gson gson;
    private Call currentCall;
    private boolean isConnected = false;
    
    public SSEClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Connect tới SSE stream cho payment
     * 
     * @param paymentId ID của payment
     * @param onEvent Callback khi nhận được event
     * @param onError Callback khi có lỗi
     */
    public void connectToPaymentStream(
            String paymentId,
            Consumer<SSEEvent> onEvent,
            Consumer<Exception> onError
    ) {
        if (isConnected) {
            System.out.println("[SSE] Already connected, disconnecting first...");
            disconnect();
        }
        
        String url = ApiConfig.getPaymentStreamUrl(paymentId);
        
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build();
        
        currentCall = client.newCall(request);
        
        // Execute in background thread
        new Thread(() -> {
            try {
                Response response = currentCall.execute();
                
                if (!response.isSuccessful()) {
                    onError.accept(new IOException("SSE connection failed: " + response.code()));
                    return;
                }
                
                isConnected = true;
                System.out.println("[SSE] Connected to payment stream: " + paymentId);
                
                // Read SSE stream
                InputStream inputStream = response.body().byteStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                
                String line;
                String eventType = null;
                StringBuilder dataBuilder = new StringBuilder();
                
                while ((line = reader.readLine()) != null && isConnected) {
                    if (line.startsWith("event:")) {
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        dataBuilder.append(line.substring(5).trim());
                    } else if (line.isEmpty()) {
                        // End of event
                        if (eventType != null && dataBuilder.length() > 0) {
                            String data = dataBuilder.toString();
                            SSEEvent event = new SSEEvent(eventType, data);
                            
                            // Call callback on EDT thread
                            javax.swing.SwingUtilities.invokeLater(() -> onEvent.accept(event));
                        }
                        
                        // Reset for next event
                        eventType = null;
                        dataBuilder.setLength(0);
                    }
                }
                
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("[SSE] Connection error: " + e.getMessage());
                    onError.accept(e);
                }
            } finally {
                isConnected = false;
                System.out.println("[SSE] Disconnected from payment stream");
            }
        }).start();
    }
    
    /**
     * Disconnect khỏi SSE stream
     */
    public void disconnect() {
        isConnected = false;
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
    }
    
    /**
     * Check connection status
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * SSE Event class
     */
    public static class SSEEvent {
        private final String eventType;
        private final String data;
        private final Gson gson = new Gson();
        
        public SSEEvent(String eventType, String data) {
            this.eventType = eventType;
            this.data = data;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public String getData() {
            return data;
        }
        
        public JsonObject getDataAsJson() {
            return gson.fromJson(data, JsonObject.class);
        }
        
        @Override
        public String toString() {
            return "SSEEvent{type='" + eventType + "', data='" + data + "'}";
        }
    }
}
