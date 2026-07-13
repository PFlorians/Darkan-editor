package darkan.editor.plugin

import darkan.editor.io.RSBuffer
import darkan.editor.plugin.extension.ConfigExtension

@PluginDescriptor(name = "Varbit Plugin", authors = ["Nshusa"], version = "2.0.0")
class Plugin : ConfigExtension(), IPlugin {

    private var high: Int = 0
    private var low: Int = 0
    private var setting: Int = 0

    override fun getFileName(): String {
        return "varbit"
    }

    override fun getModernIndexId(): Int = 22

    override fun getModernBitShift(): Int = 10

    override fun decode(currentIndex: Int, buffer: RSBuffer) {
        while (true) {
            val opcode = buffer.readUByte()

            if (opcode == 0) {
                break
            }

            if (opcode == 1) {
                setting = buffer.readUShort()
                low = buffer.readUByte()
                high = buffer.readUByte()
            }
        }
    }

    override fun encode(buffer: RSBuffer) {
        buffer.writeByte(1)
        buffer.writeShort(setting)
        buffer.writeByte(low)
        buffer.writeByte(high)
        buffer.writeByte(0)
    }

    override fun fxml(): String {
        return "scene.fxml"
    }

    override fun stylesheets(): Array<String> {
        return arrayOf("css/style.css")
    }

    override fun applicationIcon(): String {
        return "icons/icon.png"
    }

}