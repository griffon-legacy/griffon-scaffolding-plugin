package griffon.plugins.scaffolding.templates

import java.awt.Window
import griffon.transform.Threading
import griffon.plugins.scaffolding.ScaffoldingContext
import griffon.plugins.scaffolding.CommandObjectUtils

class CommandObjectController {
    def model
    def view
    def builder
    ScaffoldingContext scaffoldingContext

    protected dialog

    @Threading(Threading.Policy.INSIDE_UITHREAD_SYNC)
    void show(Window window) {
        window = window ?: Window.windows.find{it.focused}
        if(!dialog || dialog.owner != window) {
            app.windowManager.hide(dialog)
            model.title = scaffoldingContext.resolveMessage('title',
                              CommandObjectUtils.getNaturalName(scaffoldingContext.validateable))
            dialog = builder.dialog(
                owner: window,
                title: model.title,
                resizable: model.resizable,
                modal: model.modal) {
                container(view.content)
            }
            if(model.width > 0 && model.height > 0) {
                dialog.preferredSize = [model.width, model.height]
            }
            dialog.pack()
        }
        int x = window.x + (window.width - dialog.width) / 2
        int y = window.y + (window.height - dialog.height) / 2
        dialog.setLocation(x, y)
        app.windowManager.show(dialog)
    }

    @Threading(Threading.Policy.INSIDE_UITHREAD_SYNC)
    def cancel = { evt = null ->
        app.windowManager.hide(dialog)
        scaffoldingContext.validateable.validate()
        dialog = null
    }

    @Threading(Threading.Policy.INSIDE_UITHREAD_SYNC)
    def ok = { evt = null ->
        scaffoldingContext.validateable.errors.clearAllErrors()
        scaffoldingContext.validateable.validate()
        if (!scaffoldingContext.validateable.errors.hasErrors()) {
            app.windowManager.hide(dialog)
            dialog = null
        } else {
            for (errorMessage in scaffoldingContext.resolveErrorMessages()) {
                println errorMessage
            }
        }
    }
}
