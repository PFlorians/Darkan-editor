package darkan.editor.plugin

import darkan.editor.fs.graphics.RSSprite

class SpriteModel(var id: Int, var sprite: RSSprite) {

    override fun toString(): String {
        return id.toString()
    }

}