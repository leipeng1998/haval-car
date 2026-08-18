package br.com.redesurftank.havalshisuku.models

enum class SteeringWheelCustomActionType(val key: String, val description: String) {
    DEFAULT("default", "车机默认"),
    CHANGE_REGENERATION_LEVEL(
            "change_regeneration_level",
            "切换动能回收等级：低、中、高"
    ),
    CHANGE_POWER_MODE("power_mode", "切换动力模式：HEV、EV、EV 优先"),
    TOGGLE_ANION("toggle_anion", "开关空调负离子"),
    TOGGLE_ESP("toggle_esp", "开关车身稳定控制（ESP）"),
    TOGGLE_ONE_PEDAL_DRIVING("toggle_one_pedal_driving", "开关单踏板驾驶"),
    OPEN_APP("open_app", "打开指定应用"),
    CLIMATE_COMMAND("climate_command", "执行空调指令"),
    TOGGLE_CAMERA_AVM("toggle_avm", "切换驻车时关闭摄像头模式"),
    OPEN_AVM_ONCE("open_avm_once", "打开摄像头且不中断");

    companion object {
        fun fromKey(key: String): SteeringWheelCustomActionType? {
            return entries.find { it.key == key }
        }
    }
}
