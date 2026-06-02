package com.github.db1996.taskerha.datamodels

data class ActualService(
    val id: String,
    val name: String?,
    val description: String?,
    val type: String,
    val domain: String,
    val fields: MutableList<HaServiceField>,
    @Deprecated("Migrated to synthetic entity_id field")
    val targetEntity: Boolean,
    @Deprecated("Migrated to multipleEntities property on entity_id field")
    val broadEntityTarget: Boolean = false
)
