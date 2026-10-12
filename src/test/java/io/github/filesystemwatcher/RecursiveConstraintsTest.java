package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.AwaitilityUtils;
import io.github.filesystemwatcher.utilities.FilesystemUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.github.filesystemwatcher.FilesystemConstraints.FileType.REGULAR;
import static io.github.filesystemwatcher.FilesystemEventType.CREATED;

class RecursiveConstraintsTest {

    @ParameterizedTest
    @MethodSource
    void shouldWatchSubdirectoriesExcludedByConstraints(FilesystemConstraints constraints, @TempDir Path directory) {
        //given
        Path existingDirectory = FilesystemUtils.createDirectory(directory, "existing");
        List<FilesystemEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        FilesystemNotifier monitor = FilesystemMonitor.builder()
                .watchedPath(directory)
                .watchedConstraints(constraints)
                .watchedConsumer(receivedEvents::add)
                .build();
        monitor.startWatching();

        try {
            //when
            Path createdDirectory = FilesystemUtils.createDirectory(directory, "created");
            Path existingDirectoryFile = FilesystemUtils.createFile(existingDirectory, "file.txt");
            Path createdDirectoryFile = FilesystemUtils.createFile(createdDirectory, "file.txt");

            //then
            List<FilesystemEvent> expectedEvents = List.of(
                    FilesystemEvent.of(existingDirectoryFile, CREATED),
                    FilesystemEvent.of(createdDirectoryFile, CREATED)
            );
            expectedEvents.forEach(event -> AwaitilityUtils.awaitForElement(receivedEvents, event));
            Assertions.assertThat(receivedEvents).hasSameElementsAs(expectedEvents);
        } finally {
            monitor.stopWatching();
        }
    }

    static Stream<FilesystemConstraints> shouldWatchSubdirectoriesExcludedByConstraints() {
        return Stream.of(
                FilesystemConstraints.DEFAULT.withRecursive(true).withFileTypes(List.of(REGULAR)),
                FilesystemConstraints.DEFAULT.withRecursive(true).withFilenamePatterns(List.of(Pattern.compile(".*\\.txt")))
        );
    }
}
