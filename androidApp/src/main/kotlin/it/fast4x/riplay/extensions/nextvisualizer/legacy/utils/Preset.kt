package it.fast4x.riplay.extensions.nextvisualizer.legacy.utils

import android.graphics.*
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.Painter
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.fft.FftBar
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.fft.FftCLine
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.fft.FftCWaveRgb
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.misc.Background
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.misc.Icon
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.modifier.Compose
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.modifier.Rotate
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.modifier.Scale
import it.fast4x.riplay.extensions.nextvisualizer.legacy.painters.modifier.Shake

class Preset {
    companion object {

        /**
         * Feel free to add your awesome preset here ;)
         * Hint: You can use `Compose` painter to group multiple painters together as a single painter
         */
        fun getPreset(name: String): Painter {
            return when (name) {
                "debug" -> FftBar()
                else -> FftBar()
            }
        }

        fun getPresetWithBitmap(name: String, bitmap: Bitmap): Painter {
            return when (name) {
                "cIcon" -> Compose(Rotate(FftCLine()), Icon(Icon.getCircledBitmap(bitmap)))
                "cWaveRgbIcon" -> Compose(
                    Rotate(FftCWaveRgb()),
                    Icon(Icon.getCircledBitmap(bitmap))
                )
                "liveBg" -> Scale(Shake(Background(bitmap)), scaleX = 1.02f, scaleY = 1.02f)
                "debug" -> Icon(bitmap)
                else -> Icon(bitmap)
            }
        }
    }
}