# Getting started

[Deutsch](Erste-Schritte) · **English**

![Battery settings using actual networks from the capture world.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-configuration.png)

A Flux Battery connects a supported Mekanism machine to an ordinary Flux network. The slot determines its role: **discharging slot → Flux Point**, **charging slot → Flux Plug**.

## Power a machine

1. Create a Flux network with permanent access for your player. Connect an energy source through a normal Flux Plug.
2. [Craft a battery](Crafting) and hold it.
3. Right-click the air. When aiming at a block, use **Shift-right-click** to bypass that block's interface.
4. Open **Network selection**, choose the network and return to **Settings**.
5. Enter a name and check priority and limit. New batteries allow 100 million FE per network cycle.
6. Click **Save configuration** and place the battery in the machine's energy input slot. A configured empty battery is accepted too.
7. Open the machine and check its energy buffer and normal processing.

The image shows battery settings opened by the actual integrated server. A held battery is not an active network connection. The energy display reflects the moment the screen opened.

## Send energy back to a network

Place the configured battery in a supported Mekanism block's **item charging slot**, for example the plus slot of an energy cube. It first fills the battery buffer, then supplies network consumers and Flux storage. Follow [Feeding the network](Feeding-the-Network).

Removing the battery ends its connection. Identity, configuration and stored energy stay in the item. The previous machine no longer has a connection through that battery.
