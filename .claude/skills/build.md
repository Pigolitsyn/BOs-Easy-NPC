# Build Easy NPC

Build the Easy NPC project (Common, Fabric, NeoForge subprojects). Forge is skipped — requires Java 25 which is not installed.

Use this skill when the user says "собери проект", "build", "build project", "скомпилируй", or invokes /build.

<skill>
## Steps

1. Build core subprojects:

```bash
cd /home/pigolitsyn/BOs-Easy-NPC/core && bash gradlew :Common:build :Fabric:build :NeoForge:build --continue 2>&1
```

2. Build config-ui subprojects:

```bash
cd /home/pigolitsyn/BOs-Easy-NPC/config-ui && bash gradlew :Common:build :Fabric:build :NeoForge:build --continue 2>&1
```

3. Report results: which tasks succeeded, which failed, and any compilation errors.
</skill>
