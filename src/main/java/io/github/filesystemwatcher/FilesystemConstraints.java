package io.github.filesystemwatcher;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.github.filesystemwatcher.FilesystemEventType.DELETED;

@Value
@Builder
@With
public class FilesystemConstraints implements Predicate<FilesystemEvent> {

    @RequiredArgsConstructor
    public enum FileType {
        REGULAR(BasicFileAttributes::isRegularFile),
        DIRECTORY(BasicFileAttributes::isDirectory),
        LINK(BasicFileAttributes::isSymbolicLink),
        OTHER(BasicFileAttributes::isOther);

        private final Predicate<BasicFileAttributes> predicate;
    }

    public static final FilesystemConstraints DEFAULT = FilesystemConstraints.builder().build()
            .withFilenamePatterns(List.of())
            .withFilenameSubstrings(List.of())
            .withFileTypes(List.of())
            .withRecursive(false);

    private final List<String> filenameSubstrings;
    private final List<Pattern> filenamePatterns;
    private final List<FileType> fileTypes;
    private final boolean isRecursive;

    @Override
    public boolean test(FilesystemEvent event) {
        Path path = event.getPath();
        if (!fileTypes.isEmpty() && event.getEventType() != DELETED) {
            BasicFileAttributes fileAttributes = readAttributes(path);
            if (fileTypes.stream().noneMatch(fileType -> fileType.predicate.test(fileAttributes))) {
                return false;
            }
        }

        String filename = path.getFileName().toString();
        if (!filenameSubstrings.isEmpty()) {
            if (filenameSubstrings.stream().noneMatch(filename::contains)) {
                return false;
            }
        }

        if (!filenamePatterns.isEmpty()) {
            if (filenamePatterns.stream().noneMatch(pattern -> pattern.matcher(filename).matches())) {
                return false;
            }
        }
        return true;
    }

    FilesystemConstraints withoutFilters() {
        return DEFAULT.withRecursive(isRecursive);
    }

    private BasicFileAttributes readAttributes(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
