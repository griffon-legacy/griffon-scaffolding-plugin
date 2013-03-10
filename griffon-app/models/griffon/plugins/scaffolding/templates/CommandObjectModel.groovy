package griffon.plugins.scaffolding.templates

class CommandObjectModel {
    @Bindable String title
    @Bindable int width = 0
    @Bindable int height = 0
    @Bindable boolean resizable = true
    @Bindable boolean modal = true
}
