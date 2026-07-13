package darkan.editor.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.StageStyle
import darkan.editor.fs.CacheSystem
import darkan.editor.fs.CacheSystemFactory
import darkan.editor.fs.CacheSystemHolder
import java.nio.file.Path

class App : Application() {

    override fun init() {
        Settings.load()
    }

    override fun start(stage: Stage) {
        mainStage = stage

        val root : Parent = FXMLLoader.load(App::class.java.getResource("/scenes/StoreScene.fxml"))
        stage.title = "Darkan Editor [build $VERSION]"
        val scene = Scene(root)
        scene.stylesheets.add(App::class.java.getResource("/style.css").toExternalForm())
        stage.scene = scene
        stage.icons.add(Image(App::class.java.getResourceAsStream("/icons/icon.png")))
        stage.centerOnScreen()
        stage.isResizable = false
        stage.initStyle(StageStyle.UNDECORATED)
        stage.show()
    }

    override fun stop() {
        val c = cache
        if (c != null && c.isLoaded) {
            Settings.save(c.root)
        }
    }

    companion object {
        const val VERSION = "3.1.0"

        var cache: CacheSystem? = null
            private set

        lateinit var mainStage : Stage

        fun openCache(path: Path) {
            cache?.close()
            val system = CacheSystemFactory.open(path)
            if (system.load()) {
                cache = system
                CacheSystemHolder.set(system)
            }
        }

        fun closeCache() {
            cache?.close()
            cache = null
            CacheSystemHolder.set(null)
        }

        @JvmStatic
        fun main(args : Array<String>) {
            launch(App::class.java)
        }
    }

}
