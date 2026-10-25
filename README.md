[![CI](https://github.com/filesystem-watcher/filesystem-watcher/actions/workflows/ci.yml/badge.svg)](https://github.com/filesystem-watcher/filesystem-watcher/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.filesystem-watcher/filesystem-watcher)](https://central.sonatype.com/artifact/io.github.filesystem-watcher/filesystem-watcher)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=filesystem-watcher_filesystem-watcher&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=filesystem-watcher_filesystem-watcher)

# Filesystem Watcher
Watch for filesystem changes in your Java project in a simple and robust way.

Built on Java's `WatchService` (native notifications on Linux and Windows, polling on macOS)
and `FileVisitor` (handles directories that change while they are read).

## Getting Started

Requires Java 11 or newer. Add the dependency:

```xml
<dependency>
    <groupId>io.github.filesystem-watcher</groupId>
    <artifactId>filesystem-watcher</artifactId>
    <version>0.4.0</version>
</dependency>
```

Start watching:

```java
FilesystemNotifier monitor = FilesystemMonitor.builder()
        .watchedPath(Paths.get("/home/john/Desktop"))
        .watchedConstraints(FilesystemConstraints.DEFAULT.withRecursive(true))
        .watchedConsumer(event -> System.out.println(event.getPath() + " " + event.getEventType()))
        .build();

monitor.startWatching(); // returns once all directories are watched
// ...
monitor.stopWatching();
```

Existing files are reported first as `INITIAL`, then changes as `CREATED`, `MODIFIED` and `DELETED`.
Constraints are optional and can filter by file name, pattern or file type.

## Releasing

Push a tag `vX.Y.Z`. GitHub Actions publishes version `X.Y.Z` to Maven Central.

## Changelog

### Unreleased
1. Builds on JDK 17 and newer. The only runtime dependency is `slf4j-api`.
1. Per-event log messages moved to `debug`.
1. `watchedConstraints` is optional and defaults to `FilesystemConstraints.DEFAULT`.

### 0.4.0
1. Keep watching when a single event or the consumer fails.
1. Constraints no longer stop subdirectories from being watched.
1. `startWatching()` returns once all directories are watched.
1. `isRecursive = false` no longer watches subdirectories.
1. Subdirectories of a newly created directory are watched, and each new path is reported once.
1. Published from GitHub Actions through the Central Portal.

### 0.3.0
1. Support for systems with pooling WatchService implementation (FreeBSD, MacOS)
1. Surround threads with better logging so that discovering exceptions becomes easier.
