package com.pineypiney.pixelgameimporter.level

import com.pineypiney.pixelgameimporter.util.DataSource
import com.pineypiney.pixelgameimporter.util.getAttribute
import glm_.f
import glm_.i
import glm_.min
import glm_.vec2.Vec2
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class LevelImporter private constructor(){

    private fun getLevelInfo(fileName: String, stream: InputStream): LevelDetails? {
        val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = builder.parse(stream)
        val path: XPath = xPath

        val detailsRoot = "PGL/level"
        val name = getStringAt("$detailsRoot/name", doc, path)
        val width =
            try {
                getStringAt("$detailsRoot/width", doc, path).f
            } catch (e: NumberFormatException) {
                System.err.println("Could not parse width in level $fileName")
                e.printStackTrace()
                return null
            }

        val creationString = getStringAt("$detailsRoot/creation-date", doc, path)
        val editString = getStringAt("$detailsRoot/edit-date", doc, path)

        val formatter = LevelDetails.formatter

        val creationTime = try {
            LocalDateTime.parse(creationString, formatter)
        } catch (e: DateTimeParseException) {
            LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
        }
        val editTime = try {
            LocalDateTime.parse(editString, formatter)
        } catch (e: DateTimeParseException) {
            LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
        }

        return LevelDetails(name, fileName, width, creationTime, editTime)
    }

    fun loadLevel(name: String, stream: InputStream): MutableMap<String, MutableSet<ObjectState>> {
        val itemCollection = mutableMapOf<String, MutableSet<ObjectState>>()

        val doc = getDocument(stream)
        val path = xPath
        val layersRoot = "PGL/layers/layer"

        val layers = path.evaluate(layersRoot, doc, XPathConstants.NODESET) as NodeList

        val layerIDs: MutableSet<String> = mutableSetOf()
        for (x in 0 until layers.length) {

            val value = getAttribute(layers.item(x).attributes, "id", "")
            layerIDs.add(value)
        }

        layerIDs.forEach layer@ { id ->

            val layer = try{
                id.i
            }
            catch (e: java.lang.NumberFormatException){
                System.err.println("Could not parse layer $id in level $name")
                e.printStackTrace()
                return@layer
            }

            val itemsRoot = "$layersRoot[@id = $id]/item"

            val items = path.evaluate(itemsRoot, doc, XPathConstants.NODESET) as NodeList

            val itemIDs: MutableSet<String> = mutableSetOf()
            for (x in 0 until items.length) {
                val value = getAttribute(items.item(x).attributes, "name", "")
                itemIDs.add(value)
            }

            itemIDs.forEach item@ { item ->

                val itemRoot = "$itemsRoot[contains(@name, '$item')]"

                // Read the Sources
                val sourceRoot = "$itemRoot/source"
                val translationsSource = DataSource.readDataFromXML<Float>("$item-translations", sourceRoot, "float", doc, path)
                val rotationsSource = DataSource.readDataFromXML<Float>("$item-rotations", sourceRoot, "float", doc, path)
                val scalesSource = DataSource.readDataFromXML<Float>("$item-scales", sourceRoot, "float", doc, path)

                val translationsList = translationsSource.arrays.map { array -> Vec2(array) }
                val rotationsList = rotationsSource.arrays.map { array -> array[0] }
                val scalesList = scalesSource.arrays.map { array -> Vec2(array) }

                val total = translationsList.size.min(rotationsList.size).min(scalesList.size)
                for (i in 0 until total) {
                    val position = translationsList[i]
                    val rotation = rotationsList[i]
                    val scale = scalesList[i]
                    val depth = layer

                    itemCollection[item]?.add(ObjectState(position, rotation, scale, depth))
                }
            }
        }

        return itemCollection
    }

    private fun getDocument(stream: InputStream): Document{
        val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(stream)
    }

    fun getStringAt(root: String, doc: Document, path: XPath = xPath): String{
        return path.evaluate(root, doc, XPathConstants.STRING) as String
    }

    companion object {
        val xPath: XPath = XPathFactory.newInstance().newXPath()
    }
}