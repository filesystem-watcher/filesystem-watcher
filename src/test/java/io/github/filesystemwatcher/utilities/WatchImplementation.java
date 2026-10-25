package io.github.filesystemwatcher.utilities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public enum WatchImplementation {
    POOLING(Duration.ofSeconds(2)),
    NATIVE(Duration.ofMillis(200));

    @Getter
    final Duration sensitivity;

    public static WatchImplementation determineImplementation() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            String name = watchService.getClass().getSimpleName();
            if ("PollingWatchService".equals(name)) {
                return POOLING;
            }
            return NATIVE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean detectsModificationsWithinSecond() {
        return determineImplementation() == NATIVE || !isJava8();
    }

    private static boolean isJava8() {
        return System.getProperty("java.specification.version").startsWith("1.");
    }

    public static List<WatchImplementation> all() {
        return Arrays.asList(values());
    }
}
