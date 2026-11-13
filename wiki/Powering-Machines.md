# Powering machines: Flux Point

[Deutsch](Maschinen-versorgen) · **English**

![Actual Energized Smelter: battery in the discharging slot and real iron-ingot output.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-powering-machine.png)

In a supported Mekanism machine's energy discharging slot, the battery registers as a **virtual Flux Point**. The normal Flux distributor fills its buffer; Mekanism's item energy adapter transfers that energy into the machine.

## Set up

1. Supply your network through a regular Flux Plug.
2. Configure and save the battery for that network.
3. Insert it in the slot from which the machine discharges energy items. Do not confuse this with an item charging slot.
4. Add processing materials if required and check energy and operation.

No controller or additional Flux Point at the machine is required. The battery need not be precharged. The image shows a real Energized Smelter with a battery installed; the capture run actually produced iron ingots from raw iron.

## Supported machines

The adapter supports Mekanism discharging slots using `fill`/`fillOrConvert`. Tested examples include the **Digital Miner**, **Enrichment Chamber** and **Energized Smelter**. Complex Mekanism `IMultiblock` machines are excluded. Other FE machines do not automatically gain a network connection.

With multiple matching battery slots, only the first matching battery becomes a network device. Further batteries remain ordinary energy items. The first matching slot determines which role wins.

## Remove and move

Removing the battery ends its active connection and any chunk ticket. Identity, settings and energy stay in the item. Insert the same battery in another supported machine to continue there. Permissions and configuration are rechecked during operation.

The Digital Miner still follows its own search, filter and Anchor rules. A battery ticket loads only the machine chunk, not its entire mining area. See [Chunk loading](Chunk-Loading).
