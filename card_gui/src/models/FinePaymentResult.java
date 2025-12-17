package models;

/**
 * Result model for paying outstanding fines via server API.
 */
public class FinePaymentResult {
    private final long totalPaid;
    private final int paidCount;
    private final long balanceAfter;
    private final String message;

    public FinePaymentResult(long totalPaid, int paidCount, long balanceAfter, String message) {
        this.totalPaid = totalPaid;
        this.paidCount = paidCount;
        this.balanceAfter = balanceAfter;
        this.message = message;
    }

    public long getTotalPaid() {
        return totalPaid;
    }

    public int getPaidCount() {
        return paidCount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public String getMessage() {
        return message;
    }
}
