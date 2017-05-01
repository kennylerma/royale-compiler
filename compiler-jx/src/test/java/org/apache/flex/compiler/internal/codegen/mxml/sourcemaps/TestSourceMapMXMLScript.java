/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.flex.compiler.internal.codegen.mxml.sourcemaps;

import org.apache.flex.compiler.definitions.IClassDefinition;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.internal.codegen.js.flexjs.JSFlexJSEmitter;
import org.apache.flex.compiler.internal.test.FlexJSSourceMapTestBase;
import org.apache.flex.compiler.internal.test.TestBase;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.tree.as.IFunctionNode;
import org.apache.flex.compiler.tree.as.IVariableNode;
import org.apache.flex.compiler.tree.mxml.IMXMLDocumentNode;
import org.apache.flex.compiler.tree.mxml.IMXMLScriptNode;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TestSourceMapMXMLScript extends FlexJSSourceMapTestBase
{
    @Test
    public void testField()
    {
        String code = "var foo;";

        IVariableNode node = (IVariableNode) getASNode(code, IVariableNode.class);
        IMXMLDocumentNode dnode = (IMXMLDocumentNode) node
                .getAncestorOfType(IMXMLDocumentNode.class);
        IClassDefinition definition = dnode.getClassDefinition();
        ((JSFlexJSEmitter)(mxmlBlockWalker.getASEmitter())).getModel().setCurrentClass(definition);
        mxmlBlockWalker.visitDocument(dnode);
        String definitionName = definition.getQualifiedName();
        assertTrue(definitionName.startsWith(getClass().getSimpleName()));
        int endColumn = definitionName.length() + 14;
        ///**\n * @export\n * @type {*}\n */\nFalconTest_A.prototype.foo
        assertMapping(node, 0, 4, 42, 0, 42, endColumn);  // foo
    }

    @Test
    public void testMethod()
    {
        String code = "function foo(){};";

        IFunctionNode node = (IFunctionNode) getASNode(code, IFunctionNode.class);
        IMXMLDocumentNode dnode = (IMXMLDocumentNode) node
                .getAncestorOfType(IMXMLDocumentNode.class);
        IClassDefinition definition = dnode.getClassDefinition();
        ((JSFlexJSEmitter)(mxmlBlockWalker.getASEmitter())).getModel().setCurrentClass(definition);
        mxmlBlockWalker.visitDocument(dnode);
        String definitionName = definition.getQualifiedName();
        assertTrue(definitionName.startsWith(getClass().getSimpleName()));
        int nameEndColumn = definitionName.length() + 14;
        ///**\n * @export\n * @type {*}\n */\nFalconTest_A.prototype.foo
        assertMapping(node, 0, 9, 38, 0, 38, nameEndColumn);  // foo
        assertMapping(node, 0, 0, 38, nameEndColumn, 38, nameEndColumn + 11);  // = function
        assertMapping(node, 0, 12, 38, nameEndColumn + 11, 38, nameEndColumn + 12);  // (
        assertMapping(node, 0, 13, 38, nameEndColumn + 12, 38, nameEndColumn + 13);  // )
        assertMapping(node, 0, 14, 38, nameEndColumn + 14, 38, nameEndColumn + 15);  // {
        assertMapping(node, 0, 15, 39, 0, 39, 1);  // }
    }
}