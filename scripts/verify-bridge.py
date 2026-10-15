#!/usr/bin/env python3
"""Check that repacking changed only the documented bridge classes."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parent.parent
original = root / "libs/FluxNetworks-1.21.1-8.0.0.jar"
patched = root / "build/bridge/FluxNetworks-1.21.1-8.0.0-fluxbattery.1.jar"
allowed = {
    "sonar/fluxnetworks/common/connection/FluxNetwork",
    "sonar/fluxnetworks/common/connection/ServerFluxNetwork",
    "sonar/fluxnetworks/common/connection/ClientFluxNetwork",
    "sonar/fluxnetworks/common/connection/TransferIterator",
    "sonar/fluxnetworks/common/connection/NetworkStatistics",
    "sonar/fluxnetworks/common/connection/PhantomFluxDevice",
    "sonar/fluxnetworks/common/device/TileFluxDevice",
    "sonar/fluxnetworks/register/Messages",
    "sonar/fluxnetworks/api/device/IFluxRuntimeDevice",
}
with zipfile.ZipFile(original) as upstream, zipfile.ZipFile(patched) as bridge:
    names = bridge.namelist()
    assert len(names) == len(set(names)), "Duplicate zip entries"
    changed = []
    for name in set(upstream.namelist()) | set(names):
        if name.endswith("/"):
            continue
        old = upstream.read(name) if name in upstream.namelist() else None
        new = bridge.read(name) if name in names else None
        if old == new:
            continue
        changed.append(name)
        base = name.removesuffix(".class").split("$")[0]
        assert base in allowed or name in {"FLUX-NETWORKS-LICENSE", "META-INF/MANIFEST.MF"}, f"Unexpected modified entry: {name}"
        assert new is not None, f"Upstream entry removed: {name}"
print("Bridge archive: unchanged upstream resources and unrelated classes verified")
print("Changed entries:", *sorted(changed), sep="\n  ")
print("SHA-256:", hashlib.sha256(patched.read_bytes()).hexdigest())
