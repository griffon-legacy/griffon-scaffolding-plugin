/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.griffon.ast;

import griffon.plugins.scaffolding.CommandObject;
import griffon.util.Metadata;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static griffon.util.GriffonNameUtils.capitalize;
import static org.codehaus.griffon.ast.GriffonASTUtils.injectInterface;

/**
 * Handles generation of code for the {@code @CommandObject} annotation.
 * <p/>
 *
 * @author Andres Almiray
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class CommandObjectASTTransformation extends ValidateableASTTransformation {
    private static final Logger LOG = LoggerFactory.getLogger(CommandObjectASTTransformation.class);
    protected static final ClassNode COMMAND_OBJECT_TYPE = makeClassSafe(CommandObject.class);
    protected static final ClassNode COMMAND_OBJECT_ANNOTATION = makeClassSafe(griffon.transform.CommandObject.class);

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;

        ClassLoader cl = CommandObjectASTTransformation.class.getClassLoader();
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            cnfe = e;
        }

        cl = Thread.currentThread().getContextClassLoader();
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            cnfe = e;
        }

        if (cnfe != null) throw cnfe;
        return null;
    }

    /**
     * Convenience method to see if an annotated node is {@code @CommandObject}.
     *
     * @param node the node to check
     * @return true if the node is annotated with @CommandObject
     */
    public static boolean hasCommandObjectAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (COMMAND_OBJECT_ANNOTATION.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes  the ast nodes
     * @param source the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        checkNodesForAnnotationAndType(nodes[0], nodes[1]);

        ClassNode classNode = (ClassNode) nodes[1];
        String toolkitName = capitalize(Metadata.getCurrent().getApplicationToolkit());
        String astTransformationClassName = getClass().getPackage().getName() + "." + toolkitName + getClass().getSimpleName();

        if (needsValidateable(classNode, source)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Injecting " + CommandObject.class.getName() + " into " + classNode.getName());
            }
            injectInterface(classNode, COMMAND_OBJECT_TYPE);
            try {
                Class astTransformationClass = loadClass(astTransformationClassName);
                ASTTransformation astTransformation = (ASTTransformation) astTransformationClass.newInstance();
                astTransformation.visit(nodes, source);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot load " + astTransformationClassName, e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Cannot load " + astTransformationClassName, e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot load " + astTransformationClassName, e);
            }
            addValidatableBehavior(classNode);
        }
    }
}
