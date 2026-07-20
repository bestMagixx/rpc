# bm-rpc 依赖发布指南

> 目标：让别人能通过一行 `<dependency>` 引入并使用你的 RPC 框架。

## 一、前置条件（已完成）

本项目已修复以下结构问题，具备发布基础：

| 项 | 修复内容 |
|----|---------|
| starter 继承 | `bm-rpc-spring-boot-starter` 改为继承父 pom，与 core 版本统一为 `1.0-SNAPSHOT` |
| 源码 jar | 根 pom 增加 `maven-source-plugin`，`mvn install/deploy` 自动生成 `-sources.jar` |
| 发布配置 | 根 pom 预留 `distributionManagement`（注释状态），按需开启 |
| 模块隔离 | 示例模块移入 `with-examples` profile，**默认只构建/发布 core + starter 两个核心模块** |

## 二、构建范围：只打包核心，不碰示例

根 pom 的 `<modules>` 现在只保留两个核心模块，示例模块（`bm-rpc-easy`、`example-*`）被放进 `with-examples` profile：

```bash
# 默认：只构建 + 发布 core 和 starter（最干净、体积最小）
mvn clean install
mvn clean deploy

# 想连示例一起构建（本地调试用）
mvn clean install -P with-examples
```

> 说明：Maven 多模块中每个模块**独立打 jar**，示例代码本就不会进 core/starter 的 jar。
> 这里隔离示例的意义是 —— 发布时不编译、不发布示例，避免误传到仓库、加快构建。

## 三、别人如何引入（最终效果）

发布后，使用者只需在 `pom.xml` 加：

```xml
<!-- Spring Boot 项目：引入 starter 即可，core 会作为传递依赖自动引入 -->
<dependency>
    <groupId>com.bestMagixx</groupId>
    <artifactId>bm-rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

启动类加 `@EnableRpc` 注解即可激活框架（通过 `@Import` 触发，无需 spring.factories）。

## 四、五种发布方式（按推荐顺序）

> 💡 **个人 GitHub 项目首选 JitPack（方式 2）**：无需搭私服、读者无需认证，打个 git tag 别人就能引入，是最低成本的上线路径。

### 方式 1：本地安装（最快，仅本机可用）

```bash
# 在项目根目录执行，把 core 和 starter 安装到本机 ~/.m2/repository
mvn clean install
```

- 根 pom 已用 `with-examples` profile 隔离示例，默认只构建 core + starter
- 想连示例一起构建：`mvn clean install -P with-examples`
- 安装后，本机其他项目即可引入；换电脑无效

### 方式 2：JitPack（个人 GitHub 项目推荐，最低成本）

JitPack 会按你的 git tag 自动构建并托管构件，读者**无需任何认证**即可拉取，最适合个人开源项目。

**你只需做一次：**

1. 把版本改为正式版（去掉 `-SNAPSHOT`），根 pom 与 starter 的 `<version>` 统一为 `1.0.0`。
2. 提交并打 tag，推到 GitHub：
   ```bash
   git add -A
   git commit -m "release 1.0.0"
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. 打开 https://jitpack.io/com/github/bestMagixx/rpc ，看到 `v1.0.0` 变绿即构建成功（首次约 1~2 分钟）。

**别人如何引入（多模块仓库，引用 starter 子模块，core 自动传递依赖）：**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.bestMagixx.rpc</groupId>
    <artifactId>bm-rpc-spring-boot-starter</artifactId>
    <version>v1.0.0</version>
</dependency>
```

> 子模块的 groupId 规则是 `com.github.<用户名>.<仓库名>`，artifactId 沿用 pom 里声明的 `bm-rpc-spring-boot-starter`。
> 发新版本只需再打一个 tag（如 `v1.0.1`）并推送，读者改 version 即可。

### 方式 3：私有 Nexus / 阿里云私服（团队/公司内共享）

1. 取消根 pom 中 `distributionManagement` 的注释，填入 Nexus 地址：

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://你的Nexus地址/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <url>http://你的Nexus地址/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

2. 在 `~/.m2/settings.xml` 配置对应 server 的账号密码：

```xml
<servers>
    <server>
        <id>nexus-releases</id>
        <username>你的账号</username>
        <password>你的密码</password>
    </server>
    <server>
        <id>nexus-snapshots</id>
        <username>你的账号</username>
        <password>你的密码</password>
    </server>
</servers>
```

> `<id>` 必须与 pom 中 `<repository>` 的 `<id>` 一致，Maven 据此匹配凭证。

3. 发布：

```bash
mvn clean deploy
```

4. 使用者在 `settings.xml` 加你的 Nexus 为仓库源即可拉取。

### 方式 4：GitHub Packages（开源/个人项目，但读者需认证）

1. 用 GitHub Personal Access Token（PAT，需 `write:packages` 权限）作为密码。
2. 配置 `distributionManagement`：

```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/bestMagixx/rpc</url>
    </repository>
</distributionManagement>
```

3. `settings.xml` 配置凭证（用户名 + PAT）：

```xml
<server>
    <id>github</id>
    <username>bestMagixx</username>
    <password>你的PAT</password>
</server>
```

4. `mvn clean deploy`

5. 使用者需在 `settings.xml`/`pom.xml` 把 GitHub Packages 加为仓库才能拉取（**公开仓库读也需 PAT 认证**，体验不如 JitPack）。

### 方式 5：Maven Central（面向全网公开，门槛最高）

1. **必须先去掉 `-SNAPSHOT`**，发布正式版（如 `1.0.0`）。
2. 注册 [Sonatype JIRA](https://issues.sonatype.org/) 账号，申请 namespace（对应你的域名/groupId）。
3. 根 pom 补全必要元信息（licenses、scm、developers、url、description）。
4. 用 GPG 签名所有构件，配置 `maven-gpg-plugin`。
5. 用 `nexus-staging-maven-plugin` 执行 `mvn clean deploy`，再到 Sonatype 手动 Release。
6. 几小时后同步到 Maven Central，全球可用。

> ⚠️ **groupId 归属门槛**：Central 要求你拥有 groupId 对应的域名。`com.bestMagixx` 需要你拥有 `bestMagixx.com` 域名；
> 若没有，可改用 `com.github.bestMagixx`（凭 GitHub 仓库即可验证归属），但需要同步改所有模块的 groupId。
> 流程较长，建议先用方式 2/3 跑通，有稳定正式版后再走 Central。

## 五、发布正式版（去 SNAPSHOT）

`-SNAPSHOT` 是快照版，适合开发期；对外发布建议用正式版本号：

1. 修改根 pom 的 `<version>` 为正式版（如 `1.0.0`）。
2. `mvn clean deploy`。
3. starter 会通过传递依赖自动带上同版本的 core，使用者只需引 starter 一行。

## 六、验证发布是否成功

```bash
# 本地安装验证
mvn clean install

# 检查本地仓库是否生成 jar + sources
ls ~/.m2/repository/com/bestMagixx/bm-rpc-spring-boot-starter/1.0-SNAPSHOT/
# 应看到: .jar  -sources.jar  .pom
```
