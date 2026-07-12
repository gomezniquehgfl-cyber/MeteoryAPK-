package com.meteory.optimizer.data

data class DeviceProfile(
    val brand: String,
    val models: List<String>,
    val cpuGovernorGaming: String = "performance",
    val cpuGovernorNormal: String = "schedutil",
    val gpuGovernor: String       = "performance",
    val preferredRenderer: String = "auto",
    val maxHz: Int                = 60,
    val thermalThrottleTemp: Int  = 55,
    val ramThresholdMb: Int       = 512,
    val notes: String             = ""
)

object DeviceProfiles {

    private val profiles = listOf(
        DeviceProfile(
            brand    = "Samsung",
            models   = listOf("SM-A", "SM-G", "SM-S", "SM-F", "SM-M"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "energy_step",
            gpuGovernor       = "performance",
            preferredRenderer = "skiavk",
            maxHz             = 120,
            thermalThrottleTemp = 58,
            notes  = "Exynos: ajuste conservador de temperatura; Snapdragon: performance agresivo"
        ),
        DeviceProfile(
            brand    = "Xiaomi",
            models   = listOf("2201", "2211", "2107", "23028", "22071", "Redmi", "POCO", "M2"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "schedutil",
            gpuGovernor       = "performance",
            preferredRenderer = "skiavk",
            maxHz             = 144,
            thermalThrottleTemp = 60,
            notes  = "MIUI/HyperOS: deshabilitar MI SCAN y limpieza automática del sistema"
        ),
        DeviceProfile(
            brand    = "Realme",
            models   = listOf("RMX", "CPH"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "schedutil",
            gpuGovernor       = "performance",
            preferredRenderer = "skiavk",
            maxHz             = 120,
            thermalThrottleTemp = 57,
            notes  = "GT Mode activo recomendado; deshabilitar Arreglador de RAM de ColorOS"
        ),
        DeviceProfile(
            brand    = "Honor",
            models   = listOf("LGE-", "DNN-", "MNA-", "NTH-", "PGT-", "VNE-"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "schedhorizon",
            gpuGovernor       = "performance",
            preferredRenderer = "skiavk",
            maxHz             = 90,
            thermalThrottleTemp = 55,
            notes  = "Kirin: governor personalizado; evitar temperatura > 55°C"
        ),
        DeviceProfile(
            brand    = "Motorola",
            models   = listOf("XT", "moto", "edge", "razr"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "schedutil",
            gpuGovernor       = "performance",
            preferredRenderer = "opengles",
            maxHz             = 144,
            thermalThrottleTemp = 60,
            notes  = "Android puro: mínima interferencia; OpenGL suele ser más estable en Moto"
        ),
        DeviceProfile(
            brand    = "Tecno",
            models   = listOf("KC", "KG", "KH", "POVA", "SPARK", "CAMON", "PHANTOM"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "interactive",
            gpuGovernor       = "performance",
            preferredRenderer = "opengles",
            maxHz             = 90,
            thermalThrottleTemp = 53,
            notes  = "HiOS: temp crítica baja; preferir OpenGL para estabilidad"
        ),
        DeviceProfile(
            brand    = "Infinix",
            models   = listOf("X", "HOT", "NOTE", "ZERO", "GT"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "interactive",
            gpuGovernor       = "performance",
            preferredRenderer = "opengles",
            maxHz             = 90,
            thermalThrottleTemp = 53,
            notes  = "XOS: similar a TECNO; throttle agresivo, controlar temperatura"
        ),
        DeviceProfile(
            brand    = "OnePlus",
            models   = listOf("IN2", "LE2", "NE", "CPH", "DN"),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "walt",
            gpuGovernor       = "performance",
            preferredRenderer = "skiavk",
            maxHz             = 120,
            thermalThrottleTemp = 62,
            notes  = "OxygenOS: HyperBoost disponible; umbral de temp alto"
        ),
        DeviceProfile(
            brand    = "Generic",
            models   = listOf(""),
            cpuGovernorGaming = "performance",
            cpuGovernorNormal = "schedutil",
            gpuGovernor       = "performance",
            preferredRenderer = "auto",
            maxHz             = 60,
            thermalThrottleTemp = 55,
            notes  = "Perfil genérico universal"
        )
    )

    fun findProfile(brandOrModel: String): DeviceProfile {
        val query = brandOrModel.lowercase()
        return profiles.firstOrNull { p ->
            p.brand.lowercase() in query ||
            p.models.any { m -> m.lowercase() in query || query.contains(m.lowercase()) }
        } ?: profiles.last()
    }

    fun getAllBrands(): List<String> = profiles.dropLast(1).map { it.brand }
}
