# Feeding the network: Flux Plug

[Deutsch](Netzwerk-einspeisen) · **English**

![Actual energy cube with a full battery in the item charging slot.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-charging-slot.png)

In a supported Mekanism block's **item charging slot**, the same configured battery becomes a **virtual Flux Plug**. Use, for example, an energy cube's plus/charging slot.

## Energy flow

1. Select the destination network and save the battery configuration.
2. Supply energy to the charging block and provide network demand, such as a Point or Flux storage.
3. Insert the battery in the charging slot.
4. The block fills the local battery buffer first. Only then is surplus energy accepted for network delivery.
5. Flux Networks distributes that energy normally; priority, permissions and limit still apply.

The image shows an actual capture with an already full battery in an Ultimate Energy Cube's charging slot. The capture run additionally verified that a separate Flux storage received energy. Initial energy was supplied to the test scene; this does not demonstrate autonomous solar generation.

## No demand and pending energy

If the network cannot accept energy, further surplus is rejected and remains in the charging block. Energy already accepted but not yet distributed stays in the battery separately. Its tooltip lists “FE pending network delivery (charging slot)”. Removing the battery or restarting does not delete it. Reinserting the battery in a supported charging slot resumes delivery.

An ordinary FE item charger can fill the local buffer without enabling automatic network delivery. That requires the Mekanism host adapter. Complex multiblocks and multiple virtual battery devices in one machine are unsupported.

## Inspect in Flux Networks

Open an ordinary device in the destination network and select its connections tab. The battery appears at the machine location with its name, item and Plug role. [Media provenance](Media-Provenance) includes the actual registration screenshot. Instantaneous throughput may return to zero once consumers are full.
