package com.pineypiney.pixelgameimporter.model

import glm_.vec2.Vec2
import glm_.vec3.Vec3

// Meshes are made up of faces, which are in turn made up of MeshVertices.
// Mesh vertices are each associated with a position, normal and texMap,
// as well as up to 4 bone weights. The transformation of each vertex is linearly
// interpolated from these 4 bone weights in the shader

data class Mesh(val vertices: Array<MeshVertex>) {

    data class MeshVertex(val position: ModelImporter.VertexPosition, val normal: Vec3 = Vec3(), val texCoord: Vec2 = Vec2(), val weights: Array<Controller.BoneWeight> = arrayOf()){

        override fun toString(): String {
            return "[$position, $normal, $texCoord]"
        }
    }
}