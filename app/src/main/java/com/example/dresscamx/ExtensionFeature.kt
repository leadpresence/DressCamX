package com.example.dresscamx

enum class ExtensionFeature(val position: Int) {

    NONE(0),
    BOKEH(1),
    HDR(2),
    NIGHT_MODE(3);

    companion object {

        fun fromPosition(position: Int) : ExtensionFeature {
            return when (position) {
                NONE.position -> NONE
                BOKEH.position -> BOKEH
                HDR.position -> HDR
                NIGHT_MODE.position -> NIGHT_MODE
                else -> NONE
            }
        }
    }
}