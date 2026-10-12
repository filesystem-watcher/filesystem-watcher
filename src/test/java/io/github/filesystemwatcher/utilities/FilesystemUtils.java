package io.github.filesystemwatcher.utilities;

import lombok.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Supplier;

@Value
public class FilesystemUtils {

    private final Path temporaryFolder;

    public static Path delete(Path path) {
        TryIO.with(() -> Files.deleteIfExists(path));
        return path;
    }

    public static Path createDirectory(Path path, String name) {
        return TryIO.with(() -> Files.createDirectory(path.resolve(name)));
    }

    public static Path createDirectories(Path path) {
        return TryIO.with(() -> Files.createDirectories(path));
    }

    public static Path createFile(Path path, String name) {
        return TryIO.with(() -> Files.createFile(path.resolve(name)));
    }

    public static Path createTempDirectory(String prefix) {
        return TryIO.with(() -> Files.createTempDirectory(prefix));
    }

    //TODO simplify the signature
    public static Path createLink(Path linkTarget, String linkName, Path linkPlace) {
        return TryIO.with(() -> Files.createSymbolicLink(linkPlace.resolve(linkName), linkTarget));
    }

    public static Path writeFile(Path path, String content) {
        return TryIO.with(() -> Files.write(path, content.getBytes()));
    }

    public static Path move(Path source, Path target) {
        return TryIO.with(() -> Files.move(source, target));
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path) {
        return TryIO.with(() -> Files.getPosixFilePermissions(path));
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> permissions) {
        return TryIO.with(() -> Files.setPosixFilePermissions(path, permissions));
    }

    static class TryIO {

        @FunctionalInterface
        interface ThrowingSupplier<T> {
            T apply() throws IOException;
        }

        static <T> T with(ThrowingSupplier<T> throwingSupplier) {
            Supplier<T> supplier = () -> {
                try {
                    return throwingSupplier.apply();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            return supplier.get();
        }
    }
}
