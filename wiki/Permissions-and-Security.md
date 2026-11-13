# Permissions, ownership and saved data

[Deutsch](Rechte-und-Sicherheit) · **English**

![Networks actually accessible to the logged-in capture player.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-network-selection.png)

A battery is bound to an owner. The first successful save assigns the player as owner. Only that owner can edit its configuration; opening another owner's battery is rejected.

## Network permissions

The list contains accessible networks only. Private or encrypted networks require **permanent ownership or membership**. The battery stores no password, so knowing a password once does not replace membership.

When saving, the server rechecks the held item, its identity, ownership, values and destination network. An older open screen cannot bypass permissions revoked in the meantime. Permissions are checked again during operation, ending unauthorised transfers and tickets if access is lost.

The image shows actual networks available to the logged-in capture player. It does not represent a two-player permissions test. FakePlayer regression covers server-side access cases; complete multiplayer GUI acceptance remains outstanding.

## Moving, saving and restarting

The item stores identity, owner, network ID, name, priority, limit, options, local energy and pending network delivery. Removing or moving it releases old runtime bindings. The same battery can then work in a different supported host, including in another dimension.

Normal survival transfers are checked for energy conservation. Creative/admin copies of entire items also copy their data and fall outside that conservation scenario. Other mods may introduce additional limitations. Back up before changing a modpack or world data.

[Development and tests](Development-and-Tests) distinguishes automated persistence/access checks from the remaining real-player cases.
