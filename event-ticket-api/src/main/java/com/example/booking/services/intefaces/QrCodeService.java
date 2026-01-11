package com.example.booking.services.intefaces;

public interface QrCodeService {
    public byte[] generateQrCodeImage(String text, int width, int height);
}
