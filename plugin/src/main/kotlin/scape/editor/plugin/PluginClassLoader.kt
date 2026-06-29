package scape.editor.plugin

import java.net.URL
import java.net.URLClassLoader

class PluginClassLoader(url: URL) : URLClassLoader(arrayOf(url))