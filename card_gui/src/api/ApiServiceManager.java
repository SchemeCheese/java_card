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
}

