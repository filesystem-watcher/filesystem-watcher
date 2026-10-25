package io.github.filesystemwatcher;

import io.github.filesystemwatcher.utilities.FilesystemUtils;
import io.github.filesystemwatcher.utilities.WatchCoordinator;
import io.github.filesystemwatcher.utilities.WatchImplementation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.filesystemwatcher.FilesystemEventType.*;

@RequiredArgsConstructor
enum SingleDirectoryScenario implements Scenario {

    DIRECTORY_CREATE(SingleDirectoryScenario::create, WatchImplementation.all()),
    DIRECTORY_DELETE(SingleDirectoryScenario::delete, WatchImplementation.all()),
    DIRECTORY_RENAME(SingleDirectoryScenario::rename, WatchImplementation.all()),
    DIRECTORY_ADD_POSIX_PERMISSIONS(SingleDirectoryScenario::addPermissions, WatchImplementation.nativeWithPosixPermissions()),
    DIRECTORY_REMOVE_POSIX_PERMISSIONS(SingleDirectoryScenario::removePermissions, WatchImplementation.nativeWithPosixPermissions()),
    DIRECTORY_SET_SAME_PERMISSIONS(SingleDirectoryScenario::setSamePermissions, WatchImplementation.nativeWithPosixPermissions());

    private final Scenario scenario;

    @Getter
    private final List<WatchImplementation> implementations;

    @Override
    public List<FilesystemEvent> apply(Path path, WatchCoordinator watchCoordinator) {
        return scenario.apply(path, watchCoordinator);
    }

    private static List<FilesystemEvent> create(Path base, WatchCoordinator coordinator) {
        coordinator.setupCompleted();

        Path created = FilesystemUtils.createDirectory(base, "test");
        return Arrays.asList(FilesystemEvent.of(created, CREATED));
    }

    private static List<FilesystemEvent> delete(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");

        coordinator.setupCompleted();

        FilesystemUtils.delete(created);
        return Arrays.asList(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, DELETED)
        );
    }

    private static List<FilesystemEvent> rename(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Path moved = FilesystemUtils.move(created, base.resolve("test2"));

        return Arrays.asList(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, DELETED),
                FilesystemEvent.of(moved, CREATED)
        );
    }

    private static List<FilesystemEvent> addPermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return Arrays.asList(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> removePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        PosixFilePermission toRemove = permissions.stream()
                .filter(permission -> !permission.toString().startsWith("OWNER"))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cannot remove POSIX attribute, because given path=%s have all them set to false", created)));
        permissions.remove(toRemove);
        FilesystemUtils.setPosixFilePermissions(created, permissions);
        return Arrays.asList(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }

    private static List<FilesystemEvent> setSamePermissions(Path base, WatchCoordinator coordinator) {
        Path created = FilesystemUtils.createDirectory(base, "test");
        coordinator.setupCompleted();

        Set<PosixFilePermission> permissions = FilesystemUtils.getPosixFilePermissions(created);
        FilesystemUtils.setPosixFilePermissions(created, permissions);

        return Arrays.asList(
                FilesystemEvent.of(created, INITIAL),
                FilesystemEvent.of(created, MODIFIED)
        );
    }
}
