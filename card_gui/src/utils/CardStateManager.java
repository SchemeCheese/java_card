package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import models.CardInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager để lưu/load applet state vào JSON file
 * Dùng cho JCardSim demo (vì không có persistent storage)
 */
public class CardStateManager {
    private static final String STATE_DIR = "card_states";
    private static final String STATE_FILE_PREFIX = "card_state_";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Card state data structure - Mô phỏng EEPROM của JavaCard
     * Lưu đầy đủ dữ liệu để tái tạo lại toàn bộ applet state
     */
    public static class CardState {
        public String studentId;
        
        // RSA Keypair (Mô phỏng EEPROM - lưu cả public và private components)
        public String rsaPublicKeyPEM;  // RSA public key (PEM format)
        public String rsaModulusBase64;  // RSA modulus (Base64) - để restore keypair
        public String rsaExponentBase64; // RSA public exponent (Base64) - để restore keypair
        public String rsaPrivateExponentBase64; // RSA private exponent (d) - [DEMO ONLY] Mô phỏng EEPROM
        
        // PIN State
        public String pinHash;          // PIN hash (Base64) - nếu có
        public String pinSalt;          // PIN salt (Base64) - nếu có
        public String pinPlainText;     // PIN plain text (để regenerate hash/salt)
        public int pinTriesRemaining;
        
        // Card Info trên Applet (CardInfoManager)
        public String cardId;           // Card ID (10 bytes max)
        public String holderName;       // Holder name (50 bytes max)
        public String expiryDate;       // Expiry date (DDMMYYYY format)
        public boolean cardInfoEncrypted; // Flag: card info có được mã hóa không
        
        // Borrowed Books trên Applet (BookManager)
        public String[] borrowedBookIds; // Danh sách Book IDs trên applet
        
        // Balance (nếu có trên applet)
        public long balance;
        
        // Full CardInfo (cho GUI và server sync)
        public CardInfo cardInfo;       // Card information (full)
        
        // Metadata
        public long lastUpdated;        // Timestamp
        public String version;          // Version của state format
        
        public CardState() {
            this.lastUpdated = System.currentTimeMillis();
            this.version = "1.0";
            this.pinTriesRemaining = 3;
            this.balance = 0;
            this.cardInfoEncrypted = false;
            this.borrowedBookIds = new String[0];
        }
    }
    
    /**
     * Ensure state directory exists
     */
    private static void ensureDirectory() {
        File dir = new File(STATE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Get state file path for a student ID
     */
    private static String getStateFilePath(String studentId) {
        ensureDirectory();
        return STATE_DIR + File.separator + STATE_FILE_PREFIX + studentId + ".json";
    }
    
    /**
     * Save card state to JSON file
     */
    public static boolean saveCardState(String studentId, CardState state) {
        try {
            state.studentId = studentId;
            state.lastUpdated = System.currentTimeMillis();
            
            String filePath = getStateFilePath(studentId);
            String json = gson.toJson(state);
            
            Files.write(Paths.get(filePath), json.getBytes("UTF-8"));
            System.out.println("Saved card state for: " + studentId);
            return true;
        } catch (Exception e) {
            System.err.println("Error saving card state: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load card state from JSON file
     */
    public static CardState loadCardState(String studentId) {
        try {
            String filePath = getStateFilePath(studentId);
            File file = new File(filePath);
            
            if (!file.exists()) {
                return null;
            }
            
            String json = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
            CardState state = gson.fromJson(json, CardState.class);
            System.out.println("Loaded card state for: " + studentId);
            return state;
        } catch (Exception e) {
            System.err.println("Error loading card state: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Delete card state file
     */
    public static boolean deleteCardState(String studentId) {
        try {
            String filePath = getStateFilePath(studentId);
            File file = new File(filePath);
            if (file.exists()) {
                return file.delete();
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting card state: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if card state exists
     */
    public static boolean cardStateExists(String studentId) {
        String filePath = getStateFilePath(studentId);
        return new File(filePath).exists();
    }
    
    /**
     * Get all saved card states
     */
    public static Map<String, CardState> getAllCardStates() {
        Map<String, CardState> states = new HashMap<>();
        try {
            ensureDirectory();
            File dir = new File(STATE_DIR);
            File[] files = dir.listFiles((d, name) -> name.startsWith(STATE_FILE_PREFIX) && name.endsWith(".json"));
            
            if (files != null) {
                for (File file : files) {
                    try {
                        String json = new String(Files.readAllBytes(file.toPath()), "UTF-8");
                        CardState state = gson.fromJson(json, CardState.class);
                        if (state != null && state.studentId != null) {
                            states.put(state.studentId, state);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading state from file: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting all card states: " + e.getMessage());
        }
        return states;
    }
}

