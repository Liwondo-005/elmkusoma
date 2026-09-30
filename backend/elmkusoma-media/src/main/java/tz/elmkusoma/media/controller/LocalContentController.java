package tz.elmkusoma.media.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Byte transport for the local storage backend (equivalent of MinIO
 * presigned GET/PUT).  Download URLs are short-lived HMAC-signed URLs issued by
 * the existing storage abstraction; when a signature is present it is verified
 * strictly (and expiry enforced).  Unsigned references behave like the plain
 * object URLs the MinIO backend returns — the resource access decision itself
 * always happens upstream, in core, before any URL is ever issued.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.storage", havingValue = "local")
public class LocalContentController {

    private final tz.elmkusoma.media.service.impl.LocalStorageService storageService;

    @GetMapping("/local-content")
    public void getContent(@RequestParam("key") String key,
                           @RequestParam(value = "exp", required = false) Long exp,
                           @RequestParam(value = "sig", required = false) String sig,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        if (exp != null || sig != null) {
            if (exp == null || sig == null || exp < Instant.now().getEpochSecond()
                    || !storageService.verifySignature(key, exp, sig)) {
                writeError(response, HttpStatus.FORBIDDEN, "URL expired or invalid");
                return;
            }
        }

        Path file = storageService.resolveForController(key);
        if (!Files.isRegularFile(file)) {
            writeError(response, HttpStatus.NOT_FOUND, "Object not found");
            return;
        }

        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=300");

        long fileSize = Files.size(file);
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            long start = 0;
            long end = fileSize - 1;
            String spec = rangeHeader.substring("bytes=".length()).split(",")[0].trim();
            int dash = spec.indexOf('-');
            if (dash >= 0) {
                String left = spec.substring(0, dash);
                String right = spec.substring(dash + 1);
                if (!left.isEmpty()) {
                    start = Long.parseLong(left);
                }
                if (!right.isEmpty()) {
                    end = Long.parseLong(right);
                }
            }
            if (start < 0 || start >= fileSize || end < start) {
                response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
                return;
            }
            end = Math.min(end, fileSize - 1);
            long length = end - start + 1;
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setHeader(HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileSize);
            response.setContentLengthLong(length);
            try (InputStream in = Files.newInputStream(file)) {
                in.skipNBytes(start);
                copy(in, response.getOutputStream(), length);
            }
            return;
        }

        response.setStatus(HttpStatus.OK.value());
        response.setContentLengthLong(fileSize);
        try (InputStream in = Files.newInputStream(file)) {
            copy(in, response.getOutputStream(), fileSize);
        }
    }

    @PutMapping("/local-upload")
    public void putContent(@RequestParam("key") String key,
                           @RequestParam(value = "exp", required = false) Long exp,
                           @RequestParam(value = "sig", required = false) String sig,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        // Uploads are always signature-gated: a plain URL must never be enough
        // to write into the store.
        if (exp == null || sig == null || exp < Instant.now().getEpochSecond()
                || !storageService.verifySignature(key, exp, sig)) {
            writeError(response, HttpStatus.FORBIDDEN, "Invalid upload URL");
            return;
        }
        storageService.writePresignedUpload(key, request.getInputStream());
        response.setStatus(HttpStatus.CREATED.value());
    }

    /**
     * Writes the error status directly instead of via sendError(): sendError
     * triggers the container's ERROR dispatch, where the security filter chain
     * runs a second time and masks the response (403/404 surface as 401).
     */
    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
    }

    private void copy(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long remaining = limit;
        while (remaining > 0) {
            int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                break;
            }
            out.write(buffer, 0, read);
            remaining -= read;
        }
        out.flush();
    }
}
