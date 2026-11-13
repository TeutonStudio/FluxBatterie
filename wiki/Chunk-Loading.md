# Chunk loading and server configuration

[Deutsch](Chunkloading) · **English**

![Actual server configuration of the separate capture world.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-server-config.png)

Chunk loading is **disabled by default**. Enabling the item option alone is insufficient. Server settings are stored in `fluxbattery-server.toml`; NeoForge chooses the world-specific location, typically the world's `serverconfig` directory.

```toml
allowChunkLoading = false
maxTicketsPerOwner = 4
batteryCapacityFE = 1000000000
```

| Option | Effect |
|---|---|
| `allowChunkLoading` | Allows the addon's ticking battery tickets. |
| `maxTicketsPerOwner` | Maximum tickets per battery owner across all dimensions; 0–1024. |
| `batteryCapacityFE` | Default capacity for new energy states; 1 to Long-Max FE. Existing saved item capacities are not universally migrated. |

The image shows the actual configuration screen for the new capture world. Chunk loading was deliberately permitted for this capture setup. It does not demonstrate actual chunk unloading and returning.

The current beta shows some untranslated configuration keys on this NeoForge page. Use the option names in the table to identify them.

## Requirements

- The server enables `allowChunkLoading`.
- Flux Networks globally permits chunk loading.
- The battery has **Chunk loading** enabled and saved.
- It is active in a supported machine, its network is valid and its owner retains permanent access.
- The owner's shared quota is not exhausted.

An active battery keeps exactly **the machine chunk** ticking. The requirements apply to Point and Plug hosts. The Digital Miner may additionally require Mekanism's Anchor upgrade for its mining chunks. The battery does not replace those rules.

Removing the battery, breaking the machine, losing access or disabling the server permission removes the battery ticket. Restart validates saved tickets again and discards invalid/orphaned ones. Automated restart tests cover this lifecycle without replacing a complete long-running modpack acceptance test.
