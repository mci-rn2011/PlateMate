package at.platemate.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UploadStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveRestaurantImageStoresFileUnderRestaurantDirectoryAndDeletesOldUpload() throws Exception {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));
        Path oldFile = tempDir.resolve("restaurants").resolve("42").resolve("old.png");
        Files.createDirectories(oldFile.getParent());
        Files.writeString(oldFile, "old image");
        byte[] bytes = "new image".getBytes(StandardCharsets.UTF_8);

        String publicPath = service.saveRestaurantImage(
                42L,
                "menu.PNG",
                "image/png",
                bytes.length,
                new ByteArrayInputStream(bytes),
                "/uploads/restaurants/42/old.png");

        assertThat(publicPath).startsWith("/uploads/restaurants/42/");
        assertThat(publicPath).endsWith(".png");
        Path storedFile = tempDir.resolve(publicPath.substring("/uploads/".length()));
        assertThat(storedFile).exists();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(bytes);
        assertThat(oldFile).doesNotExist();
    }

    @Test
    void saveDeliveryProofStoresFileUnderDeliveryDirectory() throws Exception {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));
        byte[] bytes = "<svg/>".getBytes(StandardCharsets.UTF_8);

        String publicPath = service.saveDeliveryProof(
                7L,
                "proof.svg",
                "image/svg+xml",
                bytes.length,
                new ByteArrayInputStream(bytes),
                null);

        assertThat(publicPath).startsWith("/uploads/deliveries/7/");
        assertThat(publicPath).endsWith(".svg");
        assertThat(Files.readString(tempDir.resolve(publicPath.substring("/uploads/".length()))))
                .isEqualTo("<svg/>");
    }

    @Test
    void saveRestaurantImageRejectsMissingRestaurantIdBeforeWriting() {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));

        assertThatThrownBy(() -> service.saveRestaurantImage(
                null,
                "menu.png",
                "image/png",
                4,
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Restaurant id is required before saving uploads.");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void saveRestaurantImageRejectsUnsupportedContentType() {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));

        assertThatThrownBy(() -> service.saveRestaurantImage(
                42L,
                "menu.png",
                "text/plain",
                4,
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported upload content type.");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void saveRestaurantImageRejectsUnsupportedExtension() {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));

        assertThatThrownBy(() -> service.saveRestaurantImage(
                42L,
                "menu.gif",
                "image/png",
                4,
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported upload file extension.");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void saveRestaurantImageRejectsOversizedUpload() {
        UploadStorageService service = new UploadStorageService(uploadProperties(3));

        assertThatThrownBy(() -> service.saveRestaurantImage(
                42L,
                "menu.png",
                "image/png",
                4,
                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Upload exceeds the configured file size limit.");
        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void deleteIfUploadedIgnoresNonUploadAndTraversalPaths() {
        UploadStorageService service = new UploadStorageService(uploadProperties(1_024));

        service.deleteIfUploaded("/images/menu.png");
        service.deleteIfUploaded("/uploads/../outside.png");

        assertThat(tempDir).isEmptyDirectory();
    }

    private UploadProperties uploadProperties(long maxBytes) {
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setRoot(tempDir);
        uploadProperties.setMaxBytes(maxBytes);
        return uploadProperties;
    }
}
