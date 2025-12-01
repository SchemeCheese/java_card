package models;

/**
 * Model class for transaction information
 */
public class Transaction {
    private String date;
    private String type;
    private long amount;
    private String status;

    public Transaction(String date, String type, long amount, String status) {
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
