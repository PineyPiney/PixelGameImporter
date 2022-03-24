package com.pineypiney.pixelgameimporter.util

import com.pineypiney.pixelgameimporter.model.ModelImporter
import org.w3c.dom.Document
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants

class DataSource <T> (val id: String, val count: Int, val stride: Int, val arrays: Array<Array<T>>) {

    operator fun get(index: Int): Array<T>{
        return try{
            arrays[index]
        }
        catch (e: ArrayIndexOutOfBoundsException){
            println("Tried to access index $index of DataSource with array $arrays")
            e.printStackTrace()
            arrays[0]
        }
    }

    operator fun get(index: Int, indey: Int): T{
        return try{
            arrays[index][indey]
        }
        catch (e: ArrayIndexOutOfBoundsException){
            println("Tried to access index $index, $indey of DataSource with array $arrays")
            e.printStackTrace()
            arrays[0][0]
        }
    }

    companion object{

        @Throws(TypeCastException::class)
        inline fun <reified T> readDataFromXML(id: String, root: String, dataType: String, doc: Document, path: XPath = ModelImporter.xPath): DataSource<T> {
            val arrayRoot = "$root[contains(@id, '$id')]/${dataType}_array[contains(@id, '$id-array')]"

            val atts = getFirstAttributes(doc, "$root[contains(@id, '$id')]/technique_common/accessor[contains(@source, '#$id-array')]", path)
            val count = getAttribute(atts, "count", 0)
            val stride = getAttribute(atts, "stride", 0)

            val stringArray = (path.evaluate(arrayRoot, doc, XPathConstants.STRING) as String)
                .split(" ")
            if(stringArray.joinToString { it }.isEmpty() || stringArray.size < count * stride) return DataSource("", 0, 0, arrayOf())

            val list: List<T> = stringArray.map { convertString(it, convert(it, T::class.java)) }

            val array: Array<List<T>> = Array(count){ listOf() }
            for(i in 0 until count){
                array[i] = list.subList(i * stride, (i+1) * stride)
            }

            return DataSource(id, count, stride, array.map { it.toTypedArray() }.toTypedArray())
        }
    }
}