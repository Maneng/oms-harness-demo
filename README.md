# DeepSeek Harness：Java 库存同步实践

这是为学习 Harness / Super-agent 编写的教学项目，不是 DeepSeek 官方自带的 Demo。资料核对日期：2026-11-11。

目标分两段：

1. 本地 Harness：读取代码 → 复现问题 → 修改 → 运行测试。
2. 真实 Super-agent：GitHub 新消息 → Ticket → Worker → PR → 同一任务收到追加反馈。

## 练习内容

`InventoryLedger` 保存 SKU + 仓库维度的库存快照，每条快照带有数量和版本号。

目标规则：首次快照可接受；后续只有严格更高版本才可覆盖；旧版本与相同版本必须忽略；拒绝负库存且不能污染已有状态。

初始实现故意缺少版本保护。主测试应有 7 条，其中 2 条失败。不要把初始红灯误认为安装失败。

项目没有 Spring、数据库或第三方 Java 依赖。使用 JDK 17 或更高版本。

## 1. 检查本机并复现

以下命令适用于 macOS / Linux。解压 ZIP，然后进入解压得到的项目目录：

```bash
cd /实际路径/deepseek-harness-oms-demo
node -v
java -version
bash run-tests.sh
```

为了兼顾当前 Harness 本体和原型，建议 Node.js 24.9 或更高版本。Java 测试需要包含编译器模块的 JDK 17+；仅有裁剪过的 JRE 可能无法运行。

预期：7 条主测试中有 2 条失败，命令退出码为 1。这是故意保留的问题。

## 2. 本地 Harness 演练

为后续使用 Super-agent，下面固定采用其官方部署文档列出的 Harness 版本，而非随时变化的 latest。

```bash
npx --yes @deepseek-ai/dsh@0.2.1-rc.3 web
```

打开终端打印的完整地址，默认端口通常是 3080。若地址带本地访问参数，使用完整地址；不要自行截短。终端保持运行。

如果你已有个人 Harness 配置，想把本次练习隔开，可在一个新终端中设置单独的目录后再启动：

```bash
export DSH_HOME="$HOME/.dsh-oms-lab"
npx --yes @deepseek-ai/dsh@0.2.1-rc.3 web
```

`DSH_HOME` 是该软件的配置目录，不是项目工作区。隔离后需要在此配置中重新配置可用模型。后续原型安装与启动也要在设置了同一个 DSH_HOME 的终端中进行。

页面操作：

1. Settings → Models：在 DeepSeek 模型提供商中配置自己的 API Key，保存后选择可用模型。凭据只在本机配置。
2. Choose workspace：添加并选择本 Demo 的绝对目录。没有选择工作区时，输入框可能不可用。
3. 新建会话，发送 `docs/01-local-task.txt` 的内容。
4. 观察读取文件、命令执行、测试失败、修改和重新测试的过程。

验收：主测试 7/7 通过；测试断言没有被删除或放宽；修改集中在业务实现。请检查实际 diff 和测试输出。

本段验证的是 Harness 的会话与工具执行。本段不会自动出现 Super-agent 的 Ticket / Thread 面板。

## 3. 准备真实 Super-agent 演练

### 3.1 使用仍带缺陷的原始代码

本地练习后代码可能已修好。请从 ZIP 再解压一份，作为 GitHub 演练的原始仓库，例如 `~/ai-labs/oms-demo-github`。不要覆盖已经做完的本地练习目录。

在 GitHub 新建一个空的测试仓库，例如 `YOUR_LOGIN/oms-harness-demo`。可以用 GitHub 的空仓库指引推送代码；以下命令中的 YOUR_LOGIN 要替换为你的真实账号：

```bash
cd /实际路径/oms-demo-github
git init -b main
git add .
git commit -m "Add inventory synchronization exercise"
git remote add origin https://github.com/YOUR_LOGIN/oms-harness-demo.git
git push -u origin main
```

如果 Git 提示缺少提交身份，请在此仓库设置你自己的 `user.name` 和 `user.email`，再提交。

### 3.2 准备 Agent 账号与工具

真实原型需要独立 GitHub 用户账号作为 Agent，并给它该测试仓库的写权限，由该账号接受邀请。原型读取该账号的 notifications，GitHub App 不能替代它。

本机还需 pnpm、Git 2.32+、GitHub CLI 2.48+、Python 3.8+、tmux。首次只接 GitHub。

原型以运行它的操作系统用户执行命令；worktree 只隔开工作目录。优先使用专门的测试系统账户或开发环境。

### 3.3 让本机 coding agent 按官方流程安装

克隆原型代码：

```bash
git clone https://github.com/deepseek-ai/dsh-superagent-prototype.git
cd dsh-superagent-prototype
```

在你本机的 coding agent 中打开这个原型仓库目录，把本练习包内 `docs/00-setup-task.txt` 的内容粘贴过去，先替换其中 3 个占位值。官方 SETUP.md 本来就按 coding agent 辅助安装设计。

关键设置：

| 项目 | 本次练习取值 |
| --- | --- |
| Source | GitHub only |
| 仓库 | YOUR_LOGIN/oms-harness-demo |
| Agent 账号 | 独立的 GitHub 用户 |
| allowed list | 仅你的个人 GitHub login |
| accept mode | observe；明确的人类请求仍可接单 |
| Worker 数 | 先 1 |
| prepare | 调用本项目 scripts/prepare-worktree.sh 的绝对路径 |
| 交付 | PR + 测试证据；人工评审和合并 |
| profile | 独立的 superagent；已有配置应保留 |

GitHub CLI 的个人登录和 Agent 登录要分开。官方流程会创建凭据 wrapper，并通过 `api user --jq .login` 检查身份。不要把个人账号误配置成 Agent。

### 3.4 安装命令索引

下列命令用于理解安装步骤，推荐由本机 coding agent 按 SETUP.md 检查后执行。原型部署文档当前固定版本为 0.2.1-rc.3；不要无检查升级为 latest。

```bash
npm install -g @deepseek-ai/dsh@0.2.1-rc.3
pnpm install
pnpm build
```

只有不存在 `superagent` profile 的首次安装，才执行：

```bash
dsh superagent --from-default-profile web --dump-config > /dev/null
```

该创建命令会重建同名 profile，已有配置时不要重跑。

在原型仓库根目录注册本地 bundle：

```bash
dsh plugin --profile superagent add "link:$PWD"
```

然后按官方 `examples/profile.cordis.patch.yml` 设置 loopback 地址、3082 端口和真实绝对路径的 stateDir，并配置 GitHub Source、Policy、Executor 和模型。首次通常由面板给出缺失配置。

```bash
scripts/dsh-superagent start
scripts/dsh-superagent status
```

完成配置后，status 应返回 0，GitHub Source 正常，浏览器能看到 Superagent 面板。仅能打开页面并不能证明接入成功。

Execution → prepare 的示例内容（路径需替换）：

```text
bash /Users/你的系统用户名/ai-labs/oms-demo-github/scripts/prepare-worktree.sh
```

`scripts/prepare-worktree.sh` 封装了官方文档的 fetch + worktree 命令，只供原型注入 `DSH_SUPERAGENT_*` 环境变量时调用。不要在普通终端直接运行它。

## 4. 真正走一遍事件闭环

### A. 新请求触发

使用你自己的 GitHub 账号，在测试仓库新建 Issue。标题：`修复库存快照乱序覆盖和重复事件处理`。

正文使用 `docs/02-github-issue.txt`，把 `@AGENT_LOGIN` 换为真实 Agent 账号。

观察：在一次轮询周期后（官方默认约一分钟），面板出现 Ticket；能打开 Worker Session、看到独立目录和分支；执行测试后提交 PR。

### B. 原任务追加反馈

PR 出现后，在同一个 Issue 或该 PR 的新评论中发送 `docs/03-followup.txt`。

这轮追加要求包含新的回归用例，因此应能在原分支/PR 看到新增提交；同时观察 Ticket ID 和 Worker Session 是否延续。已有 followup-check.sh 作为额外检查入口，正常修复后通常会通过。

不要只通过“编辑旧 Issue 正文补 @”测试触发，应该发送一条新评论。

### C. 恢复实验（第一轮稳定后做）

在原型仓库执行：

```bash
scripts/dsh-superagent restart
scripts/dsh-superagent status
```

再在原 Issue / PR 发一条具体的补充要求，检查工单、工作目录、PR 和订阅是否仍然可用。人工停止的任务不会因普通反馈自动恢复；这与服务重启是不同情况。

本轮完成标志：真实 Issue 产生 Ticket；Worker 有独立工作现场；测试通过；PR 可评审；同一任务的后续评论被处理；由人决定是否合并。

## 5. 常见卡点

| 现象 | 先查什么 |
| --- | --- |
| 初始 Java 测试 2 条失败 | 这是故意的业务缺陷；先让 Agent 复现和修复 |
| Cannot find module jdk.compiler | 使用完整 JDK 17+ |
| Web UI 无法输入 | 是否已选择 workspace 和可用模型 |
| Provider 报 401 / missing credential | 在本机 Models 设置核对对应 provider 凭据 |
| 提示 Node engine 不支持 | 使用 Node 24.9+，检查当前终端真正调用的 node |
| Super-agent 插件不兼容 | 按原型 package.json 和部署文档核对 DSH 版本，不用 --force 绕过 |
| @ 后没有 Ticket | Source 是否开启、allowed list 是否包含发言者、是否暂停、是否为新消息 |
| 无法读写仓库 | Agent 独立账号是否已接受邀请，wrapper 登录是否正确 |
| prepare 失败 | 本地原始仓库绝对路径、脚本路径、GitHub访问与 Git 版本 |
| status 返回 3 | 配置未完成；看输出的 setup 字段 |
| status 返回 2 | 某个部件异常；看 errors，单纯重启不一定有效 |
| 已有个人 Harness 配置 | 使用独立 DSH_HOME/profile，保留已有配置，避免重建同名 profile |

## 6. 验证边界与一手资料

本包的 Java 缺陷、基线测试与独立修复副本由制作方做本地验证；详细结果见 VALIDATION.md。真实模型、GitHub 接单和 PR 闭环需要你的本机模型凭据与 GitHub 账号，不包含在 Java 测试结果中。

- Harness 快速开始：https://github.com/deepseek-ai/deepseek-harness
- Web 工作区与任务：https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/user/guide/index.md
- 模型配置：https://github.com/deepseek-ai/deepseek-harness/blob/master/docs/user/guide/providers.md
- 原型安装入口：https://github.com/deepseek-ai/dsh-superagent-prototype/blob/main/SETUP.md
- 原型部署与恢复：https://github.com/deepseek-ai/dsh-superagent-prototype/blob/main/docs/006-deployment.md
- 授权与订阅：https://github.com/deepseek-ai/dsh-superagent-prototype/blob/main/docs/002-authorization.md

上述资料会变化。本文区分“当前本体”与“原型固定版”，实际遇到版本差异时以原型的兼容范围和当前安装版本为准。
# oms-harness-demo
# oms-harness-demo
