package com.pineypiney.pixelgameimporter.model

import glm_.vec2.Vec2
import glm_.vec3.Vec3

data class Geometry(val id: String, val name: String, val vertices: Array<ModelImporter.VertexPosition>, val normals: Array<Vec3>, val texMaps: Array<Vec2>)