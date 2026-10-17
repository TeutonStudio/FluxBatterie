#!/usr/bin/env python3
"""Check the shipped addon, pristine Flux input and generated runtime classpaths."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parent.parent
original = root / 'libs/FluxNetworks-1.21.1-8.0.0.jar'
assert hashlib.sha256(original.read_bytes()).hexdigest() == '29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6'
addon = root / 'build/libs/fluxbattery-0.1.0-dev.jar'
with zipfile.ZipFile(addon) as jar:
    entries = jar.namelist()
    assert len(entries) == len(set(entries)), 'duplicate ZIP entries'
    assert not any(n.startswith('sonar/') for n in entries), 'upstream replacement class in addon'
    assert 'de/teutonstudio/fluxbattery/bridge/FluxRuntimeDevice.class' in entries
    assert 'META-INF/coremods.json' in entries
    assert 'coremods/fluxbattery.js' in entries
    assert 'coremods/fluxbattery-8.0.0.json' in entries
    assert not any(n.startswith(('de/teutonstudio/fluxbattery/tools/', 'de/teutonstudio/fluxbattery/compat/')) for n in entries)
    assert 'de/teutonstudio/fluxbattery/bridge/LoadTimePatched.class' not in entries
for path in (root / 'build/moddev').rglob('*'):
    if path.is_file() and path.suffix in ('.txt', '.args'):
        text = path.read_text(errors='replace')
        assert '8.0.0-fluxbattery.1.jar' not in text, f'stale bridge runtime dependency: {path}'
print('Distribution verified: single addon, no sonar classes, pristine Flux 8.0.0 input')
print('Addon SHA-256:', hashlib.sha256(addon.read_bytes()).hexdigest())
