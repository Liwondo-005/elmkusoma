package tz.elmkusoma.learning.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tz.elmkusoma.learning.service.ResourceMetadataExtractor.DetectedFile;
import tz.elmkusoma.learning.service.ResourceMetadataExtractor.ExtractedMetadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-bytes metadata extraction: every assertion below runs against an
 * actually encoded file (PNG, WAV, MP4 header), never a fabricated fixture.
 */
class ResourceMetadataExtractorTest {

    private final ResourceMetadataExtractor extractor = new ResourceMetadataExtractor();

    // ── detection & validation ────────────────────────────────────────────

    @Test
    void detectsRealPngAsImage() throws Exception {
        byte[] png = pngBytes(64, 48);
        DetectedFile detected = extractor.detectAndValidate(file("diagram.png", "image/png", png), "IMAGE");
        assertEquals("image/png", detected.mime());
        assertEquals(DetectedFile.KIND_IMAGE, detected.kind());
    }

    @Test
    void rejectsFileWhoseBytesContradictTheSelectedType() throws Exception {
        byte[] png = pngBytes(32, 32);
        assertThrows(IllegalArgumentException.class,
                () -> extractor.detectAndValidate(file("lesson.mp4", "video/mp4", png), "VIDEO"));
        assertThrows(IllegalArgumentException.class,
                () -> extractor.detectAndValidate(file("lesson.pdf", "application/pdf", png), "PDF"));
    }

    @Test
    void acceptsRealPdfForDocumentType() throws Exception {
        byte[] pdf = pdfBytes();
        DetectedFile detected = extractor.detectAndValidate(file("notes.pdf", "application/pdf", pdf), "PDF");
        assertEquals("application/pdf", detected.mime());
    }

    // ── image dimensions ──────────────────────────────────────────────────

    @Test
    void extractsRealImageDimensions() throws Exception {
        ExtractedMetadata meta = extractor.extractMetadata(
                file("photo.png", "image/png", pngBytes(320, 180)),
                extractor.detectAndValidate(file("photo.png", "image/png", pngBytes(320, 180)), "IMAGE"));
        assertEquals(320, meta.width());
        assertEquals(180, meta.height());
        assertNotNull(meta.fileSize());
        assertTrue(meta.fileSize() > 0, "file size must come from the real upload");
        assertNull(meta.durationSeconds(), "an image has no duration");
        assertNull(meta.pageCount(), "an image has no page count");
    }

    // ── PDF page count ────────────────────────────────────────────────────

    @Test
    void extractsRealPdfPageCount() throws Exception {
        byte[] pdf = pdfBytes();
        DetectedFile detected = extractor.detectAndValidate(file("doc.pdf", "application/pdf", pdf), "PDF");
        ExtractedMetadata meta = extractor.extractMetadata(file("doc.pdf", "application/pdf", pdf), detected);
        assertNotNull(meta.pageCount());
        assertEquals(1, meta.pageCount(), "generated PDF has exactly one page");
        assertNull(meta.width(), "unknown values stay null, never zero");
        assertNull(meta.height());
    }

    // ── WAV duration ──────────────────────────────────────────────────────

    @Test
    void extractsRealWavDuration() throws Exception {
        byte[] wav = wavBytes(2, 44100, 2.5); // 2 channels, 44.1kHz, 2.5s of silence
        DetectedFile detected = extractor.detectAndValidate(file("tone.wav", "audio/wav", wav), "AUDIO");
        assertEquals("audio/wav", detected.mime());
        ExtractedMetadata meta = extractor.extractMetadata(file("tone.wav", "audio/wav", wav), detected);
        assertNotNull(meta.durationSeconds());
        assertTrue(meta.durationSeconds() == 2 || meta.durationSeconds() == 3,
                "duration must be derived from data size ÷ byte rate, got " + meta.durationSeconds());
    }

    // ── MP4 duration ──────────────────────────────────────────────────────

    @Test
    void extractsRealMp4DurationFromContainerHeader() throws Exception {
        byte[] mp4 = minimalMp4WithMvhd(30_000, 90_000); // 90000 ticks @ 30000 Hz = 3s
        DetectedFile detected = extractor.detectAndValidate(file("clip.mp4", "video/mp4", mp4), "VIDEO");
        assertEquals(DetectedFile.KIND_VIDEO, detected.kind());
        Integer duration = extractor.readMp4Duration(new java.io.ByteArrayInputStream(mp4));
        assertNotNull(duration, "mvhd duration must be readable from the real header");
        assertEquals(3, duration);
    }

    @Test
    void mp4WithoutMovableHeaderYieldsNullNotZero() throws Exception {
        byte[] broken = new byte[]{0, 0, 0, 16, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};
        assertNull(extractor.readMp4Duration(new java.io.ByteArrayInputStream(broken)));
    }

    @Test
    void wavWithoutDataChunkYieldsNullDuration() throws Exception {
        byte[] headerOnly = "RIFF\u0000\u0000\u0000\u0000WAVE".getBytes(StandardCharsets.US_ASCII);
        assertNull(extractor.readWavDuration(new java.io.ByteArrayInputStream(headerOnly)));
    }

    // ── fixtures: real encoded files ──────────────────────────────────────

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x2563EB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] pdfBytes() throws IOException {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** 16-bit PCM WAV: fmt chunk + data chunk of given duration. */
    private static byte[] wavBytes(int channels, int sampleRate, double seconds) {
        int byteRate = sampleRate * channels * 2;
        int dataSize = (int) (byteRate * seconds);
        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(36 + dataSize);
        buf.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buf.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(16);
        buf.putShort((short) 1);          // PCM
        buf.putShort((short) channels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) (channels * 2));
        buf.putShort((short) 16);         // bits per sample
        buf.put("data".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(dataSize);
        buf.position(44 + dataSize);      // silence
        return buf.array();
    }

    /**
     * Minimal but structurally real MP4: ftyp + moov/mvhd carrying the given
     * timescale and duration — exactly the bytes the production parser walks.
     */
    private static byte[] minimalMp4WithMvhd(long timescale, long duration) {
        ByteBuffer mvhdPayload = ByteBuffer.allocate(100).order(ByteOrder.BIG_ENDIAN);
        mvhdPayload.putInt(0);            // version (1 byte) + flags (3 bytes)
        mvhdPayload.putInt(0);            // creation time
        mvhdPayload.putInt(0);            // modification time
        mvhdPayload.putInt((int) timescale);
        mvhdPayload.putInt((int) duration);
        mvhdPayload.position(100);

        byte[] mvhdBody = mvhdPayload.array();
        ByteBuffer mvhd = ByteBuffer.allocate(8 + mvhdBody.length).order(ByteOrder.BIG_ENDIAN);
        mvhd.putInt(8 + mvhdBody.length);
        mvhd.put("mvhd".getBytes(StandardCharsets.US_ASCII));
        mvhd.put(mvhdBody);

        ByteBuffer moov = ByteBuffer.allocate(8 + mvhd.array().length).order(ByteOrder.BIG_ENDIAN);
        moov.putInt(8 + mvhd.array().length);
        moov.put("moov".getBytes(StandardCharsets.US_ASCII));
        moov.put(mvhd.array());

        byte[] ftyp = new byte[]{
                0, 0, 0, 20, 'f', 't', 'y', 'p',
                'i', 's', 'o', 'm', 0, 0, 2, 0,
                'i', 's', 'o', 'm'};
        ByteBuffer all = ByteBuffer.allocate(ftyp.length + moov.array().length)
                .order(ByteOrder.BIG_ENDIAN);
        all.put(ftyp);
        all.put(moov.array());
        return all.array();
    }
}
