package br.com.redesurftank.havalshisuku.models

enum class SteeringWheelClimateCommandType(val key: String, val description: String) {
    TOGGLE_AC("toggle_ac", "AC"),
    TOGGLE_AUTO("toggle_auto", "自动"),
    TOGGLE_POWER("toggle_power", "开关空调"),
    FRONT_DEFROST("front_defrost", "前挡除雾送风");

    companion object {
        fun fromKey(key: String): SteeringWheelClimateCommandType? {
            return entries.find { it.key == key }
        }
    }
}
