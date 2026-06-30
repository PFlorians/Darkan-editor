package darkan.editor.gui.model

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import darkan.editor.shared.model.KeyModel

class NamedValueModel(val name: String, val value: Any) {

    val nameProperty = SimpleStringProperty(name)
    val valueProperty = SimpleObjectProperty(ValueModel(KeyModel(-1, "", -1), name, value))

}