package info.kgeorgiy.ja.osipov.implementor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


/**
 * Custom {@code SimpleFileVisitor} for delete directories
 * @author Daniil Osipov
 */
public class DeleteFileVisitor extends SimpleFileVisitor<Path> {
    /**
     * Delete file and continue.
     *
     * <p> returns {@link FileVisitResult#CONTINUE
     * CONTINUE}.
     *
     * @param file {@code Path} to visit
     * @param attrs {@link BasicFileAttributes} attributes
     * @throws IOException if deletion failed.
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Delete directory after directory and all of their
     * descendants, have been visited.
     *
     * <p> returns {@link FileVisitResult#CONTINUE
     * CONTINUE} if the directory iteration completes without an I/O exception;
     * otherwise this method re-throws the I/O exception that caused the iteration
     * of the directory to terminate prematurely.
     *
     * @param dir {@code Path} of directory to visit
     * @param exc exception when visiting directory
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
    }
}

