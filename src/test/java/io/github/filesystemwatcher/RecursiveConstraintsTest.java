package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.github.filesystemwatcher.FilesystemConstraints.FileType.REGULAR;
import static io.github.filesystemwatcher.FilesystemEventType.CREATED;

class RecursiveConstraintsTest extends AbstractMonitorTest {

    @ParameterizedTest
    @MethodSource
    void shouldWatchSubdirectoriesExcludedByConstraints(FilesystemConstraints constraints, @TempDir Path directory) {
        //given
        Path existingDirectory = FilesystemUtils.createDirectory(directory, "existing");
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(directory, constraints, receivedEvents::add);

        //when
        Path createdDirectory = FilesystemUtils.createDirectory(directory, "created");
        Path existingDirectoryFile = FilesystemUtils.createFile(existingDirectory, "file.txt");
        Path createdDirectoryFile = FilesystemUtils.createFile(createdDirectory, "file.txt");

        //then
        List<FilesystemEvent> expectedEvents = Arrays.asList(
                FilesystemEvent.of(existingDirectoryFile, CREATED),
                FilesystemEvent.of(createdDirectoryFile, CREATED)
        );
        expectedEvents.forEach(event -> AwaitilityUtils.awaitForElement(receivedEvents, event));
        Assertions.assertThat(receivedEvents).hasSameElementsAs(expectedEvents);
    }

    static Stream<FilesystemConstraints> shouldWatchSubdirectoriesExcludedByConstraints() {
        return Stream.of(
                FilesystemConstraints.DEFAULT.withRecursive(true).withFileTypes(Arrays.asList(REGULAR)),
                FilesystemConstraints.DEFAULT.withRecursive(true).withFilenamePatterns(Arrays.asList(Pattern.compile(".*\\.txt")))
        );
    }

    @Test
    void shouldNotWatchSubdirectoriesWhenNotRecursive(@TempDir Path directory) {
        //given
        Path existingDirectory = FilesystemUtils.createDirectory(directory, "existing");
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        startMonitor(directory, FilesystemConstraints.DEFAULT, receivedEvents::add);

        //when
        Path createdDirectory = FilesystemUtils.createDirectory(directory, "created");
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(createdDirectory, CREATED));
        FilesystemUtils.createFile(existingDirectory, "file.txt");
        FilesystemUtils.createFile(createdDirectory, "file.txt");
        Path sentinel = FilesystemUtils.createFile(directory, "sentinel.txt");

        //then
        AwaitilityUtils.awaitForElement(receivedEvents, FilesystemEvent.of(sentinel, CREATED));
        Assertions.assertThat(receivedEvents).allMatch(event -> event.getPath().getParent().equals(directory));
    }
}
