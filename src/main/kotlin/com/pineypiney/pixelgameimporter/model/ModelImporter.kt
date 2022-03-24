package com.pineypiney.pixelgameimporter.model

import com.pineypiney.game_engine.resources.models.animations.Animation
import com.pineypiney.game_engine.resources.models.animations.BoneState
import com.pineypiney.game_engine.resources.models.animations.KeyFrame
import com.pineypiney.pixelgameimporter.util.DataSource
import com.pineypiney.pixelgameimporter.util.getAttribute
import com.pineypiney.pixelgameimporter.util.getFirstAttribute
import com.pineypiney.pixelgameimporter.util.getFirstAttributes
import glm_.f
import glm_.i
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.math.PI

class ModelImporter private constructor(){

    fun loadModel(stream: InputStream): Model {

        // https://www.hameister.org/KotlinXml.html
        val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = builder.parse(stream)
        val path: XPath = xPath

        val meshes: MutableList<Mesh> = mutableListOf()

        val geometryMap: Map<String, Geometry> = loadGeometries(doc, path)
        val bones: Array<Bone> = loadNodes(doc, path)
        val controllerMap: Map<String, Controller> = loadControllers(doc, path)
        val physics: Physics = loadPhysics(doc, path)
        val animations: Array<Animation> = loadAnimations(doc, path)

        stream.close()

        controllerMap.forEach { (_, controller) ->
            val geo: Geometry? = geometryMap[controller.meshName.removePrefix("#")]
            if(geo != null){

                // Construct Mesh Vertices
                val vertices: MutableList<Mesh.MeshVertex> = mutableListOf()

                geo.vertices.indices.forEach { i ->
                    val pos: VertexPosition = geo.vertices[i].transform(controller.matrix)
                    val normal: Vec3 = geo.normals[i]
                    val texMap: Vec2 = geo.texMaps[i]

                    val weightsMap = controller.weights[pos.id]
                    val weights: Array<Controller.BoneWeight> = weightsMap.map {
                        val bone = bones.first { b -> b.sid == it.key }
                        Controller.BoneWeight(bone.id, bone.name, it.value)
                    }.toTypedArray()

                    vertices.add(Mesh.MeshVertex(pos, normal, texMap, weights))
                }

                meshes.add(Mesh(vertices.toTypedArray()))
            }
        }

        return Model(meshes.toTypedArray(), bones.getOrNull(0)?.getRoot(), animations, physics)
    }

    private fun loadGeometries(doc: Document, path: XPath = xPath): Map<String, Geometry>{

        // Get a list of the available geometries and their IDs
        val geometryRoot = "/PGM/meshes/mesh"
        val geometries = path.evaluate(geometryRoot, doc, XPathConstants.NODESET) as NodeList
        val geoMap: MutableMap<String, Geometry> = mutableMapOf()

        val geometryIDs: MutableList<String> = mutableListOf()
        for (x in 0 until geometries.length) {

            val value = getAttribute(geometries.item(x).attributes, "id", "")
            geometryIDs.add(value)
        }

        // Iterate through each geometry, using their IDs to access them specifically
        geometryIDs.forEach geo@ { id ->

            // More details
            val name: String = getFirstAttribute(doc, "$geometryRoot[contains(@id, '$id')]", "name", id)
            val meshRoot = "$geometryRoot[contains(@id, '$id')]"

            // Read the Sources
            val sourceRoot = "$meshRoot/source"
            val verticesSource = DataSource.readDataFromXML<Float>("$id-positions", sourceRoot, "float", doc, path)
            val normalsSource = DataSource.readDataFromXML<Float>("$id-normals", sourceRoot, "float", doc, path)
            val texMapSource = DataSource.readDataFromXML<Float>("$id-map-0", sourceRoot, "float", doc, path)

            val verticesList: List<VertexPosition> = verticesSource.arrays.indices.map { i -> VertexPosition(i, Vec3(verticesSource[i])) }
            val normalsList: List<Vec3> = normalsSource.arrays.map { Vec3(it) }
            val texMapsList: List<Vec2> = texMapSource.arrays.map { Vec2(it) }

            // -------- Compile into triangles -------

            val trianglesRoot = "$meshRoot/triangles"

            // Read the indices
            val indices = (path.evaluate("$trianglesRoot/p", doc, XPathConstants.STRING) as String)
            val indicesArray = indices.split(" ").map { index -> index.i }

            // Get attributes (offsets, count, stride)
            val vOffset: Int = getFirstAttribute(doc, "$trianglesRoot/input[contains(@semantic, 'VERTEX')]", "offset", 0, path)
            val nOffset: Int = getFirstAttribute(doc, "$trianglesRoot/input[contains(@semantic, 'NORMAL')]", "offset", 0, path)
            val tOffset: Int = getFirstAttribute(doc, "$trianglesRoot/input[contains(@semantic, 'TEXCOORD')]", "offset", 0, path)

            val attributes = getFirstAttributes(doc, trianglesRoot)
            val count = getAttribute(attributes , "count", 0)
            val stride = getAttribute(attributes, "stride", (maxOf(maxOf(vOffset, nOffset), tOffset) + 1) * 3)

            // vS is the stride per vertex, 1/3rd of the triangle stride because 3 vertices make a triangle
            val vS = stride / 3


            // There are not enough index numbers
            if(indicesArray.size < count * stride){
                return@geo
            }

            val meshVertices: MutableList<VertexPosition> = mutableListOf()
            val meshNormals: MutableList<Vec3> = mutableListOf()
            val meshTexMaps: MutableList<Vec2> = mutableListOf()

            // Make triangles from arrays and indices
            for(x in 0 until count){

                // Create 3 MeshVertices from the source data

                meshVertices.add(verticesList[indicesArray[x * stride + vOffset]])
                meshVertices.add(verticesList[indicesArray[x * stride + vS + vOffset]])
                meshVertices.add(verticesList[indicesArray[x * stride + (2 * vS) + vOffset]])

                meshNormals.add(normalsList.getOrElse(indicesArray[x * stride + nOffset]){ normalsList[0] })
                meshNormals.add(normalsList.getOrElse(indicesArray[x * stride + vS + nOffset]){ normalsList[0] })
                meshNormals.add(normalsList.getOrElse(indicesArray[x * stride + (2 * vS) + nOffset]){ normalsList[0] })

                meshTexMaps.add(texMapsList[indicesArray[x * stride + tOffset]])
                meshTexMaps.add(texMapsList[indicesArray[x * stride + vS + tOffset]])
                meshTexMaps.add(texMapsList[indicesArray[x * stride + (2 * vS) + tOffset]])
            }

            geoMap[id] = Geometry(id, name, meshVertices.toTypedArray(), meshNormals.toTypedArray(), meshTexMaps.toTypedArray())
        }
        return geoMap.toMap()
    }
    private fun loadNodes(doc: Document, path: XPath = xPath): Array<Bone>{

        val nodeRoot = "/PGM/bones/node"
        val nodes = path.evaluate(nodeRoot, doc, XPathConstants.NODESET) as NodeList
        val boneMap: MutableList<Bone> = mutableListOf()

        val nodeIDs: MutableList<String> = mutableListOf()
        for (x in 0 until nodes.length) {
            val value = getAttribute(nodes.item(x).attributes, "id", "")
            nodeIDs.add(value)
        }

        val rootId: String = nodeIDs.firstOrNull() ?: return arrayOf()
        val matrixString = (path.evaluate("$nodeRoot[contains(@id, '$rootId')]/matrix[contains(@sid, 'transform')]", doc, XPathConstants.STRING) as String)
        val floats = matrixString.split(" ").map { it.f }
        val transform = Mat4(floats)

        val idBuffer = IntArray(1) {0}

        val rootBone = Bone(null, 0, getFirstAttribute(doc, "$nodeRoot[contains(@id, '$rootId')]", "name", rootId, path), getFirstAttribute(doc, "$nodeRoot[contains(@id, '$rootId')]", "sid", rootId, path), transform)
        boneMap.add(rootBone)
        boneMap.addAll(loadNodeChildren("$nodeRoot[contains(@id, '$rootId')]/node", rootBone, idBuffer, doc, path))

        return boneMap.toTypedArray()
    }
    private fun loadControllers(doc: Document, path: XPath = xPath): Map<String, Controller>{

        val controllerRoot = "/PGM/mesh_controllers/controller"
        val controllers = path.evaluate(controllerRoot, doc, XPathConstants.NODESET) as NodeList
        val controlMap: MutableMap<String, Controller> = mutableMapOf()

        val controllerIDs: MutableList<String> = mutableListOf()
        for (x in 0 until controllers.length) {

            val value = getAttribute(controllers.item(x).attributes, "id", "")
            controllerIDs.add(value)
        }

        controllerIDs.forEach control@ { id ->

            val name: String = getFirstAttribute(doc, "$controllerRoot[contains(@id, '$id')]", "name", id, path)
            val skinRoot = "$controllerRoot[contains(@id, '$id')]"

            val mesh: String = getFirstAttribute(doc, skinRoot, "mesh", id, path)

            val matArray = (path.evaluate("$skinRoot/bind_shape_matrix", doc, XPathConstants.STRING) as String)
                .split(" ")
                .map { it.f }
            val bindMatrix = Mat4(matArray)

            // Read the Sources
            val sourceRoot = "$skinRoot/source"
            val joints = DataSource.readDataFromXML<String>("$id-joints", sourceRoot, "string", doc, path)
            val posesRoot = "$skinRoot/source[contains(@id, '$id-bind-poses')]"
            val weights = DataSource.readDataFromXML<Float>("$id-weights", sourceRoot, "float", doc, path)

            // -------- Compile into weights -------

            val weightsRoot = "$skinRoot/vertex_weights"
            val vCounts = (path.evaluate("$weightsRoot/vcount", doc, XPathConstants.STRING) as String)
                .trim()
                .split(" ")
                .map { it.i }

            val v = (path.evaluate("$weightsRoot/v", doc, XPathConstants.STRING) as String)
                .split(" ")
                .map { it.i }

            var i = 0
            var bId = 0;
            val boneWeights: MutableList<Map<String, Float>> = mutableListOf()
            vCounts.forEach { count ->
                val map: MutableMap<String, Float> = mutableMapOf()
                for(index in 0 until count){
                    map[joints[v[i], 0]] = weights[v[i + 1], 0]
                    i += 2
                }
                boneWeights.add(map.toMap())
                bId++
            }

            controlMap[id] = Controller(id, name, mesh, bindMatrix, boneWeights)
        }
        return controlMap.toMap()
    }
    private fun loadPhysics(doc: Document, path: XPath = xPath): Physics {
        val physicsRoot = "/PGM/physics"
        val colliderRoot = "$physicsRoot/collider"

        val originString = path.evaluate("$colliderRoot/origin", doc, XPathConstants.STRING) as String
        val origin = Vec2(originString.split(" ").map { s -> s.f })
        val sizeString = path.evaluate("$colliderRoot/size", doc, XPathConstants.STRING) as String
        val size = Vec2(sizeString.split(" ").map { s -> s.f})

        return Physics(origin, size)
    }
    private fun loadAnimations(doc: Document, path: XPath = xPath): Array<Animation>{

        val animationRoot = "/PGM/animations/animation"
        val animationMap: MutableList<Animation> = mutableListOf()

        val animations = path.evaluate(animationRoot, doc, XPathConstants.NODESET) as NodeList
        val animationsIDs: MutableList<String> = mutableListOf()
        for (x in 0 until animations.length) {

            val value = getAttribute(animations.item(x).attributes, "id", "")
            animationsIDs.add(value)
        }

        animationsIDs.forEach animation@ { id ->

            val stateMap: MutableMap<Float, MutableList<BoneState>> = mutableMapOf()
            val animRoot = "$animationRoot[contains(@id, '$id')]/animation"

            val bones = path.evaluate(animRoot, doc, XPathConstants.NODESET) as NodeList
            val boneIDs: MutableList<String> = mutableListOf()
            for (x in 0 until bones.length) {

                val value = getAttribute(bones.item(x).attributes, "id", "")
                boneIDs.add(value)
            }

            boneIDs.forEach bone@ { bone ->

                // Read the Sources
                val sourceRoot = "$animRoot[contains(@id, '$bone')]/source"
                val times = DataSource.readDataFromXML<Float>("$bone-time", sourceRoot, "float", doc, path)
                val translations =
                    DataSource.readDataFromXML<Float>("$bone-translation", sourceRoot, "float", doc, path)
                val rotations = DataSource.readDataFromXML<Float>("$bone-rotation", sourceRoot, "float", doc, path)
                val interpolations =
                    DataSource.readDataFromXML<String>("$bone-interpolation", sourceRoot, "string", doc, path)

                // -------- Compile into Animation -------

                val timesArray = times.arrays
                val tranArray = translations.arrays
                val rotArray = rotations.arrays

                if(timesArray.size >= rotArray.size){
                    timesArray.indices.forEach { i ->
                        val time = timesArray[i][0]
                        val translation = Vec2(tranArray[i].toFloatArray())
                        val rotation = rotArray[i][0]

                        // Initialise list if it doesn't yet exist, and don't forget to convert degrees to radians
                        if(!stateMap.containsKey(time)) stateMap[time] = mutableListOf()
                        stateMap[time]?.add(BoneState(bone.removePrefix("Animation_"), rotation * -PI.f/180f, translation))
                    }
                }
            }

            val frames: MutableList<KeyFrame> = mutableListOf()
            stateMap.forEach { (time, states) ->
                frames.add(KeyFrame(time, states.toTypedArray()))
            }

            animationMap.add(Animation(id, frames.sortedBy { key -> key.time }.toTypedArray()))
        }
        return animationMap.toTypedArray()
    }

    private fun loadNodeChildren(root: String, parent: Bone, idBuffer: IntArray, doc: Document, path: XPath = xPath): Collection<Bone>{

        val nodes = path.evaluate(root, doc, XPathConstants.NODESET) as NodeList
        val nodeIDs: MutableList<String> = mutableListOf()
        val boneList: MutableList<Bone> = mutableListOf()

        for (x in 0 until nodes.length) {
            val value = getAttribute(nodes.item(x).attributes, "id", "")
            nodeIDs.add(value)
        }

        nodeIDs.forEach node@ { id ->
            val matrixString = (path.evaluate("$root[contains(@id, '$id')]/matrix[contains(@sid, 'transform')]", doc, XPathConstants.STRING) as String)
            val floats = matrixString.split(" ").map { it.f }
            val transform = Mat4(floats)

            idBuffer[0]++
            val child = Bone(parent, idBuffer[0], getFirstAttribute(doc, "$root[contains(@id, '$id')]", "name", id, path), getFirstAttribute(doc, "$root[contains(@id, '$id')]", "sid", id, path), transform)
            parent.addChild(child)

            boneList.add(child)
            boneList.addAll(loadNodeChildren("$root[contains(@id, '$id')]/node", child, idBuffer, doc, path))
        }

        return boneList
    }

    companion object {
        val xPath: XPath = XPathFactory.newInstance().newXPath()
    }

    data class VertexPosition(val id: Int, var pos: Vec3) {

        fun transform(m: Mat4): VertexPosition {
            return VertexPosition(this.id, Vec3(m * Vec4(this.pos)))
        }
    }
}