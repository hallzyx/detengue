package com.hallzyx.detengue.ui

import androidx.annotation.StringRes
import com.hallzyx.detengue.R
import com.hallzyx.detengue.core.model.ObjectClass

/** Nombre mostrado al ciudadano para cada clase (PRD §32). */
@StringRes
fun ObjectClass.labelRes(): Int = when (this) {
    ObjectClass.BUCKET -> R.string.object_bucket
    ObjectClass.CONTAINER -> R.string.object_container
    ObjectClass.PLANT_POT -> R.string.object_plant_pot
    ObjectClass.BOTTLE -> R.string.object_bottle
    ObjectClass.TIRE -> R.string.object_tire
    ObjectClass.PET_BOWL -> R.string.object_pet_bowl
    ObjectClass.DRUM -> R.string.object_drum
    ObjectClass.WATER_TANK -> R.string.object_water_tank
}
