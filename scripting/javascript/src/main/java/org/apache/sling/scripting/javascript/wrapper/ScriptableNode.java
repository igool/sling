/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.javascript.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.apache.sling.scripting.javascript.helper.SlingWrapper;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for JCR nodes that exposes all properties and child nodes as
 * properties of a Javascript object.
 */
public class ScriptableNode extends ScriptableObject implements SlingWrapper {

    public static final String CLASSNAME = "Node";
    public static final Class<?> [] WRAPPED_CLASSES = { Node.class };

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Node node;

    public ScriptableNode() {
    }

    public ScriptableNode(Node item) {
        super();
        this.node = item;
    }

    public void jsConstructor(Object res) {
        this.node = (Node) res;
    }

    public String getClassName() {
        return CLASSNAME;
    }

    public Class<?> [] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }

    public Object jsFunction_addNode(String path, String primaryType) throws RepositoryException {
        Node n = null;
        if(primaryType == null || "undefined".equals(primaryType)) {
            n = node.addNode(path);
        } else {
            n = node.addNode(path, primaryType);
        }

        final Object result = ScriptRuntime.toObject(this, n);
        return result;
    }

    public Object jsFunction_getNode(String path) throws RepositoryException {
        return ScriptRuntime.toObject(this, node.getNode(path));
    }

    public ScriptableItemMap jsFunction_getChildren() {
        try {
            return new ScriptableItemMap(node.getNodes());
        } catch (RepositoryException re) {
            log.warn("Cannot get children of " + jsFunction_getPath(), re);
            return new ScriptableItemMap();
        }
    }

    public ScriptableItemMap jsFunction_getProperties() {
        try {
            return new ScriptableItemMap(node.getProperties());
        } catch (RepositoryException re) {
            log.warn("Cannot get children of " + jsFunction_getPath(), re);
            return new ScriptableItemMap();
        }
    }

    public Object jsFunction_getPrimaryItem() {
        try {
            return ScriptRuntime.toObject(this, node.getPrimaryItem());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public String jsFunction_getUUID() {
        try {
            return node.getUUID();
        } catch (RepositoryException re) {
            return "";
        }
    }

    public int jsFunction_getIndex() {
        try {
            return node.getIndex();
        } catch (RepositoryException re) {
            return 1;
        }
    }

    public Iterator<?> jsFunction_getReferences() {
        try {
            return node.getReferences();
        } catch (RepositoryException re) {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    public Object jsFunction_getPrimaryNodeType() {
        try {
            return node.getPrimaryNodeType();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public NodeType[] jsFunction_getMixinNodeTypes() {
        try {
            return node.getMixinNodeTypes();
        } catch (RepositoryException re) {
            return new NodeType[0];
        }
    }

    public Object jsFunction_getDefinition() {
        try {
            return node.getDefinition();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public boolean jsFunction_getCheckedOut() {
        try {
            return node.isCheckedOut();
        } catch (RepositoryException re) {
            return false;
        }
    }

    public Object jsFunction_getVersionHistory() {
        try {
            return node.getVersionHistory();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsFunction_getBaseVersion() {
        try {
            return node.getBaseVersion();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public Object jsFunction_getLock() {
        try {
            return node.getLock();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public boolean jsFunction_getLocked() {
        try {
            return node.isLocked();
        } catch (RepositoryException re) {
            return false;
        }
    }

    public Object jsFunction_getSession() {
        try {
            return node.getSession();
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public String jsFunction_getPath() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            return node.toString();
        }
    }

    public String jsFunction_getName() {
        try {
            return node.getName();
        } catch (RepositoryException e) {
            return node.toString();
        }
    }

    public Object jsFunction_getParent() {
        try {
            return ScriptRuntime.toObject(this, node.getParent());
        } catch (RepositoryException re) {
            return Undefined.instance;
        }
    }

    public int jsFunction_getDepth() {
        try {
            return node.getDepth();
        } catch (RepositoryException re) {
            return -1;
        }
    }

    public boolean jsFunction_getNew() {
        return node.isNew();
    }

    public boolean jsFunction_getModified() {
        return node.isModified();
    }

    /**
     * Gets the value of a (Javascript) property or child node. If there is a single single-value
     * JCR property of this node, return its string value. If there are multiple properties
     * of the same name or child nodes of the same name, return an array.
     */
    @Override
    public Object get(String name, Scriptable start) {

        // builtin javascript properties (jsFunction_ etc.) have priority
        final Object fromSuperclass = super.get(name, start);
        if(fromSuperclass != Scriptable.NOT_FOUND) {
            return fromSuperclass;
        }

        if(node == null) {
            return Undefined.instance;
        }

        final List<Scriptable> items = new ArrayList<Scriptable>();

        // Add all matching nodes to result
        try {
            NodeIterator it = node.getNodes(name);
            while (it.hasNext()) {
                items.add(new ScriptableNode(it.nextNode()));
            }
        } catch (RepositoryException e) {
            log.debug("RepositoryException while collecting Node children",e);
        }

        // Add all matching properties to result
        try {
            PropertyIterator it = node.getProperties(name);
            while (it.hasNext()) {
                Property prop = it.nextProperty();
                int type = prop.getType();
                if (prop.getDefinition().isMultiple()) {
                    Value[] values = prop.getValues();
                    for (int i=0;i<values.length;i++) {
                        items.add(wrap(values[i], type));
                    }
                } else {
                    if (type==PropertyType.REFERENCE) {
                        items.add(new ScriptableNode(prop.getNode()));
                    } else {
                        items.add(wrap(prop.getValue(), type));
                    }
                }
            }
        } catch (RepositoryException e) {
            log.debug("RepositoryException while collecting Node properties",e);
        }

        if (items.size()==0) {
            return Scriptable.NOT_FOUND;
        } else if (items.size()==1) {
            return items.iterator().next();
        } else {
            //TODO: add write support
            NativeArray result = new NativeArray(items.toArray());
            ScriptRuntime.setObjectProtoAndParent(result, this);
            return result;
        }
    }

    /** Wrap JCR Values in a simple way */
    private Scriptable wrap(Value value, int type) throws ValueFormatException, IllegalStateException, RepositoryException {
        Object valObj;
        if (type==PropertyType.BINARY) {
            valObj = value.getBoolean();
        } else if (type==PropertyType.DOUBLE) {
            valObj = value.getDouble();
        } else if (type==PropertyType.LONG) {
            valObj = value.getLong();
        } else {
            valObj = value.getString();
        }

        return ScriptRuntime.toObject(this, valObj);
    }

    @Override
    public Object[] getIds() {
        Collection<String> ids = new ArrayList<String>();
        try {
            PropertyIterator pit = node.getProperties();
            while (pit.hasNext()) {
                ids.add(pit.nextProperty().getName());
            }
        } catch (RepositoryException e) {
            //do nothing, just do not list properties
        }
        try {
            NodeIterator nit = node.getNodes();
            while (nit.hasNext()) {
                ids.add(nit.nextNode().getName());
            }
        } catch (RepositoryException e) {
            //do nothing, just do not list child nodes
        }
        return ids.toArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getDefaultValue(Class typeHint) {
        try {
            return node.getPath();
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public boolean has(String name, Scriptable start) {
        try {
            // TODO should this take into account our jsFunction_ members?
            return node.hasProperty(name) || node.hasNode(name);
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        try {
            return node.getPath();
        } catch (RepositoryException e) {
            return node.toString();
        }
    }

    // ---------- Wrapper interface --------------------------------------------

    // returns the wrapped node
    public Object unwrap() {
        return node;
    }
}
