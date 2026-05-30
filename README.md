# Better Backups

[简体中文](README.zh-CN.md)

A better backup mod for Minecraft worlds.

## Features

- Create backups manually whenever you need one.
- Run scheduled backups with interval or cron timing.
- Keep only the newest backups based on your configured limit.
- Restore backups with a delay and cancellation window.
- Manage backups and settings from clickable chat menus.
- Show messages in English or Chinese.

## Quick Start

Use `/backup menu` to open the clickable backup menu. It gives you access to backup creation, backup lists, restore actions, status, and config controls without typing every command by hand.

All commands require server owner permission.

## Common Commands

```text
/backup menu               Open the clickable backup menu.
/backup start              Create a backup now.
/backup list               Show available backups.
/backup restore <backup>   Restore a backup.
/backup restore cancel     Cancel a pending restore.
/backup clear              Delete all backups after confirmation.
/backup status             Show current backup status.
/backup config             Show current config.
/backup help               Show command help.
```

Use `/backup help` in game for the full command list.

## Backups

Backups are named with the server's local time.

```text
2026-05-29_21-34-08+0800.zip
```

Scheduled backups can use either interval timing or cron timing. Setting one replaces the other.

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule cron "0 4 * * *"
```

Interval schedules support `m`, `h`, and `d`. Cron schedules use 5-part cron expressions in the server timezone.

Schedule mode controls how time is counted:

- `active` counts only while the server is actively ticking.
- `realtime` uses wall clock time while the server process is running.

## Restore

Restore actions wait before applying the selected backup, giving you time to cancel with `/backup restore cancel`. By default, the server stops after the restore finishes so your server manager can start it again.

## Config

Use `/backup menu` for clickable config controls, or `/backup config` to view the current config.

Supported languages are `en_us` and `zh_cn`.
