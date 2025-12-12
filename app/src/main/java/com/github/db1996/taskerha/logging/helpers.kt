package com.github.db1996.taskerha.logging

enum class LogChannel {
    GENERAL,
    WEBSOCKET
}

enum class LogLevel(val priority: Int, val label: String) {
    OFF(100, "Don't save logs"),
    ERROR(40, "Only save errors, ex: CallService failed with error"),
    WARN(30, "Save warnings and errors"),
    INFO(20, "Save info and warnings/errors, ex: CallService success"),
    DEBUG(10, "Save debug info and warnings/errors, ex: Starting websocket, reconnecting websocket"),
    VERBOSE(0, "Save everything, ex: all websocket messages");

    fun allows(level: LogLevel): Boolean = level.priority >= this.priority

    companion object {
        fun fromString(value: String?, default: LogLevel): LogLevel {
            return runCatching { value?.let { LogLevel.valueOf(it) } }.getOrNull() ?: default
        }
    }
}
