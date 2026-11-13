# Troubleshooting

[Deutsch](Fehlerbehebung) · **English**

![Synthetic check: invalid priority prevents saving.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/battery-validation.png)

The image shows an intentionally invalid priority in the synthetic client checks. The dialog stays open and does not transmit that invalid draft.

| Symptom | Check and remedy |
|---|---|
| Flux version/hash startup failure | Install the documented original Flux Networks 8.0.0 file and remove replacement/bridge JARs. Renaming a file does not bypass validation. |
| Mandatory patch conflict | The log identifies the changed required structure. Compare the mod list and isolate the conflicting transformer. |
| Right-click opens the machine | Hold the battery and Shift-right-click, or right-click the air. |
| No networks listed | Create a Flux network, check permanent access and reopen the dialog. |
| Held battery remains disconnected | Expected: it becomes active only in a supported machine slot. |
| Empty battery rejected | Configure and save a network first; verify the Mekanism slot and multiblock exclusion. |
| Machine receives no energy | Check network source/Flux Plug, saved network, permissions, slot, priority and limits. |
| Upgraded Miner too slow | Old batteries retain their limit. Adjust battery and source Plug limits, and check supply and machine demand. |
| Charging slot feeds no energy | The local battery buffer fills first. The network then needs demand. Ordinary FE chargers do not provide automatic input. |
| Pending FE remain in tooltip | Accepted delivery energy remains in the item. Reinsert it into a permitted Mekanism charging slot. |
| Second battery does not connect | Only the first matching battery in a machine becomes a virtual network device. |
| Chunk loading has no effect | Check the saved item option, server and Flux permissions, owner quota and active host binding. |
| Miner stops beyond machine chunk | Follow Mekanism's Anchor rules for additional mining chunks. |
| Save rejected | Correct invalid fields; reopen if the held item or permissions changed. Client and server must use protocol 2. |

## Report a problem

Include addon, Minecraft, NeoForge, Mekanism, KFF and Flux versions, the role (Point/Plug), machine and slot, and steps to reproduce. Add relevant logs and the JAR hash, without credentials. [Open an issue](https://github.com/TeutonStudio/FluxBatterie/issues).

0.1.3-beta.2 remains a prerelease. Full two-player, actual chunk-unload/return and multi-hour modpack acceptance is still outstanding.
