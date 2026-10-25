package io.github.filesystemwatcher;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Builder
public class FilesystemReader implements FilesystemNotifier {

    private final Path watchedPath;
    private final FilesystemConstraints watchedConstraints;
    private final Consumer<FilesystemEvent> watchedConsumer;

    @Override
    public void startWatching() {
        ConstraintsFilteringVisitor visitor = new ConstraintsFilteringVisitor(watchedPath, watchedConstraints);
        try {
            log.debug("Reading started path={}", watchedPath);
            Files.walkFileTree(watchedPath, visitor);
            log.debug("Reading completed path={}", watchedPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        visitor.getEvents().forEach(watchedConsumer);
    }

    @Override
    public void stopWatching() {
        // For now we do not need to implement this. Maybe later to make this class more responsive to stop.
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    private static final class ConstraintsFilteringVisitor extends SimpleFileVisitor<Path> {

        private final Path watchedPatch;
        private final FilesystemConstraints constraints;
        @Getter
        private final List<FilesystemEvent> events = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            super.preVisitDirectory(dir, attrs);

            if (dir.equals(watchedPatch)) {
                return FileVisitResult.CONTINUE;
            }

            addFilesystemEvent(dir);

            if (!constraints.isRecursive()) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            super.visitFile(file, attrs);
            addFilesystemEvent(file);
            return FileVisitResult.CONTINUE;
        }

        private void addFilesystemEvent(Path path) {
            FilesystemEvent filesystemEvent = FilesystemEvent.of(path, FilesystemEventType.INITIAL);

            if (constraints.test(filesystemEvent)) {
                events.add(filesystemEvent);
                log.debug("Created event: {}", filesystemEvent);
            }
        }
    }
}
