package griffon.plugins.scaffolding.templates

int sizeThreshold = 250

Map attributesCopy = [:]
attributesCopy.putAll(constrainedProperty.attributes)

if (constrainedProperty.minSize >= sizeThreshold || constrainedProperty.maxSize >= sizeThreshold ||
    constrainedProperty.size?.from >= sizeThreshold || constrainedProperty.size?.to >= sizeThreshold) {

    Map scrollPaneAttributes = [constraints: 'top, grow']
    Map textAreaAttributes = [id: propertyName]
    if (attributesCopy.containsKey('scrollPane')) {
        scrollPaneAttributes.putAll(attributesCopy.remove('scrollPane'))
    }
    if (attributesCopy.containsKey('textArea')) {
        textAreaAttributes.putAll(attributesCopy.remove('textArea'))
    }
    textAreaAttributes.putAll(attributesCopy)
    textAreaAttributes.editable = constrainedProperty.editable

    scrollPane(scrollPaneAttributes) {
        textArea(textAreaAttributes)
        noparent {
            bean(getVariable(propertyName), text: bind('value', mutual: true,
                target: scaffoldingContext.validateable."${propertyName}Property"()))
        }
    }
} else {
    attributesCopy.id = propertyName
    if (!attributesCopy.containsKey('constraints')) attributesCopy.constraints = 'top, grow'
    if (!attributesCopy.containsKey('columns')) attributesCopy.columns = 20
    attributesCopy.editable = constrainedProperty.editable

    String widgetNode = constrainedProperty.password ? 'passwordField' : 'textField'
    "${widgetNode}"(attributesCopy)
    noparent {
        bean(getVariable(propertyName), text: bind('value', mutual: true,
            target: scaffoldingContext.validateable."${propertyName}Property"()))
    }
}
