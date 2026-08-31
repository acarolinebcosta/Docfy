package io.github.acarolinebcosta.docfy.file.application;

import io.github.acarolinebcosta.docfy.file.config.DocumentFileProperties;
import io.github.acarolinebcosta.docfy.shared.error.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

@Component
public class DocumentFileValidator {

    private static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "pdf", "application/pdf",
            "txt", "text/plain",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );

    private final long maximumSize;

    public DocumentFileValidator(DocumentFileProperties properties) {
        this.maximumSize = properties.maxSize().toBytes();
    }

    public ValidatedDocumentFile validate(UploadDocumentFileCommand command) {
        byte[] content = command.content();

        if (content == null || content.length == 0) {
            throw new BadRequestException("File must not be empty");
        }

        if (content.length > maximumSize) {
            throw new BadRequestException("File exceeds the maximum allowed size");
        }

        String filename = safeFilename(command.originalFilename());
        String extension = extension(filename);
        String expectedContentType = SUPPORTED_TYPES.get(extension);

        if (expectedContentType == null) {
            throw new BadRequestException("File type is not allowed");
        }

        String suppliedContentType = normalizeContentType(command.contentType());

        if (!expectedContentType.equals(suppliedContentType)
                || !contentMatches(extension, content)) {
            throw new BadRequestException(
                    "File content does not match its declared type"
            );
        }

        return new ValidatedDocumentFile(
                filename,
                expectedContentType,
                extension,
                content
        );
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File name is required");
        }

        String normalized = originalFilename.replace('\\', '/');
        String filename;

        try {
            filename = Path.of(normalized).getFileName().toString().trim();
        } catch (InvalidPathException exception) {
            throw new BadRequestException("File name is invalid");
        }

        if (filename.isBlank()
                || filename.length() > 255
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException(
                    "File name is invalid"
            );
        }

        return filename;
    }

    private String extension(String filename) {
        int separator = filename.lastIndexOf('.');

        if (separator < 1 || separator == filename.length() - 1) {
            return "";
        }

        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }

        return contentType
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private boolean contentMatches(String extension, byte[] content) {
        return switch (extension) {
            case "pdf" -> startsWith(
                    content,
                    "%PDF-".getBytes(StandardCharsets.US_ASCII)
            );
            case "png" -> startsWith(content, new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            });
            case "jpg", "jpeg" -> startsWith(content, new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            });
            case "txt" -> isUtf8Text(content);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }

        return true;
    }

    private boolean isUtf8Text(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return false;
            }
        }

        try {
            StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    public record ValidatedDocumentFile(
            String originalFilename,
            String contentType,
            String extension,
            byte[] content
    ) {
    }
}
