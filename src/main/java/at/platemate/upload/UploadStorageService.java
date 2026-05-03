package at.platemate.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "svg");

    private final UploadProperties uploadProperties;
    private final Path root;

    public UploadStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
        this.root = uploadProperties.getRoot().toAbsolutePath().normalize();
    }

    public String saveRestaurantImage(Long restaurantId, MultipartFile file, String oldPublicPath) {
        if (restaurantId == null) {
            throw new IllegalArgumentException("Restaurant id is required before saving uploads.");
        }
        validate(file);

        String extension = extensionOf(file.getOriginalFilename());
        return saveRestaurantImage(
                restaurantId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                inputStream(file),
                oldPublicPath);
    }

    public String saveRestaurantImage(
            Long restaurantId,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream,
            String oldPublicPath) {
        if (restaurantId == null) {
            throw new IllegalArgumentException("Restaurant id is required before saving uploads.");
        }
        validate(originalFilename, contentType, size, inputStream);

        String extension = extensionOf(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;
        Path restaurantDir = root.resolve("restaurants").resolve(restaurantId.toString()).normalize();
        Path target = restaurantDir.resolve(filename).normalize();

        if (!target.startsWith(restaurantDir) || !restaurantDir.startsWith(root)) {
            throw new IllegalArgumentException("Invalid upload target.");
        }

        try {
            Files.createDirectories(restaurantDir);
            try (inputStream) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded file.", ex);
        }

        deleteIfUploaded(oldPublicPath);
        return "/uploads/restaurants/" + restaurantId + "/" + filename;
    }

    public String saveDeliveryProof(
            Long deliveryId,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream,
            String oldPublicPath) {
        if (deliveryId == null) {
            throw new IllegalArgumentException("Delivery id is required before saving proof uploads.");
        }
        validate(originalFilename, contentType, size, inputStream);

        String extension = extensionOf(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;
        Path deliveryDir = root.resolve("deliveries").resolve(deliveryId.toString()).normalize();
        Path target = deliveryDir.resolve(filename).normalize();

        if (!target.startsWith(deliveryDir) || !deliveryDir.startsWith(root)) {
            throw new IllegalArgumentException("Invalid upload target.");
        }

        try {
            Files.createDirectories(deliveryDir);
            try (inputStream) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded file.", ex);
        }

        deleteIfUploaded(oldPublicPath);
        return "/uploads/deliveries/" + deliveryId + "/" + filename;
    }

    public void deleteIfUploaded(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return;
        }

        Path target = root.resolve(publicPath.substring("/uploads/".length())).normalize();
        if (!target.startsWith(root)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete uploaded file.", ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is required.");
        }
        validate(file.getOriginalFilename(), file.getContentType(), file.getSize(), true);
    }

    private void validate(String originalFilename, String contentType, long size, InputStream inputStream) {
        validate(originalFilename, contentType, size, inputStream != null);
    }

    private void validate(String originalFilename, String contentType, long size, boolean hasInputStream) {
        if (!hasInputStream) {
            throw new IllegalArgumentException("Upload file is required.");
        }
        if (size > uploadProperties.getMaxBytes()) {
            throw new IllegalArgumentException("Upload exceeds the configured file size limit.");
        }
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported upload content type.");
        }
        extensionOf(originalFilename);
    }

    private InputStream inputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read uploaded file.", ex);
        }
    }

    private String extensionOf(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("Upload file extension is required.");
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported upload file extension.");
        }
        return extension;
    }
}
