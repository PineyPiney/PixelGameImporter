package com.pineypiney.pixelgameimporter.util

import com.pineypiney.pixelgameimporter.model.ModelImporter
import glm_.d
import glm_.f
import glm_.i
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.NodeList
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants


/**
 * A function to get the value of an attribute called [att] cast to [T] from the first instance that fits the expression [ex]
 *
 * @param doc The Document being read
 * @param ex The expression for the location of the attribute
 * @param att The name of the attribute
 * @param default The value to return if no matching attribute can be found
 *
 * @return The value of the attribute cast to [T], or [default] if no attribute is found
 */
inline fun <reified T> getFirstAttribute(doc: Document, ex: String, att: String, default: T, path: XPath = ModelImporter.xPath): T{
    val attributes = getFirstAttributes(doc, ex, path) ?: return default
    return getAttribute(attributes, att, default)
}

fun getFirstAttributes(doc: Document, ex: String, path: XPath = ModelImporter.xPath): NamedNodeMap?{
    val nodes = path.evaluate(ex, doc, XPathConstants.NODESET) as NodeList
    return nodes.item(0)?.attributes
}

inline fun <reified T> getAttribute(attributes: NamedNodeMap?, att: String, default: T): T{

    val a: String = attributes?.getNamedItem(att)?.nodeValue ?: return default

    return convertString(a, default)
}

inline fun <reified T> convertString(string: String, default: T): T{
    if(default is String) return string as T
    return try{
        T::class.java.cast(when(T::class.java){
            Integer::class.java -> string.i
            java.lang.Float::class.java -> string.f
            java.lang.Double::class.java -> string.d
            else -> default
        })
    }
    catch(e: ClassCastException){
        println("Could not cast $string to ${T::class.java}")
        default
    }
}

inline fun <reified R, reified T> convert(base: R, cls: Class<T>): T {
    if(R::class.java == cls) return base as T
    return cls.cast(when(R::class.java){
        Integer::class.java -> {
            val b = Integer::class.java.cast(base)
            when(cls){
                java.lang.Float::class.java -> b.f
                java.lang.Double::class.java -> b.d
                String::class.java -> b.toString()
                else -> null
            }
        }
        java.lang.Float::class.java -> {
            val b = Float::class.java.cast(base)
            when(cls){
                Integer::class.java -> b.i
                java.lang.Double::class.java -> b.d
                String::class.java -> b.toString()
                else -> null
            }
        }
        java.lang.Double::class.java -> {
            val b = Double::class.java.cast(base)
            when(cls){
                Integer::class.java -> b.i
                java.lang.Float::class.java -> b.f
                String::class.java -> b.toString()
                else -> null
            }
        }
        String::class.java -> {
            val b = String::class.java.cast(base)
            when(cls){
                Integer::class.java -> b.i
                Float::class.java -> b.f
                Double::class.java -> b.d
                else -> null
            }
        }
        else -> null
    })
}