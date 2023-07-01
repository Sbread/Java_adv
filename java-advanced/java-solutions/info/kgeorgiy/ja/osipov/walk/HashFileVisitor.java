package info.kgeorgiy.ja.osipov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

abstract class HashFileVisitor extends SimpleFileVisitor<Path> {

    private static final int BUF_SIZE = 1024;

    public static final String ZEROS = "0".repeat(64);

    static BufferedWriter writer;
    static String hashAlgorithm;

    static MessageDigest digest;

    static byte[] buffer = new byte[BUF_SIZE];


    HashFileVisitor(final BufferedWriter writer, final String hashAlgorithm) {
        HashFileVisitor.writer = writer;
        HashFileVisitor.hashAlgorithm = hashAlgorithm;
        try {
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("No such hashing algorithm. " + ex.getMessage());
        }
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        try {
            calculateHash(file, writer);
        } catch (IOException e) {
            System.err.println("Write error occurred " + e.getMessage());
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
        try {
            writeHash(ZEROS, file.toString(), writer);
        } catch (IOException e) {
            System.err.println("Write error occurred " + e.getMessage());
            return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
    }

    public static void calculateHash(final Path path, final BufferedWriter writer) throws IOException {
        try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
            int cnt;
            while ((cnt = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, cnt);
            }
            writeHash(HexFormat.of().formatHex(digest.digest()), path.toString(), writer);
        } catch (IOException e) {
            System.err.println("Error occurs reading file. " + e.getMessage());
            writeHash(ZEROS, path.toString(), writer);
        }
    }

    public static void writeHash(final String hash, final String file, final BufferedWriter writer) throws IOException {
        writer.write(hash + " " + file);
        writer.newLine();
    }
}
