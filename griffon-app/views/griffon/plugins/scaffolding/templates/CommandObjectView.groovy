package griffon.plugins.scaffolding.templates

import static griffon.util.GriffonNameUtils.getNaturalName

panel(id: 'content') {
    migLayout(layoutConstraints: 'wrap 2', columnConstraints: '[left][left, grow]')
    scaffoldingContext.validateable.constrainedProperties().each { propertyName, constrainedProperty ->
        if (!constrainedProperty.display) return
        label(scaffoldingContext.resolveMessage(propertyName + '.label', getNaturalName(propertyName)+':'),
              constraints: 'top, left')
        Class widgetTemplate = scaffoldingContext.resolveWidget(propertyName)
        setVariable('propertyName', propertyName)
        setVariable('constrainedProperty', constrainedProperty)
        build(widgetTemplate)
    }
    button(cancelAction, constraints: 'skip, split 2, tag cancel')
    button(okAction, constraints: 'tag ok')

    keyStrokeAction(component: current,
        keyStroke: 'ESCAPE',
        condition: 'in focused window',
        action: cancelAction)
}
