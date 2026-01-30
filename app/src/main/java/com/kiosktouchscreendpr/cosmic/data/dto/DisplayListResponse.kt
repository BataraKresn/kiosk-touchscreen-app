package com.kiosktouchscreendpr.cosmic.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response list display dari CMS
 * Endpoint: GET /api/displays
 */
@Serializable
data class DisplayListResponse(
    @SerialName("data")
    val data: List<DisplayListItem> = emptyList(),

    @SerialName("meta")
    val meta: PaginationMeta? = null
)

@Serializable
data class DisplayListItem(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String? = null,
    
    @SerialName("token")
    val token: String? = null
)

@Serializable
data class PaginationMeta(
    @SerialName("current_page")
    val currentPage: Int? = null,

    @SerialName("per_page")
    val perPage: Int? = null,

    @SerialName("total")
    val total: Int? = null
)