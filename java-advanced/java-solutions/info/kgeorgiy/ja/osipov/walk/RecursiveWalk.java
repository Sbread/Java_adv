package info.kgeorgiy.ja.osipov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static info.kgeorgiy.ja.osipov.walk.HashFileVisitor.ZEROS;

public class RecursiveWalk {

    public static void walk(final String[] args, final boolean rec) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Expected non-null input and output files");
            return;
        }
        Path in, out;
        try {
            in = Path.of(args[0]);
        } catch (InvalidPathException e) {
            System.err.println("Invalid in path. " + e.getMessage());
            return;
        }
        try {
            out = Path.of(args[1]);
            if (out.getParent() != null) {
                try {
                    Files.createDirectories(out.getParent());
                } catch (SecurityException e) {
                    System.err.println("create directories access denied " + e.getMessage());
                    return;
                }
            }
        } catch (InvalidPathException e) {
            System.err.println("Invalid in path. " + e.getMessage());
            return;
        } catch (IOException e) {
            System.err.println("Cannot create parent directory for output file. " + e.getMessage());
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                SHA256HashFileVisitor fileVisitor = new SHA256HashFileVisitor(writer);
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        try {
                            Path startPath = Path.of(line);
                            if (rec) {
                                try {
                                    Files.walkFileTree(startPath, fileVisitor);
                                } catch (IOException e) {
                                    System.err.println("Visitor method IO error. " + e.getMessage());
                                } catch (SecurityException e) {
                                    System.err.println("access to file denied" + e.getMessage());
                                    HashFileVisitor.writeHash(ZEROS, line, writer);
                                }
                            } else {
                                SHA256HashFileVisitor.calculateHash(startPath, writer);
                            }
                        } catch (InvalidPathException e) {
                            System.err.println("Invalid file path. " + e.getMessage());
                            HashFileVisitor.writeHash(ZEROS, line, writer);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Input file read error" + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Writing error occurred. " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Input file read error. " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        walk(args, true);
    }
}
