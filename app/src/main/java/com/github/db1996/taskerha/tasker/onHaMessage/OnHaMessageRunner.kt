package com.github.db1996.taskerha.tasker.onHaMessage

import android.content.Context
import com.github.db1996.taskerha.logging.CustomLogger
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied

class OnHaMessageRunner :
    TaskerPluginRunnerConditionEvent<
            OnHaMessageInput,
            OnHaMessageOutput,
            OnHaMessageOutput
            >() {

    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnHaMessageInput>,
        update: OnHaMessageOutput?
    ): TaskerPluginResultCondition<OnHaMessageOutput> {
        val type = update?.type?.trim() ?: ""
        val message = update?.message?.trim() ?: ""

        if(!input.regular.type.isBlank() && input.regular.type != type){
            return TaskerPluginResultConditionUnsatisfied()
        }
        if(!input.regular.message.isBlank() && input.regular.message != message){
            return TaskerPluginResultConditionUnsatisfied()
        }

        CustomLogger.i("HaWebSocketService", "update: type=${type}, message=${message}")
        return TaskerPluginResultConditionSatisfied(context, update)
    }
}
