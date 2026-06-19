package com.github.db1996.taskerha.tasker.ontriggerstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
@TaskerOutputObject
class OnTriggerStateOutput @JvmOverloads constructor(

    @field:TaskerInputField("rawJson")
    @get:TaskerOutputVariable("ha_raw", labelResIdName = "ha_raw_label")
    var rawJson: String? = null,

    @field:TaskerInputField("entityId")
    @get:TaskerOutputVariable("ha_entity", labelResIdName = "ha_on_trigger_entity_label")
    var entityId: String? = null,

    @field:TaskerInputField("fromState")
    @get:TaskerOutputVariable("ha_from", labelResIdName = "ha_on_trigger_from_label")
    var fromState: String? = null,

    @field:TaskerInputField("toState")
    @get:TaskerOutputVariable("ha_to", labelResIdName = "ha_on_trigger_to_label")
    var toState: String? = null,

    @field:TaskerInputField("forDuration")
    @get:TaskerOutputVariable("ha_for", labelResIdName = "ha_on_trigger_for_label")
    var forDuration: String? = null,


    @field:TaskerInputField("attributesJson")
    @get:TaskerOutputVariable("ha_attrs", labelResIdName = "ha_on_trigger_attrs_label")
    var attributesJson: String? = null,

    @field:TaskerInputField("entityIds")
    @get:TaskerOutputVariable("ha_entities", labelResIdName = "ha_on_trigger_entities_label")
    var entityIds: String? = null,

    @field:TaskerInputField("triggerId")
    var triggerId: String? = null,

    @field:TaskerInputField("haAttr1")
    @get:TaskerOutputVariable("ha_attr_1", labelResIdName = "ha_on_trigger_attr_1_label")
    var haAttr1: String? = null,

    @field:TaskerInputField("haAttr2")
    @get:TaskerOutputVariable("ha_attr_2", labelResIdName = "ha_on_trigger_attr_2_label")
    var haAttr2: String? = null,

    @field:TaskerInputField("haAttr3")
    @get:TaskerOutputVariable("ha_attr_3", labelResIdName = "ha_on_trigger_attr_3_label")
    var haAttr3: String? = null,

    @field:TaskerInputField("haAttr4")
    @get:TaskerOutputVariable("ha_attr_4", labelResIdName = "ha_on_trigger_attr_4_label")
    var haAttr4: String? = null,

    @field:TaskerInputField("haAttr5")
    @get:TaskerOutputVariable("ha_attr_5", labelResIdName = "ha_on_trigger_attr_5_label")
    var haAttr5: String? = null,

    @field:TaskerInputField("haAttr6")
    @get:TaskerOutputVariable("ha_attr_6", labelResIdName = "ha_on_trigger_attr_6_label")
    var haAttr6: String? = null,

    @field:TaskerInputField("haAttr7")
    @get:TaskerOutputVariable("ha_attr_7", labelResIdName = "ha_on_trigger_attr_7_label")
    var haAttr7: String? = null,

    @field:TaskerInputField("haAttr8")
    @get:TaskerOutputVariable("ha_attr_8", labelResIdName = "ha_on_trigger_attr_8_label")
    var haAttr8: String? = null,

    @field:TaskerInputField("haAttr9")
    @get:TaskerOutputVariable("ha_attr_9", labelResIdName = "ha_on_trigger_attr_9_label")
    var haAttr9: String? = null,

    @field:TaskerInputField("haAttr10")
    @get:TaskerOutputVariable("ha_attr_10", labelResIdName = "ha_on_trigger_attr_10_label")
    var haAttr10: String? = null
)
