# Better Backups

A better backup mod for Minecraft worlds.

## Commands

All commands require server owner permission.

```text
/backup start
/backup list
/backup clear
/backup clear confirm
/backup restore <backup>
/backup restore cancel
/backup status

/backup set schedule on
/backup set schedule off
/backup set schedule every <duration>
/backup set schedule warning on
/backup set schedule warning off
/backup set schedule warning before <duration>
/backup set max-backups <count>
/backup set stop-after-restore on
/backup set stop-after-restore off
/backup set restore-delay on
/backup set restore-delay off
/backup set restore-delay time <duration>
/backup set clear-confirm on
/backup set clear-confirm off
/backup set language <language>
/backup config
```

Schedule interval values use `m`, `h`, or `d`. Warning time values also support `s`.

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule every 1d
/backup set schedule warning before 30s
```

Supported languages are `en_us` and `zh_cn`.

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
