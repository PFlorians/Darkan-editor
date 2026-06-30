package darkan.editor.plugin

import darkan.editor.plugin.IPlugin
import darkan.editor.plugin.PluginDescriptor

@PluginDescriptor(name = "317 Archive Plugin", authors = ["Nshusa"])
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