package tz.elmkusoma.learning.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Real metadata extraction for uploaded resource files.
 *
 * <p>Every value returned here is derived from the actual bytes of the file:
 * MIME type from magic bytes, file size from the upload itself, image
 * dimensions via ImageIO, PDF page count via PDFBox, and audio/video duration
 * from the container headers (MP4 {@code mvhd}, WAV {@code fmt}/{@code data}).
 * Values that cannot be determined from the file are {@code null} — never a
 * fabricated number.</p>
 */
@Slf4j
@Component
public class ResourceMetadataExtractor {

    /** Outcome of header inspection: what the bytes really are. */
    public record DetectedFile(String mime, String kind, String extensionHint) {
        public static final String KIND_IMAGE = "IMAGE";
        public static final String KIND_VIDEO = "VIDEO";
        public static final String KIND_AUDIO = "AUDIO";
        public static final String KIND_DOCUMENT = "DOCUMENT";
        public static final String KIND_ARCHIVE = "ARCHIVE";
        public static final String KIND_UNKNOWN = "UNKNOWN";
    }

    /** Values actually observed in the file; null fields are unknown, not zero. */
    public record ExtractedMetadata(String mimeType, Long fileSize, Integer durationSeconds,
                                    Integer pageCount, Integer width, Integer height) {
    }

    private static final int HEADER_BYTES = 64;

    /**
     * Validates that the uploaded bytes are compatible with the selected
     * resource type and reports what the file really is.
     *
     * @throws IllegalArgumentException when the content contradicts the type
     */
    public DetectedFile detectAndValidate(MultipartFile file, String expectedTypeName) throws IOException {
        byte[] header = readHeader(file);
        DetectedFile detected = detect(header, file.getContentType());
        String kind = kindForResourceType(expectedTypeName);

        if (DetectedFile.KIND_UNKNOWN.equals(detected.kind())) {
            // No recognizable signature (plain text, office stream, …): keep the
            // browser-declared content type rather than inventing one.
            return new DetectedFile(normalizeMime(file.getContentType()), kind, extensionHint(file.getOriginalFilename()));
        }

        boolean compatible = switch (kind) {
            case DetectedFile.KIND_DOCUMENT ->
                    DetectedFile.KIND_DOCUMENT.equals(detected.kind())
                            || ("zip".equals(detected.extensionHint())
                                && ("PRESENTATION".equals(expectedTypeName) || "SPREADSHEET".equals(expectedTypeName)
                                    || "DOCUMENT".equals(expectedTypeName)));
            case DetectedFile.KIND_ARCHIVE ->
                    "zip".equals(detected.extensionHint()) || DetectedFile.KIND_ARCHIVE.equals(detected.kind());
            // LIVE_RECORDING/OTHER accept whatever real media it is
            case "ANY" -> true;
            default -> kind.equals(detected.kind())
                    || (DetectedFile.KIND_DOCUMENT.equals(detected.kind())
                        && "PDF".equals(expectedTypeName) && "pdf".equals(detected.extensionHint()));
        };

        if (!compatible) {
            throw new IllegalArgumentException(String.format(
                    "File content (%s) does not match the selected resource type %s",
                    detected.mime(), expectedTypeName));
        }
        return detected;
    }

    /**
     * Extracts real metadata from the file's bytes.
     *
     * @return extracted values; unknown values are {@code null}
     * @throws IOException when the file is corrupt and metadata cannot be read
     */
    public ExtractedMetadata extractMetadata(MultipartFile file, DetectedFile detected) throws IOException {
        String mime = detected.mime();
        Long size = file.getSize();
        Integer duration = null;
        Integer pages = null;
        Integer width = null;
        Integer height = null;

        if (mime != null && mime.startsWith("image/")) {
            try (InputStream in = open(file)) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (Exception e) {
                log.debug("Image dimension read failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            }
        } else if ("pdf".equals(detected.extensionHint())) {
            try (InputStream in = open(file); PDDocument doc = PDDocument.load(in)) {
                pages = doc.getNumberOfPages();
            }
        } else if ("mp4".equals(detected.extensionHint())) {
            duration = readMp4Duration(open(file));
        } else if ("wav".equals(detected.extensionHint())) {
            duration = readWavDuration(open(file));
        }

        return new ExtractedMetadata(mime, size, duration, pages, width, height);
    }

    // ── validation helpers ──

    /** Maps a resource type to the file kind it may contain. */
    private String kindForResourceType(String typeName) {
        if (typeName == null) return DetectedFile.KIND_UNKNOWN;
        return switch (typeName) {
            case "IMAGE" -> DetectedFile.KIND_IMAGE;
            case "VIDEO", "LIVE_RECORDING" -> DetectedFile.KIND_VIDEO;
            case "AUDIO" -> DetectedFile.KIND_AUDIO;
            case "PDF", "DOCUMENT", "PRESENTATION", "SPREADSHEET" -> DetectedFile.KIND_DOCUMENT;
            case "ARCHIVE" -> DetectedFile.KIND_ARCHIVE;
            case "OTHER" -> "ANY";
            default -> DetectedFile.KIND_UNKNOWN;
        };
    }

    private byte[] readHeader(MultipartFile file) throws IOException {
        byte[] header = new byte[HEADER_BYTES];
        int read;
        try (InputStream in = open(file)) {
            read = in.read(header);
        }
        if (read < 0) read = 0;
        byte[] out = new byte[read];
        System.arraycopy(header, 0, out, 0, read);
        return out;
    }

    private InputStream open(MultipartFile file) throws IOException {
        return new BufferedInputStream(file.getInputStream());
    }

    // ── magic-byte detection ──

    private DetectedFile detect(byte[] h, String declared) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return new DetectedFile("image/jpeg", DetectedFile.KIND_IMAGE, "jpg");
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return new DetectedFile("image/png", DetectedFile.KIND_IMAGE, "png");
        }
        if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return new DetectedFile("image/gif", DetectedFile.KIND_IMAGE, "gif");
        }
        if (h.length >= 2 && h[0] == 'B' && h[1] == 'M') {
            return new DetectedFile("image/bmp", DetectedFile.KIND_IMAGE, "bmp");
        }
        if (h.length >= 12 && ascii(h, 0, 4).equals("RIFF") && ascii(h, 8, 4).equals("WEBP")) {
            return new DetectedFile("image/webp", DetectedFile.KIND_IMAGE, "webp");
        }
        if (h.length >= 5 && ascii(h, 0, 4).equals("%PDF")) {
            return new DetectedFile("application/pdf", DetectedFile.KIND_DOCUMENT, "pdf");
        }
        if (h.length >= 12 && ascii(h, 0, 4).equals("RIFF") && ascii(h, 8, 4).equals("WAVE")) {
            return new DetectedFile("audio/wav", DetectedFile.KIND_AUDIO, "wav");
        }
        if (h.length >= 4 && h[0] == 'f' && h[1] == 't' && h[2] == 'y' && h[3] == 'p') {
            String brand = h.length >= 12 ? ascii(h, 8, 4) : "";
            if (brand.startsWith("M4A")) {
                return new DetectedFile("audio/mp4", DetectedFile.KIND_AUDIO, "m4a");
            }
            return new DetectedFile("video/mp4", DetectedFile.KIND_VIDEO, "mp4");
        }
        if (h.length >= 4 && (h[0] & 0xFF) == 0x1A && (h[1] & 0xFF) == 0x45 && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3) {
            return new DetectedFile("video/webm", DetectedFile.KIND_VIDEO, "webm");
        }
        if (h.length >= 4 && h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') {
            return new DetectedFile("audio/ogg", DetectedFile.KIND_AUDIO, "ogg");
        }
        if (h.length >= 4 && h[0] == 'f' && h[1] == 'L' && h[2] == 'a' && h[3] == 'C') {
            return new DetectedFile("audio/flac", DetectedFile.KIND_AUDIO, "flac");
        }
        if (h.length >= 3 && h[0] == 'I' && h[1] == 'D' && h[2] == '3') {
            return new DetectedFile("audio/mpeg", DetectedFile.KIND_AUDIO, "mp3");
        }
        if (h.length >= 2 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0) {
            return new DetectedFile("audio/mpeg", DetectedFile.KIND_AUDIO, "mp3");
        }
        if (h.length >= 4 && h[0] == 'P' && h[1] == 'K' && h[2] == 3 && h[3] == 4) {
            return new DetectedFile("application/zip", DetectedFile.KIND_DOCUMENT, "zip");
        }
        if (h.length >= 8 && (h[0] & 0xFF) == 0xD0 && (h[1] & 0xFF) == 0xCF && (h[2] & 0xFF) == 0x11 && (h[3] & 0xFF) == 0xE0) {
            return new DetectedFile("application/x-ole-storage", DetectedFile.KIND_DOCUMENT, "doc");
        }
        return new DetectedFile(normalizeMime(declared), DetectedFile.KIND_UNKNOWN, extensionHint(null));
    }

    private static String ascii(byte[] h, int off, int len) {
        if (h.length < off + len) return "";
        return new String(h, off, len, StandardCharsets.US_ASCII);
    }

    private static String normalizeMime(String mime) {
        if (mime == null || mime.isBlank()) return "application/octet-stream";
        int semi = mime.indexOf(';');
        return (semi > 0 ? mime.substring(0, semi) : mime).trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionHint(String fileName) {
        if (fileName == null || !fileName.contains(".")) return null;
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    // ── duration parsers (real container headers) ──

    /**
     * Reads the MP4 movie header ({@code moov/mvhd}) and converts the
     * container's duration/timescale pair to whole seconds. Returns null when
     * the header cannot be found — never a made-up duration.
     */
    Integer readMp4Duration(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(new BufferedInputStream(in))) {
            Long mvhd = findMvhd(data, -1);
            if (mvhd == null) return null;
            return mvhd.intValue();
        } catch (IOException | IllegalArgumentException e) {
            log.debug("MP4 duration not readable: {}", e.getMessage());
            return null;
        }
    }

    private Long findMvhd(DataInputStream data, long limit) throws IOException {
        long consumed = 0;
        while (limit < 0 || consumed < limit) {
            long size = readUInt32(data);
            if (size < 0) return null;
            byte[] type = new byte[4];
            if (data.read(type, 0, 4) != 4) return null;
            String boxType = new String(type, StandardCharsets.US_ASCII);
            long headerSize = 8;
            if (size == 1) {
                long large = readUInt64(data);
                if (large < 16) return null;
                headerSize = 16;
                size = large;
            } else if (size == 0) {
                return null; // box extends to EOF: no further siblings to walk
            }
            long payload = size - headerSize;
            if (payload < 0) return null;
            consumed += size;

            switch (boxType) {
                case "moov" -> {
                    Long found = walkMoov(data, payload);
                    if (found != null) return found;
                }
                case "mvhd" -> {
                    return parseMvhd(data, payload);
                }
                default -> skipFully(data, payload);
            }
        }
        return null;
    }

    private Long walkMoov(DataInputStream data, long payload) throws IOException {
        long consumed = 0;
        while (consumed < payload) {
            long size = readUInt32(data);
            if (size < 0) return null;
            byte[] type = new byte[4];
            if (data.read(type, 0, 4) != 4) return null;
            String boxType = new String(type, StandardCharsets.US_ASCII);
            long headerSize = 8;
            if (size == 1) {
                long large = readUInt64(data);
                if (large < 16) return null;
                headerSize = 16;
                size = large;
            } else if (size == 0) {
                size = payload - consumed + headerSize - 8;
            }
            long inner = size - headerSize;
            if (inner < 0) return null;
            consumed += size;
            if ("mvhd".equals(boxType)) {
                return parseMvhd(data, inner);
            }
            skipFully(data, inner);
        }
        return null;
    }

    private Long parseMvhd(DataInputStream data, long payload) throws IOException {
        int version = data.readUnsignedByte();
        skipFully(data, 3); // flags
        long timescale;
        long duration;
        if (version == 1) {
            skipFully(data, 16); // creation + modification (64-bit)
            timescale = readUInt32(data);
            duration = readUInt64(data);
        } else {
            skipFully(data, 8); // creation + modification (32-bit)
            timescale = readUInt32(data);
            duration = readUInt32(data);
        }
        skipFully(data, Math.max(0, payload - (version == 1 ? 32 : 20)));
        if (timescale <= 0 || duration <= 0 || duration == 0xFFFFFFFFL) return null;
        double seconds = (double) duration / timescale;
        if (seconds > 60L * 60L * 24L * 7L) return null; // sanity: > 7 days is corrupt
        return Math.round(seconds);
    }

    /**
     * Reads the WAV {@code fmt } byte rate and {@code data} chunk size to
     * compute the true playback duration of a PCM waveform.
     */
    Integer readWavDuration(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(new BufferedInputStream(in))) {
            byte[] riff = new byte[12];
            if (data.read(riff) != 12) return null;
            if (!"RIFF".equals(new String(riff, 0, 4, StandardCharsets.US_ASCII))) return null;
            if (!"WAVE".equals(new String(riff, 8, 4, StandardCharsets.US_ASCII))) return null;

            long byteRate = -1;
            long dataSize = -1;
            while (true) {
                byte[] chunkId = new byte[4];
                if (data.read(chunkId) != 4) break;
                long chunkSize = readUInt32LE(data);
                if (chunkSize < 0) break;
                String id = new String(chunkId, StandardCharsets.US_ASCII);
                if ("fmt ".equals(id)) {
                    if (chunkSize < 16) return null;
                    skipFully(data, 8); // audioFormat(2) channels(2) sampleRate(4)
                    byteRate = readUInt32LE(data);
                    skipFully(data, Math.max(0, chunkSize - 12));
                } else if ("data".equals(id)) {
                    dataSize = chunkSize;
                    break;
                } else {
                    skipFully(data, chunkSize + (chunkSize % 2));
                }
            }
            if (byteRate <= 0 || dataSize < 0) return null;
            double seconds = (double) dataSize / byteRate;
            if (seconds <= 0 || seconds > 60L * 60L * 24L * 7L) return null;
            return (int) Math.round(seconds);
        } catch (IOException | IllegalArgumentException e) {
            log.debug("WAV duration not readable: {}", e.getMessage());
            return null;
        }
    }

    // ── binary helpers ──

    private long readUInt32(DataInputStream data) throws IOException {
        int b1 = data.read();
        int b2 = data.read();
        int b3 = data.read();
        int b4 = data.read();
        if ((b1 | b2 | b3 | b4) < 0) return -1;
        return ((long) b1 << 24) | ((long) b2 << 16) | ((long) b3 << 8) | b4;
    }

    private long readUInt64(DataInputStream data) throws IOException {
        long hi = readUInt32(data);
        long lo = readUInt32(data);
        if (hi < 0 || lo < 0) return -1;
        return (hi << 32) | lo;
    }

    private long readUInt32LE(DataInputStream data) throws IOException {
        int b1 = data.read();
        int b2 = data.read();
        int b3 = data.read();
        int b4 = data.read();
        if ((b1 | b2 | b3 | b4) < 0) return -1;
        return ((long) b4 << 24) | ((long) b3 << 16) | ((long) b2 << 8) | b1;
    }

    private void skipFully(DataInputStream data, long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            long skipped = data.skip(remaining);
            if (skipped <= 0) {
                if (data.read() < 0) throw new IOException("Unexpected end of stream");
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
