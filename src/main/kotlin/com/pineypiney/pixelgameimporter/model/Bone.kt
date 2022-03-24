package com.pineypiney.pixelgameimporter.model

import glm_.mat4x4.Mat4

class Bone(val parent: Bone?, val id: Int, val name: String, val sid: String, val parentTransform: Mat4) {

    private val children: MutableList<Bone> = mutableListOf()

    fun addChild(newBone: Bone){
        children.add(newBone)
    }

    fun getRoot(): Bone{
        return this.parent?.getRoot() ?: this
    }
}