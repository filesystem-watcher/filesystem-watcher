package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;

@RequiredArgsConstructor
enum RecursiveFileScenario implements Scenario {

    RECURSIVE_FILE_CREATE(RecursiveFileScenario::create, WatchImplementation.all()),
    RECURSIVE_FILE_DELETE(RecursiveFileScenario::delete, WatchImplementation.all()),
    RECURSIVE_FILE_RENAME(RecursiveFileScenario::rename, WatchImplementation.all()),
    RECURSIVE_FILE_ADD_POSIX_PERMISSIONS(RecursiveFileScenario::addPermissions, Arrays.asList(WatchImplementation.NATIVE)),
    RECURSIVE_FILE_REMOVE_POSIX_PERMISSIONS(RecursiveFileScenario::removePermissions, Arrays.asList(WatchImplementation.NATIVE)),
    RECURSIVE_FILE_SET_SAME_PERMISSIONS(RecursiveFileScenario::setSamePermissions, Arrays.asList(WatchImplementation.NATIVE));

    private final Scenario scenario;
    @Getter
    private final List<WatchImplementation> implementations;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");

        coordinator.setupCompleted();

        Path createdFile = FilesystemUtils.createFile(third, "test.txt");

        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL)));

        initial.add(FilesystemEvent.of(createdFile, CREATED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path file = FilesystemUtils.createFile(third, "test.txt");

        coordinator.setupCompleted();

        FilesystemUtils.delete(file);
        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(file, INITIAL)));

        initial.add(FilesystemEvent.of(file, DELETED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    private static List<FilesystemEvent> rename(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path file = FilesystemUtils.createFile(third, "test.txt");

        coordinator.setupCompleted();

        Path moved = FilesystemUtils.move(file, base.resolve("moved.txt"));

        List<FilesystemEvent> initial = new ArrayList<>(Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(file, INITIAL)));

        initial.add(FilesystemEvent.of(moved, CREATED));
        initial.add(FilesystemEvent.of(file, DELETED));
        if (WatchImplementation.determineImplementation() == WatchImplementation.POOLING) {
            initial.add(FilesystemEvent.of(third, MODIFIED));
        }
        return initial;
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path file = FilesystemUtils.createFile(third, "test.txt");

        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(file);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(file, permissions);
        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(file, INITIAL),
                FilesystemEvent.of(file, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path file = FilesystemUtils.createFile(third, "test.txt");

        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(file);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", file)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(file, permissions);
        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(file, INITIAL),
                FilesystemEvent.of(file, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path first = FilesystemUtils.createDirectory(base, "first");
        Path second = FilesystemUtils.createDirectory(first, "second");
        Path third = FilesystemUtils.createDirectory(second, "third");
        Path file = FilesystemUtils.createFile(third, "test.txt");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(file);
        FilesystemUtils.setPosixFilePermissions(file, permissions);

        return Arrays.asList(
                FilesystemEvent.of(first, INITIAL),
                FilesystemEvent.of(second, INITIAL),
                FilesystemEvent.of(third, INITIAL),
                FilesystemEvent.of(file, INITIAL),
                FilesystemEvent.of(file, MODIFIED)
        );
    }

}
