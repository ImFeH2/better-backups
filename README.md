# Better Backups

[简体中文](README.zh-CN.md)

A better backup mod for Minecraft worlds.

## Commands

All commands require server owner permission.

Common commands:

```text
/backup start              Create a backup now.
/backup list               Show available backups.
/backup clear              Delete all backups after confirmation.
/backup restore <backup>   Restore a backup.
/backup restore cancel     Cancel a pending restore.
/backup status             Show current backup status.
/backup config             Show current settings.
/backup help               Show command help.
/backup menu               Open the interactive backup menu.
```

Settings:

```text
/backup set schedule on|off
/backup set schedule every <duration>
/backup set schedule cron "<expression>"
/backup set schedule mode active|realtime
/backup set schedule warning on|off
/backup set schedule warning before <duration>
/backup set max-backups <count>
/backup set stop-after-restore on|off
/backup set restore-delay on|off
/backup set restore-delay time <duration>
/backup set clear-confirm on|off
/backup set language <language>
```

`/backup clear` asks for confirmation by default. Use the clickable confirm message in chat, or run `/backup clear confirm`.
Use `/backup menu` for clickable backup actions and config controls.

Schedule trigger can be `every` or `cron`. They are mutually exclusive: setting one replaces the other.
Schedule mode `active` only runs scheduled backups while the server is actively ticking. `realtime` uses wall clock time while the server process is running.
Interval schedules use `m`, `h`, or `d`. Cron schedules use 5-part cron expressions in the server timezone.
Warning and restore delay times also support `s`.

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule every 1d
/backup set schedule cron "0 4 * * *"
/backup set schedule cron "*/30 * * * *"
/backup set schedule warning before 30s
/backup set restore-delay time 30s
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

<!-- modrinth_exclude.start -->
## Build

```text
JAVA_HOME=/path/to/java-25 ./gradlew build
```
<!-- modrinth_exclude.end -->
