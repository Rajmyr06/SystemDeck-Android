package dev.rajmyr.systemdeck.core.model

enum class AppSection(
    val label: String,
    val phase: String,
) {
    Overview("overview", "live"),
    Performance("performance", "live"),
    Cpu("cpu", "live"),
    Memory("memory", "live"),
    Battery("battery", "live"),
    Thermal("thermal", "live"),
    Network("network", "live"),
    Storage("storage", "live"),
    Gpu("gpu", "experimental"),
    Sensors("sensors", "live"),
    Device("device", "ready"),
    Alerts("alerts", "ready"),
    Diagnostics("diagnostics", "live"),
    Settings("settings", "ready"),
}
