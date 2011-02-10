/*
 * Copyright 2003-2010 the original author or authors.
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
package org.codehaus.groovy.eclipse.dsl.script;

/**
 * The result of a ContextQuery.  Could be empty, or
 * an ASTNode, or a set of ASTNodes
 * @author andrew
 * @created Nov 22, 2010
 */
public interface IContextQueryResult<T> {

    public static enum ResultKind { EMPTY, SINGLE_NODE, MULTIPLE_NODES, OTHER }
    
    ResultKind getResultKind();
    
    T getResult();
}