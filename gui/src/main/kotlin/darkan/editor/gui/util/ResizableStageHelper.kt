package darkan.editor.gui.util

import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

object ResizableStageHelper {

    private const val BORDER = 8.0
    private const val MIN_WIDTH = 400.0
    private const val MIN_HEIGHT = 300.0

    fun install(stage: Stage) {
        val scene = stage.scene ?: return
        var resizeDir = Cursor.DEFAULT

        scene.addEventFilter(MouseEvent.MOUSE_MOVED) { event ->
            if (!stage.isResizable || stage.isMaximized) {
                scene.cursor = Cursor.DEFAULT
                return@addEventFilter
            }
            scene.cursor = detectEdge(event.sceneX, event.sceneY, scene)
        }

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            if (!stage.isResizable || stage.isMaximized) return@addEventFilter
            resizeDir = detectEdge(event.sceneX, event.sceneY, scene)
        }

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            if (resizeDir == Cursor.DEFAULT) return@addEventFilter

            event.consume()

            val screenX = event.screenX
            val screenY = event.screenY

            var newW = stage.width
            var newH = stage.height
            var newX = stage.x
            var newY = stage.y

            when (resizeDir) {
                Cursor.E_RESIZE, Cursor.NE_RESIZE, Cursor.SE_RESIZE -> {
                    newW = (screenX - stage.x).coerceAtLeast(MIN_WIDTH)
                }
                Cursor.W_RESIZE, Cursor.NW_RESIZE, Cursor.SW_RESIZE -> {
                    val rightEdge = stage.x + stage.width
                    newW = (rightEdge - screenX).coerceAtLeast(MIN_WIDTH)
                    newX = rightEdge - newW
                }
                else -> {}
            }

            when (resizeDir) {
                Cursor.S_RESIZE, Cursor.SE_RESIZE, Cursor.SW_RESIZE -> {
                    newH = (screenY - stage.y).coerceAtLeast(MIN_HEIGHT)
                }
                Cursor.N_RESIZE, Cursor.NE_RESIZE, Cursor.NW_RESIZE -> {
                    val bottomEdge = stage.y + stage.height
                    newH = (bottomEdge - screenY).coerceAtLeast(MIN_HEIGHT)
                    newY = bottomEdge - newH
                }
                else -> {}
            }

            // Force native GTK window resize by constraining min/max to target
            stage.minWidth = newW
            stage.maxWidth = newW
            stage.minHeight = newH
            stage.maxHeight = newH
            stage.x = newX
            stage.y = newY
            stage.width = newW
            stage.height = newH
        }

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED) {
            if (resizeDir != Cursor.DEFAULT) {
                stage.minWidth = MIN_WIDTH
                stage.maxWidth = Double.MAX_VALUE
                stage.minHeight = MIN_HEIGHT
                stage.maxHeight = Double.MAX_VALUE
            }
            resizeDir = Cursor.DEFAULT
        }
    }

    private fun detectEdge(x: Double, y: Double, scene: Scene): Cursor {
        val w = scene.width
        val h = scene.height

        val left = x < BORDER
        val right = x > w - BORDER
        val top = y < BORDER
        val bottom = y > h - BORDER

        return when {
            left && top -> Cursor.NW_RESIZE
            left && bottom -> Cursor.SW_RESIZE
            right && top -> Cursor.NE_RESIZE
            right && bottom -> Cursor.SE_RESIZE
            left -> Cursor.W_RESIZE
            right -> Cursor.E_RESIZE
            top -> Cursor.N_RESIZE
            bottom -> Cursor.S_RESIZE
            else -> Cursor.DEFAULT
        }
    }
}
