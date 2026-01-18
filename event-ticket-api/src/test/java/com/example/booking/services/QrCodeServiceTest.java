package com.example.booking.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @InjectMocks
    private QrCodeServiceImpl qrCodeService;

    private static final byte[] PNG_HEADER = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    @DisplayName("Should generate QR Code image bytes successfully when input is valid")
    void generateQrCodeImage_ShouldReturnPngBytes_WhenInputIsValid() {
        String text = "VALID_TOKEN_123";
        int width = 200;
        int height = 200;

        byte[] result = qrCodeService.generateQrCodeImage(text, width, height);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        assertThat(result).startsWith(PNG_HEADER);
    }

    @Test
    @DisplayName("Should throw RuntimeException when text is empty (ZXing restriction)")
    void generateQrCodeImage_ShouldThrowException_WhenTextIsEmpty() {
        String emptyText = "";
        int width = 200;
        int height = 200;

        assertThatThrownBy(() -> qrCodeService.generateQrCodeImage(emptyText, width, height))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generating QR code");
    }

    @Test
    @DisplayName("Should throw RuntimeException when text is null")
    void generateQrCodeImage_ShouldThrowException_WhenTextIsNull() {
        assertThatThrownBy(() -> qrCodeService.generateQrCodeImage(null, 200, 200))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generating QR code");
    }

    @Test
    @DisplayName("Should throw RuntimeException when dimensions are invalid (negative)")
    void generateQrCodeImage_ShouldThrowException_WhenDimensionsAreInvalid() {
        String text = "Valid Text";
        int width = -100;
        int height = 200;

        assertThatThrownBy(() -> qrCodeService.generateQrCodeImage(text, width, height))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generating QR code");
    }
}
