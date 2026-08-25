package darkan.editor.plugin

@PluginDescriptor(name = "Sprite Plugin", authors = ["Nshusa"], version = "2.0.0")
class Plugin : IPlugin {

    override fun applicationIcon(): String {
        return "icons/icon.png"
    }

    override fun fxml(): String {
        return "scene.fxml"
    }

    override fun stylesheets(): Array<String> {
        return arrayOf("css/style.css")
    }
}
