package com.urbanride.component5;

import com.urbanride.component3.Booking;

public class PaymentManager {
    // This represents the payment and transaction logic from the running project
    public static void processFare(Booking booking) {
        System.out.println(String.format("Processing payment for Booking ID: B%03d", booking.getBookingId()));
        System.out.println("Fare amount: LKR " + booking.getFare());
        System.out.println("Payment Method: " + booking.getPaymentMethod());
        // In the running project, this logic is handled dynamically in BookingController
    }
}

