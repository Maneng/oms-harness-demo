# 验证记录

日期：2026-11-11。

## Java 行为验证

执行环境为 Linux、Java 21.0.13。当前环境包含 jdk.compiler，但不含历史版本符号表 ct.sym。脚本在明确收到 release version 17 not supported 后，使用 -source 17 -target 17 编译，再在 Java 21 运行。

因此，这里的实测结论是 Java 21 执行通过；源码和字节码目标为 Java 17，未声称在独立 Java 17 运行时执行过。

| 版本 | 命令 | 通过 | 失败 | 退出码 |
| --- | --- | ---: | ---: | ---: |
| 本包初始缺陷版 | bash run-tests.sh | 5 | 2 | 1 |
| 本包初始缺陷版 | bash followup-check.sh | 1 | 2 | 1 |
| 独立参考修复副本 | bash run-tests.sh | 7 | 0 | 0 |
| 独立参考修复副本 | bash followup-check.sh | 3 | 0 | 0 |

参考副本只修复了业务代码的版本接收逻辑，未修改三个测试源文件；三个测试文件的 SHA-256 已比对一致。交付包中保留的是有缺陷的起始实现，不包含参考修复副本。

主测试覆盖首次快照、新版本、旧版本、重复版本、SKU 隔离、仓库隔离、非法数量不污染状态。后续测试覆盖同版本冲突数量、首次版本 0、拒绝旧事件后继续接受新事件。

`scripts/prepare-worktree.sh` 已通过 bash 语法检查。它改写自官方部署文档的 prepare 命令；真实 GitHub 拉取和 worktree 创建尚需在用户配置的原型环境验证。

## Harness 验证范围

已在制作环境下载并执行官方指定兼容版本：

```text
npx --yes @deepseek-ai/dsh@0.2.1-rc.3 --version
0.2.1-rc.3
```

启动 Web 服务的探测收到了 HTTP 401；未完成浏览器认证，所以未将 Web 界面标为实测通过。测试用进程已停止。

模型请求、独立 GitHub 账号登录、真实事件接单、PR 发布与跟进处理未在制作环境执行。这些操作需要用户在本机配置自己的模型凭据和 GitHub Agent 账号。

当前包不含模型凭据、GitHub 令牌、Harness 配置、服务日志、编译产物或参考答案。
