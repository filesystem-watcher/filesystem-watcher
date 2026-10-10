[![CI](https://github.com/filesystem-watcher/filesystem-watcher/actions/workflows/ci.yml/badge.svg)](https://github.com/filesystem-watcher/filesystem-watcher/actions/workflows/ci.yml)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=filesystem-watcher&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=filesystem-watcher)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=filesystem-watcher&metric=coverage)](https://sonarcloud.io/dashboard?id=filesystem-watcher)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.filesystem-watcher/filesystem-watcher/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz.jirutka.rsql/rsql-parser)
# Filesystem Watcher
Watch for filesystem changes in you Java project in simple and robust way.
Project utilize Java 7 APIs like WatchService (which does not pool filesystem for changes in most common environments) 
and FileVisitor (which correctly handles intensively changing directories) 

## Getting Started

Add the dependency:

```
<dependency>
    <groupId>io.github.filesystem-watcher</groupId>
    <artifactId>filesystem-watcher</artifactId>
    <version>0.1.0</version>
</dependency>
```

Write few lines of Java code: 

```
FilesystemMonitor.builder()
    .watchedPath("/home/john/Desktop")
    .watchedConsumer(event -> System.out.println("Path " + event.getPath() + " " + event.getType()))
    .build()
    .watch();
```

## Changelog

### 0.3.0
1. Support for systems with pooling WatchService implementation (FreeBSD, MacOS)
1. Surround threads with better logging so that discovering exceptions becomes easier.








