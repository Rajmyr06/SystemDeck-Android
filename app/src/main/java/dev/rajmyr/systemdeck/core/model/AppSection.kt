package dev.rajmyr.systemdeck.core.model

enum class AppSection(
    val label: String,
    val phase: String,
) {
    Overview("overview", "ready"),
    Performance("performance", "phase 6"),
    Cpu("cpu", "phase 6"),
    Memory("memory", "phase 6"),
    Battery("battery", "phase 6"),
    Thermal("thermal", "phase 6"),
    Network("network", "phase 6"),
    Storage("storage", "phase 6"),
    Gpu("gpu", "phase 6"),
    Sensors("sensors", "phase 6"),
    Device("device", "ready"),
    Diagnostics("diagnostics", "ready"),
    Settings("settings", "ready"),
}
