package info.kgeorgiy.ja.osipov.walk;

import java.io.BufferedWriter;

class SHA256HashFileVisitor extends HashFileVisitor {

    SHA256HashFileVisitor(final BufferedWriter writer) {
        super(writer, "SHA-256");
    }
}
