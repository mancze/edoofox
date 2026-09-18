# Working conventions

- Use minimalist progress messages.
- Make logical commits autonomously as work is completed and verified. Do not push unless requested. Exclude APKs, machine-specific configuration, signing keys, and unrelated user changes.
- Test with the separate `.qa` application ID. Do not clear, inspect, or modify the user's signed-in app data. Avoid taking over an emulator the user is actively using; use a separate background instance when needed.
- Docker Desktop is installed. If `docker` is not on PATH, locate it or ask the user to start Docker Desktop before treating it as unavailable.
