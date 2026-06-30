package darkan.editor.plugin

interface IPlugin {

    fun fxml(): String

    fun stylesheets(): Array<String>

    fun applicationIcon(): String

}