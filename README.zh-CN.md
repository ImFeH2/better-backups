# Better Backups

[English](README.md)

一个更好的 Minecraft 世界备份模组。

## 功能

- 随时手动创建备份。
- 使用间隔或 cron 定时自动备份。
- 根据设置的保留数量只保留最新备份。
- 恢复备份前提供延迟和取消时间。
- 通过可点击的聊天菜单管理备份和设置。
- 支持英文和中文消息。

## 快速开始

使用 `/backup menu` 打开可点击的备份菜单。你可以直接在菜单里创建备份、查看备份列表、恢复备份、查看状态和调整设置，不需要手动输入每条命令。

所有命令都需要服务器所有者权限。

## 常用命令

```text
/backup                    打开可点击的备份菜单。
/backup start              立即创建备份。
/backup list               显示可用备份。
/backup restore <backup>   恢复指定备份。
/backup restore cancel     取消等待中的恢复操作。
/backup clear              确认后删除所有备份。
/backup status             显示当前备份状态。
/backup config             显示当前设置。
/backup help               显示命令帮助。
```

在游戏内使用 `/backup help` 可以查看完整命令列表。

## 备份

备份名称使用服务器本地时间。

```text
2026-05-29_21-34-08+0800.zip
```

定时备份可以使用间隔时间或 cron 时间。设置其中一种会替换另一种。

```text
/backup set schedule every 30m
/backup set schedule every 2h
/backup set schedule cron "0 4 * * *"
```

间隔定时支持 `m`、`h`、`d`。Cron 定时使用服务器时区下的 5 段 cron 表达式。

定时模式决定如何计时：

- `active` 只在服务器实际运行 tick 时计时。
- `realtime` 使用服务器进程运行期间的真实时间。

## 恢复

恢复操作会先等待一段时间再应用选中的备份，让你可以用 `/backup restore cancel` 取消。默认情况下，恢复完成后服务器会停止，以便你的服务器管理器重新启动它。

## 设置

使用 `/backup menu` 可以通过点击调整设置，也可以使用 `/backup config` 查看当前设置。

支持的语言是 `en_us` 和 `zh_cn`。
