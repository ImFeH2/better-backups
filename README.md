# Better Backups

A better backup mod for Minecraft worlds.

## Commands

All commands require server owner permission.

```text
/backup start
/backup list
/backup restore <backup>
/backup status

/backup set schedule on
/backup set schedule off
/backup set schedule every <duration>
/backup set max-backups <count>
/backup set stop-after-restore on
/backup set stop-after-restore off
/backup config
```

Duration values use `m`, `h`, or `d`.

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule every 1d
```

## Backups

Create a world backup whenever you need one, or enable scheduled backups to run automatically. Older backups are cleaned up based on the configured limit.

```text
2026-05-29_21-34-08+0800.zip
```

Backup names use the server's local time.

## Restore

Restore any available backup from the command list. By default, the server stops and starts again after the selected backup is restored.

## Build

```text
JAVA_HOME=/path/to/java-25 ./gradlew build
```
