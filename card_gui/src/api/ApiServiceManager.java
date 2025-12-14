package api;

/**
 * Central manager for all API services
 * Provides single point of access to all API services
 */
public class ApiServiceManager {
    private static ApiServiceManager instance;
    
    private final ApiClient apiClient;
    private final CardApiService cardApiService;
    private final BookApiService bookApiService;
    private final TransactionApiService transactionApiService;
    private final BookInventoryApiService bookInventoryApiService;
    
    private ApiServiceManager() {
        this.apiClient = new ApiClient();
        this.cardApiService = new CardApiService();
        this.bookApiService = new BookApiService();
        this.transactionApiService = new TransactionApiService();
        this.bookInventoryApiService = new BookInventoryApiService();
    }
    
    public static synchronized ApiServiceManager getInstance() {
        if (instance == null) {
            instance = new ApiServiceManager();
        }
        return instance;
    }
    
    public ApiClient getApiClient() {
        return apiClient;
    }
    
    public CardApiService getCardApiService() {
        return cardApiService;
    }
    
    public BookApiService getBookApiService() {
        return bookApiService;
    }
    
    public TransactionApiService getTransactionApiService() {
        return transactionApiService;
    }
    
    public BookInventoryApiService getBookInventoryApiService() {
        return bookInventoryApiService;
    }
    
    /**
     * Check if server is available
     */
    public boolean isServerAvailable() {
        return apiClient.isServerAvailable();
    }
    
    /**
     * Set authentication token for all API services
     */
    public void setAuthToken(String token) {
        apiClient.setAuthToken(token);
        // Note: Each service has its own ApiClient instance, so we need to set token for each
        // For now, we'll set it on the shared apiClient and services should use it
        // TODO: Refactor to use shared ApiClient instance
    }
    
    /**
     * Clear authentication token
     */
    public void clearAuthToken() {
        apiClient.clearAuthToken();
    }
}

