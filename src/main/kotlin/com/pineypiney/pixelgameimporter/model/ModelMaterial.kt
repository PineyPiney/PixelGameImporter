package com.pineypiney.pixelgameimporter.model

import glm_.vec3.Vec3

class ModelMaterial(val name: String, val textures: Map<String, Int>, val baseColour: Vec3 = Vec3(1)) {
}