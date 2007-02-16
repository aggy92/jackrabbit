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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.name.Path;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>Restore</code>...
 */
public class Restore extends AbstractOperation {

    private final NodeState nodeState;
    private final Path relQPath;
    private final NodeState[] versionStates;
    private final boolean removeExisting;

    private Restore(NodeState nodeState, Path relQPath, NodeState[] versionStates, boolean removeExisting) {
        this.nodeState = nodeState;
        this.relQPath = relQPath;
        this.versionStates = versionStates;
        this.removeExisting = removeExisting;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        visitor.visit(this);
    }

    /**
     * In case of a workspace-restore or 'removeExisting' the complete tree gets
     * invalidated, otherwise the given <code>NodeState</code> that has been
     * updated and all its decendants.
     *
     * @see Operation#persisted(CacheBehaviour)
     * @param cacheBehaviour
     */
    public void persisted(CacheBehaviour cacheBehaviour) {
        if (cacheBehaviour == CacheBehaviour.INVALIDATE) {
            NodeEntry entry;
            if (nodeState == null || removeExisting) {
                // invalidate the complete tree
                // -> start searching root-entry from any version-entry or
                //    from the given nodestate
                entry = (nodeState == null) ? versionStates[0].getNodeEntry() : nodeState.getNodeEntry();
                while (entry.getParent() != null) {
                    entry = entry.getParent();
                }
            } else {
                entry = nodeState.getNodeEntry();
            }
            entry.invalidate(true);
        }
    }
    //----------------------------------------< Access Operation Parameters >---

    /**
     * Returns state or the closest existing state of the restore target or
     * <code>null</code> in case of a {@link javax.jcr.Workspace#restore(Version[], boolean)}
     *
     * @return
     */
    public NodeState getNodeState() {
        return nodeState;
    }

    /**
     * Relative qualified path to the non-existing restore target or <code>null</code>
     * if the state returned by {@link #getNodeState()} is the target.
     *
     * @return
     * @see javax.jcr.Node#restore(Version, String, boolean) 
     */
    public Path getRelativePath() {
        return relQPath;
    }

    public NodeState[] getVersionStates() {
        return versionStates;
    }

    public boolean removeExisting() {
        return removeExisting;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param versionState
     * @return
     */
    public static Operation create(NodeState nodeState, Path relQPath, NodeState versionState, boolean removeExisting) {
        if (nodeState == null || versionState == null) {
            throw new IllegalArgumentException("Neither nodeId nor versionState must be null.");
        }
        Restore up = new Restore(nodeState, relQPath, new NodeState[] {versionState}, removeExisting);
        return up;
    }

    /**
     *
     * @param versionStates
     * @return
     */
    public static Operation create(NodeState[] versionStates, boolean removeExisting) {
        if (versionStates == null) {
            throw new IllegalArgumentException("Version states must not be null.");
        }
        Restore up = new Restore(null, null, versionStates, removeExisting);
        return up;
    }
}