package pl.foodhub.pos.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PosAppVersionDto(
    @SerialName("versionCode") val versionCode: Int,
)
