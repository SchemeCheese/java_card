package api;

/**
 * Central manager for all API services
 * Provides single point of access to all API services
 * All services share the same ApiClient instance for token management
 */
public class ApiServiceManager {
    private static ApiServiceManager instance;
    
    private final ApiClient sharedApiClient;
    private final CardApiService cardApiService;
    private final BookApiService bookApiService;
    private final TransactionApiService transactionApiService;
    private final BookInventoryApiService bookInventoryApiService;
    
    private ApiServiceManager() {
        // Create a single shared ApiClient for all services
        this.sharedApiClient = new ApiClient();
        
        // Pass shared ApiClient to all services
        this.cardApiService = new CardApiService(sharedApiClient);
        this.bookApiService = new BookApiService(sharedApiClient);
        this.transactionApiService = new TransactionApiService(sharedApiClient);
        this.bookInventoryApiService = new BookInventoryApiService(sharedApiClient);
    }
    
    public static synchronized ApiServiceManager getInstance() {
        if (instance == null) {
            instance = new ApiServiceManager();
        }
        return instance;
    }
    
    public ApiClient getApiClient() {
        return sharedApiClient;
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
        return sharedApiClient.isServerAvailable();
    }
    
    /**
     * Set authentication token for all API services
     * Since all services share the same ApiClient, setting token once affects all
     */
    public void setAuthToken(String token) {
        sharedApiClient.setAuthToken(token);
        System.out.println("[ApiServiceManager] Auth token set for all services");
    }
    
    /**
     * Clear authentication token
     */
    public void clearAuthToken() {
        sharedApiClient.clearAuthToken();
        System.out.println("[ApiServiceManager] Auth token cleared");
    }
    
    /**
     * Get current auth token
     */
    public String getAuthToken() {
        return sharedApiClient.getAuthToken();
    }
}

