# Better Backups

[English](README.md)

一个更好的 Minecraft 世界备份模组。

## 命令

所有命令都需要服务器所有者权限。

常用命令：

```text
/backup start              立即创建备份。
/backup list               显示可用备份。
/backup clear              确认后删除所有备份。
/backup restore <backup>   恢复指定备份。
/backup restore cancel     取消等待中的恢复操作。
/backup status             显示当前备份状态。
/backup config             显示当前设置。
/backup help               显示命令帮助。
/backup menu               打开交互式备份菜单。
```

设置命令：

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

`/backup clear` 默认需要确认。你可以点击聊天里的确认消息，也可以运行 `/backup clear confirm`。
使用 `/backup menu` 可以通过点击消息完成备份、恢复和设置调整。

定时触发方式可以是 `every` 或 `cron`，两者互斥，设置其中一个会替换另一个。
定时模式 `active` 只在服务器实际运行 tick 时计时，`realtime` 使用服务器进程运行期间的真实时间。
间隔定时支持 `m`、`h`、`d`。Cron 定时使用服务器时区下的 5 段 cron 表达式。
备份前提醒和恢复延迟也支持 `s`。

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule every 1d
/backup set schedule cron "0 4 * * *"
/backup set schedule cron "*/30 * * * *"
/backup set schedule warning before 30s
/backup set restore-delay time 30s
```

支持的语言是 `en_us` 和 `zh_cn`。

## 备份

你可以随时手动创建世界备份，也可以启用定时备份自动运行。旧备份会根据设置的保留数量自动清理。

```text
2026-05-29_21-34-08+0800.zip
```

备份名称使用服务器本地时间。

## 恢复

从备份列表中选择任意备份即可恢复。默认情况下，恢复完成后服务器会停止并重新启动。
