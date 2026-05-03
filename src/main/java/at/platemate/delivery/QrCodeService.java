package at.platemate.delivery;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 240;

    public String createPngDataUrl(String payload) {
        if (payload == null || payload.isBlank()) {
            return "";
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            var hints = java.util.Map.of(EncodeHintType.MARGIN, 1);
            var matrix = writer.encode(payload.trim(), BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate QR code.", ex);
        }
    }
}
