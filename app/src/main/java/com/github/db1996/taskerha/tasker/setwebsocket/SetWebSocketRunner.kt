package com.github.db1996.taskerha.tasker.setwebsocket

import android.content.Context
import android.os.Build
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.tasker.base.BaseTaskerRunner
import com.github.db1996.taskerha.tasker.base.RunnerResult

class SetWebSocketRunner : BaseTaskerRunner<SetWebSocketInput, SetWebSocketOutput>() {

    override val needsClient = false

    override val logTag: String
        get() = "SetWebSocketRunner"

    override suspend fun execute(
        context: Context,
        input: SetWebSocketInput
    ): RunnerResult<SetWebSocketOutput> {
        return if (input.enabled == "true") {
            val targetId = input.instanceId.ifBlank {
                HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
            }
            // Mirror applyWsEnable(): disable wsEnabled on all other instances, enable on target
            HaInstanceRepository.instances.value
                .filter { it.wsEnabled && it.id != targetId }
                .forEach { HaInstanceRepository.update(it.copy(wsEnabled = false)) }
            val target = HaInstanceRepository.getById(targetId) ?: HaInstanceRepository.getDefault()
            if (target != null) {
                HaInstanceRepository.update(target.copy(wsEnabled = true))
                HaInstanceRepository.setActive(target.id)
                logInfo("Set active instance to ${target.id}")
            }
            HaSettings.saveWebSocketEnabled(context, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                HaWebSocketService.start(context)
                logInfo("WebSocket service started")
            } else {
                logError("Cannot start WebSocket service: requires Android 8.0+")
            }
            RunnerResult.Success(SetWebSocketOutput())
        } else {
            // Mirror disableWs(): clear wsEnabled on the active instance
            val active = HaInstanceRepository.getActive()
            if (active != null) {
                HaInstanceRepository.update(active.copy(wsEnabled = false))
            }
            HaSettings.saveWebSocketEnabled(context, false)
            HaWebSocketService.stop(context)
            logInfo("WebSocket service stopped")
            RunnerResult.Success(SetWebSocketOutput())
        }
    }
}
