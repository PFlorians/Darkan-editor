package darkan.editor.plugin.model

import javafx.beans.property.SimpleStringProperty
import darkan.editor.plugin.IPlugin
import darkan.editor.plugin.PluginClassLoader
import darkan.editor.plugin.PluginDescriptor

class PluginWrapper(val plugin: IPlugin, val loader: PluginClassLoader, val path: String, val meta: PluginDescriptor) {
    val nameProperty = SimpleStringProperty(meta.name)

    val versionProperty = SimpleStringProperty(meta.version)

    override fun toString(): String {
        return meta.name
    }
}