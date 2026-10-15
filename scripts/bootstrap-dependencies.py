#!/usr/bin/env python3
"""Fetch the pinned upstream jar. No credentials; verify before replacing local input."""
import hashlib
from pathlib import Path
from urllib.request import urlopen

root = Path(__file__).resolve().parent.parent
path = root / "libs/FluxNetworks-1.21.1-8.0.0.jar"
expected = "29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6"
if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() == expected:
    print("Flux Networks 8.0.0: verified")
else:
    url = "https://cursemaven.com/curse/maven/flux-networks-248020/6089446/flux-networks-248020-6089446.jar"
    with urlopen(url, timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != expected:
        raise SystemExit("Upstream checksum mismatch; refusing to use this jar")
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".download")
    temporary.write_bytes(data)
    temporary.replace(path)
    print("Flux Networks 8.0.0: downloaded and verified")
