package com.pineypiney.pixelgameimporter.model

import com.pineypiney.game_engine.resources.models.animations.Animation

data class Model(val meshes: Array<Mesh>, val bone: Bone?, val animations: Array<Animation>, val physics: Physics)
