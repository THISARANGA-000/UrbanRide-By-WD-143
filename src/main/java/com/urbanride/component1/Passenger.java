package com.urbanride.component1;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passengers")
public class Passenger extends User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "passenger_seq")
    @SequenceGenerator(name = "passenger_seq", sequenceName = "passenger_sequence", allocationSize = 1)
    private int userId;

    @Override
    public int getUserId() { return userId; }
    @Override
    public void setUserId(int id) { this.userId = id; }

    private String homeAddress;
    private String workAddress;
    private String paymentMethod;
    private Double walletBalance = 0.0;
    private String passengerType = "Regular";
    
    // Saved Card Details
    private String cardNumber;
    private String cardExpiry;
    private String cardHolderName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "passenger_booking_history", joinColumns = @JoinColumn(name = "passenger_id"))
    @Column(name = "booking_id")
    private List<String> bookingHistory = new ArrayList<>();

    public Passenger() {}

    public Passenger(int userId, String name, String email, String password, String mobile) {
        super(userId, name, email, password, mobile, "passenger");
    }

    // Getters & Setters
    public String getHomeAddress() { return homeAddress; }
    public void setHomeAddress(String addr) { this.homeAddress = addr; }
    public String getWorkAddress() { return workAddress; }
    public void setWorkAddress(String addr) { this.workAddress = addr; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String method) { this.paymentMethod = method; }
    public Double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(Double bal) { this.walletBalance = bal; }
    public String getPassengerType() { return passengerType; }
    public void setPassengerType(String type) { this.passengerType = type; }
    public List<String> getBookingHistory() { return bookingHistory; }
    public void setBookingHistory(List<String> bookingHistory) { this.bookingHistory = bookingHistory; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardExpiry() { return cardExpiry; }
    public void setCardExpiry(String cardExpiry) { this.cardExpiry = cardExpiry; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public void topUpWallet(Double amount) { 
        if (this.walletBalance == null) this.walletBalance = 0.0;
        this.walletBalance += amount; 
    }
    
    public boolean deductWallet(Double amount) {
        if (this.walletBalance == null) this.walletBalance = 0.0;
        if (this.walletBalance >= amount) {
            this.walletBalance -= amount;
            return true;
        }
        return false;
    }
    public void addBooking(String bookingId) { bookingHistory.add(bookingId); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPasswordHash().equals(hashPassword(password));
    }

    @Override
    public void displayProfile() {
        System.out.println("=== Passenger: " + getName() + " | Wallet: LKR " + walletBalance + " ===");
    }
}

