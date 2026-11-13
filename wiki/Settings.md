# Settings and energy display

[Deutsch](Einstellungen) · **English**

![Synthetic client check: enabled switches and maximum transfer-limit value.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/battery-switches.png)

These settings configure the virtual Flux device in both Point and Plug operation. They apply only after saving.

| Field | Meaning |
|---|---|
| Name | Up to 24 characters, used as the device name in Flux Networks. |
| Priority | Integer from −9999 to 9999. Higher values take precedence in Flux Networks. |
| Limit | Non-negative FE per network cycle, up to 9,223,372,036,854,775,807. New batteries default to 100,000,000. |
| Surge mode | Uses Flux Networks surge priority for allocation. It does not create energy. |
| Disable limit | Ignores the entered transfer limit. The value is retained and temporarily read-only. |
| Chunk loading | Requests chunk loading while installed, subject to [server rules](Chunk-Loading). |

Existing configured batteries retain their saved limits. Updating does not automatically raise them. High throughput also requires sufficient energy supply, a suitable limit on the regular Flux Plug and a machine able to accept energy. Unlimited transfer does not replace an energy source.

## Buffer and validation

The compact display shows fill percentage, stored energy and capacity. Its tooltip provides exact FE values. These are values **when the screen opened**, not a live reading or the network's total energy.

Invalid priority values and missing/overflowing limits prevent saving. Correct the field; the draft is preserved. The server independently rechecks identity, ownership and permissions.

The image comes from the synthetic client checks and shows enabled options with the maximum long limit. It demonstrates rendering and widget actions, not a machine chunk actually being kept loaded.
