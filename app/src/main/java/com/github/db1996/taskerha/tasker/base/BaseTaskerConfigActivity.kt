package com.github.db1996.taskerha.tasker.base

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

/**
 * Base configuration activity for Tasker plugin actions
 *
 * @param I Input type - the Tasker input class annotated with @TaskerInputRoot
 * @param O Output type - the Tasker output class (use Unit if no output)
 * @param F Form type - the mutable state form used in ViewModel
 * @param B Built form type - the immutable form saved to Tasker
 * @param VM ViewModel type that extends BaseViewModel<F, B>
 *
 * Usage example:
 * ```
 * class MyConfigActivity : BaseTaskerConfigActivity<
 *     MyInput,
 *     MyOutput,
 *     MyForm,
 *     MyBuiltForm,
 *     MyViewModel
 * >() {
 *     override fun createViewModelFactory() = MyViewModelFactory(client)
 *
 *     override fun createHelper() = MyConfigHelper(this)
 *
 *     override fun createScreen(onSave: (MyBuiltForm) -> Unit): @Composable () -> Unit = {
 *         MyScreen(viewModel, onSave)
 *     }
 *
 *     override fun convertBuiltFormToInput(builtForm: MyBuiltForm): MyInput {
 *         return MyInput().apply {
 *             field1 = builtForm.field1
 *         }
 *     }
 *
 *     override fun convertInputToBuiltForm(input: MyInput): MyBuiltForm {
 *         return MyBuiltForm(
 *             field1 = input.field1,
 *             blurb = "Description"
 *         )
 *     }
 * }
 * ```
 */
abstract class BaseTaskerConfigActivity<I : Any, O : Any, F : Any, B : Any, VM : BaseViewModel<F, B>> :
    AppCompatActivity(),
    TaskerPluginConfig<I> {

    override val context: Context
        get() = applicationContext

    /**
     * The ViewModel instance - must be implemented by subclass
     */
    protected abstract val viewModel: VM

    /**
     * The Tasker plugin helper - lazy loaded
     */
    protected val helper: TaskerPluginConfigHelper<I, O, *> by lazy { createHelper() }

    /**
     * Stored built form data (set when saving or restoring)
     */
    protected var currentBuiltForm: B? = null

    /**
     * Create the ViewModelProvider.Factory for this activity
     */
    protected abstract fun createViewModelFactory(): ViewModelProvider.Factory

    /**
     * Create the TaskerPluginConfigHelper for this activity
     */
    protected abstract fun createHelper(): TaskerPluginConfigHelper<I, O, *>

    /**
     * Create the Composable screen for this activity
     *
     * @param onSave Callback to invoke when user saves the configuration
     * @return A Composable function that renders the screen
     */
    protected abstract fun createScreen(onSave: (B) -> Unit): @Composable () -> Unit

    /**
     * Convert a BuiltForm to a Tasker Input object
     */
    protected abstract fun convertBuiltFormToInput(builtForm: B): I

    /**
     * Convert a Tasker Input to a BuiltForm (for restoration)
     */
    protected abstract fun convertInputToBuiltForm(input: I): B

    /**
     * Return null if valid, or an error message string if invalid
     */
    protected open fun validateBeforeSave(builtForm: B): String? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        helper.onCreate()

        setContent {
            TaskerHaTheme {
                val screen = createScreen { builtForm ->
                    handleSave(builtForm)
                }
                screen()
            }
        }
    }

    /**
     * Handle the save action from the UI
     */
    private fun handleSave(builtForm: B) {
        // Optional validation
        val validationError = validateBeforeSave(builtForm)
        if (validationError != null) {
            // Could show a toast or dialog here
            CustomLogger.e("BaseTaskerConfigActivity", "Validation failed: $validationError")
            return
        }

        currentBuiltForm = builtForm
        helper.finishForTasker()
    }

    /**
     * Called by Tasker to restore configuration from saved data
     */
    override fun assignFromInput(input: TaskerInput<I>) {
        val builtForm = convertInputToBuiltForm(input.regular)
        currentBuiltForm = builtForm
        viewModel.restoreForm(builtForm)
        CustomLogger.e("CallServiceViewModel", "assignFromInput: $builtForm")
    }

    /**
     * Called by Tasker to get the configuration to save
     */
    override val inputForTasker: TaskerInput<I>
        get() {
            val builtForm = currentBuiltForm ?: viewModel.buildForm()
            val input = convertBuiltFormToInput(builtForm)
            return TaskerInput(input)
        }
}

