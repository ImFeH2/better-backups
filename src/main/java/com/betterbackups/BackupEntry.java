package com.betterbackups;

import java.nio.file.Path;
import java.time.Instant;

public record BackupEntry(String name, Path path, long sizeBytes, Instant modifiedAt) {
}
