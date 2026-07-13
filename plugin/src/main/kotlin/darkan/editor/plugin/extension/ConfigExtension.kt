package darkan.editor.plugin.extension

import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import darkan.editor.fs.RSArchive
import darkan.editor.fs.RSFileStore
import darkan.editor.fs.CacheSystem
import darkan.editor.fs.CacheFormat
import darkan.editor.fs.CacheSystemHolder
import darkan.editor.io.RSBuffer
import darkan.editor.plugin.PluginDescriptor
import darkan.editor.shared.model.KeyModel
import darkan.editor.util.getFileNameWithoutExtension
import java.io.IOException
import darkan.editor.util.mapToInstance

import java.util.*
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Double
import java.lang.Exception
import java.lang.reflect.Modifier


abstract class ConfigExtension : IPluginExtension {

    open abstract fun getFileName(): String

    open fun getStoreId(): Int {
        return RSFileStore.ARCHIVE_FILE_STORE
    }

    open fun getFileId(): Int {
        return RSArchive.CONFIG_ARCHIVE
    }

    open fun useMetaFile(): Boolean {
        return true
    }

    open fun readLength(buffer: RSBuffer): Int {
        return buffer.readUShort()
    }

    open fun writeLength(buffer: RSBuffer, size: Int) {
        buffer.writeShort(size)
    }

    open fun writeOffset(metaBuf: RSBuffer, dataBuf: RSBuffer, lastPos: Int) {
        metaBuf.writeShort(dataBuf.position - lastPos)
    }

    open fun setInitialDataBufOffset(buffer: RSBuffer) {
        buffer.position = 2
    }

    protected abstract fun decode(currentIndex: Int, buffer: RSBuffer)

    open fun onLoad(list: ObservableList<KeyModel>, archive: RSArchive) {
        try {
            val dataBuf = RSBuffer.wrap(archive.readFile(getDataFileName()).array())

            var length: Int

            if (useMetaFile()) {
                val metaBuf = RSBuffer.wrap(archive.readFile(getMetaFileName()).array())
                length = readLength(metaBuf)
                setInitialDataBufOffset(dataBuf)
            } else {
                length = readLength(dataBuf)
            }

            for (i in 0 until length) {
                val instance = this.javaClass.getConstructor().newInstance()
                instance.decode(i, dataBuf)
                val set = RSPropertySet().mapInstanceFields(instance)
                val model = KeyModel(i, set.getOrDefault("name", "null"), instance)
                model.map = TreeMap(set.properties)
                list.add(model)
            }

            onFinish(dataBuf)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    open fun getModernIndexId(): Int = -1

    open fun getModernBitShift(): Int = 0

    open fun onLoadModern(list: ObservableList<KeyModel>, cache: CacheSystem) {
        try {
            val indexId = getModernIndexId()
            if (indexId < 0) {
                return
            }

            val index = cache.getIndex(indexId) ?: return
            val bitShift = getModernBitShift()
            val archiveIds = index.archiveIds

            for (archiveId in archiveIds) {
                val archive = index.getArchive(archiveId) ?: continue
                val fileIds = archive.fileIds

                for (fileId in fileIds) {
                    val defId = (archiveId shl bitShift) or fileId
                    val fileData = cache.readFile(indexId, archiveId, fileId) ?: continue
                    if (fileData.remaining() == 0) continue

                    try {
                        val instance = this.javaClass.getConstructor().newInstance()
                        instance.decode(defId, RSBuffer.wrap(fileData.array()))
                        val set = RSPropertySet().mapInstanceFields(instance)
                        val model = KeyModel(defId, set.getOrDefault("name", "null"), instance)
                        model.map = TreeMap(set.properties)
                        list.add(model)
                    } catch (ex: Exception) {
                        // Skip definitions that fail to decode
                        System.err.println("Failed to decode definition $defId: ${ex.message}")
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            showError(ex)
        }
    }

    open fun onSaveModern(list: ObservableList<KeyModel>, cache: CacheSystem) {
        try {
            if (list.isEmpty()) {
                return
            }

            val indexId = getModernIndexId()
            if (indexId < 0) {
                return
            }

            val bitShift = getModernBitShift()

            for (item in list) {
                val instance = item.instance as ConfigExtension
                item.map.mapToInstance(instance)

                val dataBuf = RSBuffer.init()
                instance.encode(dataBuf)

                val defId = item.id
                val archiveId = defId ushr bitShift
                val fileId = defId and ((1 shl bitShift) - 1)

                cache.writeFile(indexId, archiveId, fileId, dataBuf.toArray())
            }

            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Info"
            alert.headerText = "Success!"
            Platform.runLater { alert.show() }

        } catch (ex: Exception) {
            ex.printStackTrace()
            showError(ex)
        }
    }

    open fun showError(ex: Exception) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Error"
        alert.headerText = "Look, there's an error!"

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        ex.printStackTrace(pw)
        val exceptionText = sw.toString()

        val label = Label("The exception stacktrace was:")

        val textArea = TextArea(exceptionText)
        textArea.isEditable = false
        textArea.isWrapText = true

        textArea.maxWidth = Double.MAX_VALUE
        textArea.maxHeight = Double.MAX_VALUE
        GridPane.setVgrow(textArea, Priority.ALWAYS)
        GridPane.setHgrow(textArea, Priority.ALWAYS)

        val expContent = GridPane()
        expContent.maxWidth = Double.MAX_VALUE
        expContent.add(label, 0, 0)
        expContent.add(textArea, 0, 1)

        alert.dialogPane.expandableContent = expContent

        Platform.runLater {
            alert.showAndWait()
        }
    }

    open fun onSave(list: ObservableList<KeyModel>, archive: RSArchive) {
        try {
            if (list.isEmpty()) {
                return
            }

            val metaBuf = RSBuffer.init()
            val dataBuf = RSBuffer.init()

            writeLength(dataBuf, list.size)

            if (useMetaFile()) {
                writeLength(metaBuf, list.size)
            }

            for (i in 0 until list.size) {
                val item = list[i]
                val instance = item.instance as ConfigExtension
                item.map.mapToInstance(instance)

                var lastPos = dataBuf.position

                instance.encode(dataBuf)

                if (useMetaFile()) {
                    writeOffset(metaBuf, dataBuf, lastPos)
                }
            }

            archive.writeFile(getDataFileName(), dataBuf.toArray())

            if (useMetaFile()) {
                archive.writeFile(getMetaFileName(), metaBuf.toArray())
            }

            val cache = CacheSystemHolder.get() ?: return
            val encoded = archive.encode() ?: return

            if (cache.writeFile(getStoreId(), getFileId(), encoded)) {
                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Info"
                alert.headerText = "Success!"
                Platform.runLater { alert.show() }
            }

        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun onFinish(buffer: RSBuffer) {
        var name = this.javaClass.simpleName

        if (this.javaClass.isAnnotationPresent(PluginDescriptor::class.java)) {
            val plugin = this.javaClass.getAnnotation(PluginDescriptor::class.java)
            name = plugin.name
        }

        if (buffer.position != buffer.capacity()) {
            val alert = Alert(Alert.AlertType.WARNING)
            alert.title = "Warning"
            alert.headerText =
                String.format("%s was not fully loaded pos=%d capacity=%d", name, buffer.position, buffer.capacity())
            Platform.runLater { alert.show() }
        }
    }

    protected abstract fun encode(buffer: RSBuffer)

    open fun dataFileNameSuffix(): String {
        return DATA_SUFFIX
    }

    open fun metaFileNameSuffix(): String {
        return META_SUFFIX
    }

    private fun getDataFileName(): String {
        return "${getFileName().getFileNameWithoutExtension()}.${dataFileNameSuffix()}"
    }

    private fun getMetaFileName(): String {
        return "${getFileName().getFileNameWithoutExtension()}.${metaFileNameSuffix()}"
    }

    companion object {

        const val DATA_SUFFIX = "dat"
        const val META_SUFFIX = "idx"

        class RSPropertySet {

            var properties: Map<String, Any> = HashMap()

            fun <T : Any> getOrDefault(name: String, value: T): T {
                return (properties as Map<String, Any>).getOrDefault(name, value) as T
            }

            fun mapInstanceFields(instance: Any): RSPropertySet {
                val map = HashMap<String, Any>()
                val set = RSPropertySet()
                set.properties = map

                try {
                    val fields = instance.javaClass.declaredFields

                    for (field in fields) {

                        if (Modifier.isStatic(field.modifiers)) {
                            continue
                        }

                        field.isAccessible = true

                        val name = field.name

                        val value = field.get(instance) ?: continue

                        map[name] = value
                    }
                } catch (ex: kotlin.Exception) {
                    ex.printStackTrace()
                }
                return set
            }
        }
    }
}
