// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.printerlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Reads manifests and writes deterministic, licensed measurement artifacts. */
@UtilityClass
final class LabFiles {
    @NonNull
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private static final String COPYRIGHT = "2026 Anton Lem <antonlem78@gmail.com>";

    /**
     * Hashes text consistently across operating systems.
     * @param text content to hash
     * @return SHA-256 of UTF-8 content
     */
    @NonNull
    static String hash(@NonNull String text) {
        return hash(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hashes exact artifact contents.
     * @param bytes content to hash
     * @return SHA-256 of the supplied bytes
     */
    @NonNull
    static String hash(@NonNull byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK lacks SHA-256", exception);
        }
    }

    /**
     * Reads operative manifest entries.
     * @param path input manifest
     * @return non-comment, non-blank lines
     */
    @NonNull
    static List<String> readManifest(@NonNull Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
    }

    /**
     * Writes a licensed JSON artifact.
     * @param path destination
     * @param value payload, wrapped without changing its values
     */
    static void writeJson(@NonNull Path path, @NonNull Object value) throws IOException {
        ObjectNode envelope = JSON.createObjectNode();
        envelope.put("SPDX-FileCopyrightText", COPYRIGHT);
        envelope.put("SPDX-License-Identifier", "Apache-2.0");
        envelope.set("data", JSON.valueToTree(value));
        Files.writeString(path, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(envelope) + "\n");
    }

    /**
     * Reads a licensed artifact.
     * @param path licensed artifact
     * @return its data payload
     */
    @NonNull
    static JsonNode readJson(@NonNull Path path) throws IOException {
        JsonNode envelope = JSON.readTree(path.toFile());
        if (!envelope.has("data")) {
            throw new IllegalArgumentException("Missing measurement envelope: " + path);
        }
        return envelope.get("data");
    }

    /**
     * Reads original JMH measurements.
     * @param path raw JMH JSON
     * @return the unchanged JMH result tree
     */
    @NonNull
    static JsonNode readRawJson(@NonNull Path path) throws IOException {
        return JSON.readTree(path.toFile());
    }

    /**
     * Writes a licensed Markdown report.
     * @param path destination
     * @param text Markdown body
     */
    static void writeMarkdown(@NonNull Path path, @NonNull String text) throws IOException {
        Files.writeString(
                path,
                "<!--\nSPDX-FileCopyrightText: " + COPYRIGHT + "\nSPDX-License-Identifier: Apache-2.0\n-->\n\n" + text);
    }

    /**
     * Adds SPDX metadata to a completed log.
     * @param path completed tool log to annotate without altering its original body
     */
    static void licenseLog(@NonNull Path path) throws IOException {
        String text = Files.readString(path);
        Files.writeString(
                path, "# SPDX-FileCopyrightText: " + COPYRIGHT + "\n# SPDX-License-Identifier: Apache-2.0\n" + text);
    }

    /**
     * Executes a command without shell interpolation and checks its status.
     * @param directory process working directory
     * @param command executable and arguments
     * @return standard output
     */
    @NonNull
    static String execute(@NonNull Path directory, @NonNull List<String> command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Command failed: " + command + "\n" + output);
        }
        return output.strip();
    }
}
