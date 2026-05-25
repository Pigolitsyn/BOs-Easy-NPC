# Deploy Fabric Mods

Copy built Fabric jars from this project into the Modrinth instance mods folder, excluding `-sources` and `-javadoc` jars.

Use this skill when the user says "deploy", "deploy fabric", "перемести моды", "deploy fabric mods", or invokes `/deploy-fabric`.

<skill>
## Destination

`/home/pigolitsyn/.local/share/ModrinthApp/profiles/instance to mine/versions/g42g3g/mods/`

## Source jars

- `/home/pigolitsyn/BOs-Easy-NPC/core/Fabric/build/libs/*-fabric-*.jar`
- `/home/pigolitsyn/BOs-Easy-NPC/config-ui/Fabric/build/libs/*-fabric-*.jar`
- `/home/pigolitsyn/BOs-Easy-NPC/bundle/Fabric/build/libs/*-fabric-*.jar` (if exists)

## Steps

1. Verify destination directory exists. Bail out with error if missing — do not create it.

```bash
DEST="/home/pigolitsyn/.local/share/ModrinthApp/profiles/instance to mine/versions/g42g3g/mods"
[ -d "$DEST" ] || { echo "Destination missing: $DEST"; exit 1; }
```

2. Copy main jars only. Filter out `-sources.jar` and `-javadoc.jar`. Use one command per source dir to keep output readable.

```bash
DEST="/home/pigolitsyn/.local/share/ModrinthApp/profiles/instance to mine/versions/g42g3g/mods"
for d in /home/pigolitsyn/BOs-Easy-NPC/core/Fabric/build/libs \
         /home/pigolitsyn/BOs-Easy-NPC/config-ui/Fabric/build/libs \
         /home/pigolitsyn/BOs-Easy-NPC/bundle/Fabric/build/libs; do
  [ -d "$d" ] || continue
  for jar in "$d"/*-fabric-*.jar; do
    [ -f "$jar" ] || continue
    case "$jar" in
      *-sources.jar|*-javadoc.jar) continue ;;
    esac
    cp -v "$jar" "$DEST/"
  done
done
```

3. List final destination contents matching `easy_npc*`.

```bash
ls -lh "/home/pigolitsyn/.local/share/ModrinthApp/profiles/instance to mine/versions/g42g3g/mods/" | grep easy_npc
```

4. Report which jars copied. If a `build/libs` dir was missing or had no matching jar, mention it — user may need to run `/build` first.
</skill>
