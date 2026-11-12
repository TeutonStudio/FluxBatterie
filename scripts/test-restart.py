#!/usr/bin/env python3
"""Two real server JVMs, isolated test world, mandatory pass-marker checks."""
from pathlib import Path
import subprocess
import re
import tempfile

root = Path(__file__).resolve().parent.parent
# Each invocation gets a fresh world; only prepare/verify share its persisted state.
# Keep the directory for inspection instead of deleting an earlier test world.
(root / "build").mkdir(exist_ok=True)
run_directory = Path(tempfile.mkdtemp(prefix="restart-world-", dir=root / "build"))
print(f"Restart world: {run_directory}", flush=True)
config = run_directory / "defaultconfigs/fluxbattery-server.toml"
config.parent.mkdir(parents=True, exist_ok=True)
config.write_text("allowChunkLoading=true\nmaxTicketsPerOwner=4\nbatteryCapacityFE=1000000000\n")
for stage in ("prepare", "verify"):
    log = root / f"build/restart-{stage}.log"
    log.parent.mkdir(exist_ok=True)
    with log.open("w") as output:
        result = subprocess.run([str(root / "gradlew"), "runGameTestServer", f"-PrestartStage={stage}", f"-PgameTestDirectory={run_directory}", "-PpackagedAddon", "--offline", "--no-daemon", "--max-workers=2"], cwd=root, stdout=output, stderr=subprocess.STDOUT, timeout=300)
    text = log.read_text(errors="replace")
    if "all mandatory original-8.0.0 runtime patches verified" not in text or result.returncode or not re.search(r"All \d+ required tests passed", text):
        raise SystemExit(f"Restart {stage} failed; inspect {log}")
    print(f"Restart {stage}: passed ({log})", flush=True)
