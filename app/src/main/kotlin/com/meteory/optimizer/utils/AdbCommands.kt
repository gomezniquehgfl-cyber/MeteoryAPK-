package com.meteory.optimizer.utils

object AdbCommands {

    // ─── Rendering ───────────────────────────────────────────────────────

    const val FORCE_VULKAN   = "setprop debug.hwui.renderer skiavk"
    const val FORCE_OPENGLES = "setprop debug.hwui.renderer opengles"
    const val RESET_RENDERER = "setprop debug.hwui.renderer"

    // ─── Display / Refresh ───────────────────────────────────────────────

    fun setRefreshRate(hz: Int) = "service call SurfaceFlinger 1035 i32 $hz"
    const val RESET_REFRESH    = "service call SurfaceFlinger 1035 i32 0"

    // ─── CPU Governor ────────────────────────────────────────────────────

    fun setCpuGovernor(core: Int, governor: String) =
        "echo $governor > /sys/devices/system/cpu/cpu$core/cpufreq/scaling_governor"

    fun setCpuMaxFreq(core: Int, freq: Long) =
        "echo $freq > /sys/devices/system/cpu/cpu$core/cpufreq/scaling_max_freq"

    fun setCpuMinFreq(core: Int, freq: Long) =
        "echo $freq > /sys/devices/system/cpu/cpu$core/cpufreq/scaling_min_freq"

    fun lockCpuPerf(coreCount: Int): List<String> = (0 until coreCount).map {
        setCpuGovernor(it, "performance")
    }

    fun resetCpuGov(coreCount: Int): List<String> = (0 until coreCount).map {
        setCpuGovernor(it, "schedutil")
    }

    // ─── GPU ─────────────────────────────────────────────────────────────

    const val GPU_PERF_MODE     = "echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor"
    const val GPU_RESET_MODE    = "echo msm-adreno-tz > /sys/class/kgsl/kgsl-3d0/devfreq/governor"
    const val GPU_MAX_FREQ      = "cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq"
    const val GPU_CUR_FREQ      = "cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq"

    // ─── RAM / Process ───────────────────────────────────────────────────

    fun killPackage(pkg: String)         = "am force-stop $pkg"
    fun freezePackage(pkg: String)       = "pm disable $pkg"
    fun unfreezePackage(pkg: String)     = "pm enable $pkg"
    fun clearAppCache(pkg: String)       = "pm clear --cache-only $pkg"
    fun setPriority(pid: Int, prio: Int) = "renice -n $prio -p $pid"

    const val DROP_CACHES        = "sync; echo 3 > /proc/sys/vm/drop_caches"
    const val COMPACT_MEMORY     = "echo 1 > /proc/sys/vm/compact_memory"
    const val SET_SWAPPINESS_LOW = "sysctl -w vm.swappiness=10"

    // ─── Network ─────────────────────────────────────────────────────────

    const val TCP_NO_DELAY     = "sysctl -w net.ipv4.tcp_nodelay=1"
    const val TCP_FAST_OPEN    = "sysctl -w net.ipv4.tcp_fastopen=3"
    const val WIFI_SCAN_ALWAYS = "settings put global wifi_scan_always_enabled 0"
    const val WIFI_SLEEP_OFF   = "settings put global wifi_sleep_policy 2"

    // ─── Do Not Disturb / Gaming ─────────────────────────────────────────

    const val DND_ON            = "cmd notification set_dnd on"
    const val DND_OFF           = "cmd notification set_dnd off"
    const val ANIMATIONS_OFF    = "settings put global window_animation_scale 0.5"
    const val ANIMATIONS_RESET  = "settings put global window_animation_scale 1.0"
    const val TRANSITIONS_OFF   = "settings put global transition_animation_scale 0.5"
    const val TRANSITIONS_RESET = "settings put global transition_animation_scale 1.0"

    // ─── Battery / Thermal ───────────────────────────────────────────────

    const val THERMAL_ENGINE_OFF = "stop thermal-engine"
    const val THERMAL_ENGINE_ON  = "start thermal-engine"
    const val BATTERY_SAVER_OFF  = "settings put global low_power 0"

    // ─── Logcat ──────────────────────────────────────────────────────────

    const val LOGCAT_CLEAR = "logcat -c"
    const val LOGCAT_LEVEL = "setprop log.tag.Choreographer SUPPRESS"
}
