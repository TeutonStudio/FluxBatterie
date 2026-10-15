#!/usr/bin/env python3
"""Two real server JVMs, isolated test world, mandatory pass-marker checks."""
from pathlib import Path
import subprocess
import re

root = Path(__file__).resolve().parent.parent
config = root / "run-restart/defaultconfigs/fluxbattery-server.toml"
config.parent.mkdir(parents=True, exist_ok=True)
config.write_text("allowChunkLoading=true\nmaxTicketsPerOwner=4\nbatteryCapacityFE=1000000000\n")
for stage in ("prepare", "verify"):
    log = root / f"build/restart-{stage}.log"
    log.parent.mkdir(exist_ok=True)
    with log.open("w") as output:
        result = subprocess.run([str(root / "gradlew"), "runGameTestServer", f"-PrestartStage={stage}", "--no-daemon", "--max-workers=2"], cwd=root, stdout=output, stderr=subprocess.STDOUT)
    text = log.read_text(errors="replace")
    if result.returncode or not re.search(r"All \d+ required tests passed", text):
        raise SystemExit(f"Restart {stage} failed; inspect {log}")
    print(f"Restart {stage}: passed ({log})")
