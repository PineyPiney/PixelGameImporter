package com.pineypiney.pixelgameimporter.model

import glm_.mat4x4.Mat4

data class Controller(val id: String, val name: String, val meshName: String, val matrix: Mat4, val weights: MutableList<Map<String, Float>>) {

    data class BoneWeight(val id: Int, val boneName: String, val weight: Float = 0f)
}