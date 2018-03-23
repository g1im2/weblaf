/*
 * This file is part of WebLookAndFeel library.
 *
 * WebLookAndFeel library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebLookAndFeel library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebLookAndFeel library.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alee.laf.tree;

import com.alee.utils.collection.EmptyEnumeration;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * {@link WebTreeNode} is a WebLaF approach for {@link MutableTreeNode} implementation.
 * Unlike {@link javax.swing.tree.DefaultMutableTreeNode} it has generics for node type and stored object type.
 *
 * @param <N> tree node type
 * @param <T> stored object type
 * @author Rob Davis
 * @author Mikle Garin
 */

public class WebTreeNode<N extends WebTreeNode<N, T>, T> implements MutableTreeNode, Cloneable, Serializable
{
    /**
     * Parent node or {@code null} if this node has no parent.
     */
    protected N parent;

    /**
     * {@link List} of child nodes, may be {@code null} if this node has no children.
     */
    protected List<N> children;

    /**
     * Whether or not this node is able to have children.
     */
    protected boolean allowsChildren;

    /**
     * Optional node {@link Object}.
     */
    protected transient T userObject;

    /**
     * Constructs new {@link WebTreeNode} that has no parent and no children, but which allows children.
     */
    public WebTreeNode ()
    {
        this ( null );
    }

    /**
     * Constructs new {@link WebTreeNode} with no parent, no children, but which allows children and has a {@link #userObject}.
     *
     * @param userObject optional node {@link Object}
     */
    public WebTreeNode ( final T userObject )
    {
        this ( userObject, true );
    }

    /**
     * Constructs new {@link WebTreeNode} with no parent, no children, has a {@link #userObject} and that allows children only if specified.
     *
     * @param userObject     optional node {@link Object}
     * @param allowsChildren whether or not this node is able to have children
     */
    public WebTreeNode ( final T userObject, final boolean allowsChildren )
    {
        super ();
        parent = null;
        this.allowsChildren = allowsChildren;
        this.userObject = userObject;
    }

    /**
     * Removes {@code child} from its present parent (if it has a parent), sets the child's parent to this node,
     * and then adds the child to this node's child {@link List} at index {@code childIndex}.
     * {@code child} must not be {@code null} and must not be an ancestor of this node.
     *
     * @param child child node to insert under this node
     * @param index index in this node's child {@link List} where this node is to be inserted
     * @throws IllegalStateException          if this node does not allow children
     * @throws IllegalArgumentException       if {@code child} is null or is an ancestor of this node
     * @throws ArrayIndexOutOfBoundsException if {@code childIndex} is out of bounds
     * @see #isNodeDescendant(WebTreeNode)
     */
    @Override
    public void insert ( final MutableTreeNode child, final int index )
    {
        if ( !allowsChildren )
        {
            throw new IllegalStateException ( "Node does not allow children" );
        }
        else if ( child == null )
        {
            throw new IllegalArgumentException ( "New child is null" );
        }
        else if ( isNodeAncestor ( ( N ) child ) )
        {
            throw new IllegalArgumentException ( "New child is an ancestor" );
        }

        final N oldParent = ( N ) child.getParent ();
        if ( oldParent != null )
        {
            oldParent.remove ( child );
        }
        child.setParent ( this );
        if ( children == null )
        {
            children = new ArrayList<N> ();
        }
        children.add ( index, ( N ) child );
    }

    /**
     * Removes the child at the specified index from this node's children and sets that node's parent to {@code null}.
     * The child node to remove must be a {@link WebTreeNode}.
     *
     * @param index the index in this node's child {@link List} of the child to remove
     * @throws ArrayIndexOutOfBoundsException if {@code childIndex} is out of bounds
     */
    @Override
    public void remove ( final int index )
    {
        final N child = getChildAt ( index );
        children.remove ( index );
        child.setParent ( null );
    }

    /**
     * Sets this node's parent to {@code newParent} but does not change the parent's child {@link List}.
     * This method is called from {@code insert()} and {@code remove()} to reassign a child's parent,
     * it should not be messaged from anywhere else.
     *
     * @param parent this node's new parent
     */
    @Override
    public void setParent ( final MutableTreeNode parent )
    {
        this.parent = ( N ) parent;
    }

    /**
     * Returns this node's parent or {@code null} if this node has no parent.
     *
     * @return this node's parent node, or {@code null} if this node has no parent
     */
    @Override
    public N getParent ()
    {
        return parent;
    }

    /**
     * Returns the child node at the specified index in this node's child {@link List}.
     *
     * @param index index in this node's child {@link List}
     * @return the child node at the specified index in this node's child {@link List}
     * @throws ArrayIndexOutOfBoundsException if {@code index} is out of bounds
     */
    @Override
    public N getChildAt ( final int index )
    {
        if ( children == null )
        {
            throw new ArrayIndexOutOfBoundsException ( "Node has no children" );
        }
        return children.get ( index );
    }

    /**
     * Returns number of children in this node.
     *
     * @return number of children in this node
     */
    @Override
    public int getChildCount ()
    {
        return children != null ? children.size () : 0;
    }

    /**
     * Returns index of the specified node in this node's child {@link List}.
     * If the specified node is not a child of this node, returns {@code -1}.
     * This method performs a linear search and is O(n) where n is the number of children.
     *
     * @param child node to search for among this node's children
     * @return index of the specified node in this node's child {@link List}, or {@code -1} if it is a not a child of this node
     * @throws IllegalArgumentException if {@code child} is {@code null}
     */
    @Override
    public int getIndex ( final TreeNode child )
    {
        if ( child == null )
        {
            throw new IllegalArgumentException ( "Node is null" );
        }
        return isNodeChild ( ( N ) child ) ? children.indexOf ( child ) : -1;
    }

    /**
     * Returns a forward-order {@link Enumeration} of this node's children.
     * Modifying this node's child {@link List} invalidates any child enumerations created before the modification.
     *
     * @return a forward-order {@link Enumeration} of this node's children
     */
    @Override
    public Enumeration<N> children ()
    {
        return children != null ? Collections.enumeration ( children ) : EmptyEnumeration.<N>instance ();
    }

    /**
     * Determines whether or not this node is allowed to have children.
     * If {@code allows} is {@code false}, all of this node's children are removed.
     *
     * Note: By default, a node allows children.
     *
     * @param allows whether or not this node is allowed to have children
     */
    public void setAllowsChildren ( final boolean allows )
    {
        if ( allows != allowsChildren )
        {
            allowsChildren = allows;
            if ( !allowsChildren )
            {
                removeAllChildren ();
            }
        }
    }

    /**
     * Returns whether or not this node is allowed to have children.
     *
     * @return {@code true} if this node allows children, {@code false} otherwise
     */
    @Override
    public boolean getAllowsChildren ()
    {
        return allowsChildren;
    }

    /**
     * Sets the user object for this node to {@code userObject}.
     *
     * @param userObject optional node {@link Object}
     * @see #getUserObject()
     * @see #toString()
     */
    @Override
    public void setUserObject ( final Object userObject )
    {
        this.userObject = ( T ) userObject;
    }

    /**
     * Returns this node's user object.
     *
     * @return the Object stored at this node by the user
     * @see #setUserObject(Object)
     * @see #toString()
     */
    public T getUserObject ()
    {
        return userObject;
    }

    /**
     * Removes the subtree rooted at this node from the tree, giving this node a {@code null} parent.
     * Does nothing if this node is the root of its tree.
     */
    @Override
    public void removeFromParent ()
    {
        final N parent = getParent ();
        if ( parent != null )
        {
            parent.remove ( this );
        }
    }

    /**
     * Removes {@code child} from this node's child {@link List}, giving it a {@code null} parent.
     *
     * @param child a child of this node to remove
     * @throws IllegalArgumentException if {@code child} is {@code null} or is not a child of this node
     */
    @Override
    public void remove ( final MutableTreeNode child )
    {
        if ( child == null )
        {
            throw new IllegalArgumentException ( "Node is null" );
        }
        if ( !isNodeChild ( ( N ) child ) )
        {
            throw new IllegalArgumentException ( "Node is not a child" );
        }
        remove ( getIndex ( child ) );
    }

    /**
     * Removes all of this node's children, setting their parents to {@code null}.
     * If this node has no children, this method does nothing.
     */
    public void removeAllChildren ()
    {
        for ( int i = getChildCount () - 1; i >= 0; i-- )
        {
            remove ( i );
        }
    }

    /**
     * Removes {@code child} from its parent and makes it a child of this node by adding it to the end of this node's child {@link List}.
     *
     * @param child node to add as a child of this node
     * @throws IllegalStateException    if this node does not allow children
     * @throws IllegalArgumentException if {@code child} is {@code null}
     * @see #insert(MutableTreeNode, int)
     */
    public void add ( final MutableTreeNode child )
    {
        if ( child != null && child.getParent () == this )
        {
            insert ( child, getChildCount () - 1 );
        }
        else
        {
            insert ( child, getChildCount () );
        }
    }

    /**
     * Returns whether or not {@code anotherNode} is an ancestor of this node.
     * If {@code anotherNode} is {@code null}, this method returns {@code false}.
     * Note that any node is considered as an ancestor of itself.
     * This operation is at worst O(h) where h is the distance from the root to this node.
     *
     * @param anotherNode node to test as an ancestor of this node
     * @return {@code true} if {@code anotherNode} is an ancestor of this node, {@code false} otherwise
     * @see #isNodeDescendant(WebTreeNode)
     * @see #getSharedAncestor(WebTreeNode)
     */
    public boolean isNodeAncestor ( final N anotherNode )
    {
        if ( anotherNode != null )
        {
            N ancestor = ( N ) this;
            do
            {
                if ( ancestor == anotherNode )
                {
                    return true;
                }
            }
            while ( ( ancestor = ancestor.getParent () ) != null );
        }
        return false;
    }

    /**
     * Returns whether or not {@code anotherNode} is a descendant of this node.
     * If {@code anotherNode} is {@code null}, this method returns {@code false}.
     * Note that any node is considered as a descendant of itself.
     * This operation is at worst O(h) where h is the distance from the root to {@code anotherNode}.
     *
     * @param anotherNode node to test as descendant of this node
     * @return {@code true} if {@code anotherNode} is a descendant of this node, {@code false} otherwise
     * @see #isNodeAncestor(WebTreeNode)
     * @see #getSharedAncestor(WebTreeNode)
     */
    public boolean isNodeDescendant ( final N anotherNode )
    {
        return anotherNode != null && anotherNode.isNodeAncestor ( ( N ) this );
    }

    /**
     * Returns the nearest common ancestor to this node and {@code anotherNode}.
     * Returns {@code null}, if this node and {@code anotherNode} are in different trees or if {@code anotherNode} is {@code null}.
     * Note that any node is considered as an ancestor of itself.
     *
     * @param anotherNode node to find common ancestor with
     * @return nearest ancestor common to this node and {@code anotherNode}, or {@code null} if none
     * @see #isNodeAncestor(WebTreeNode)
     * @see #isNodeDescendant(WebTreeNode)
     */
    public N getSharedAncestor ( final N anotherNode )
    {
        if ( anotherNode == this )
        {
            return ( N ) this;
        }
        else if ( anotherNode == null )
        {
            return null;
        }

        final int level1;
        final int level2;
        int diff;
        N node1;
        N node2;

        level1 = getLevel ();
        level2 = anotherNode.getLevel ();

        if ( level2 > level1 )
        {
            diff = level2 - level1;
            node1 = anotherNode;
            node2 = ( N ) this;
        }
        else
        {
            diff = level1 - level2;
            node1 = ( N ) this;
            node2 = anotherNode;
        }

        // Go up the tree until the nodes are at the same level
        while ( diff > 0 )
        {
            node1 = node1.getParent ();
            diff--;
        }

        /**
         * Move up the tree until we find a common ancestor.
         * Since we know that both nodes are at the same level, we won't cross paths unknowingly.
         * (if there is a common ancestor, both nodes hit it in the same iteration)
         */
        do
        {
            if ( node1 == node2 )
            {
                return node1;
            }
            node1 = node1.getParent ();
            node2 = node2.getParent ();
        }
        while ( node1 != null );

        /**
         * Only need to check one.
         * They're at the same level so if one is {@code null} - the other is.
         */
        if ( node1 != null || node2 != null )
        {
            throw new Error ( "Nodes shouldn't be null" );
        }

        return null;
    }

    /**
     * Returns {@code true} if and only if {@code anotherNode} is in the same tree as this node.
     * Returns {@code false} if {@code anotherNode} is {@code null}.
     *
     * @param anotherNode node to find relation with
     * @return {@code true} if {@code anotherNode} is in the same tree as this node, {@code false} if {@code anotherNode} is {@code null}
     * @see #getSharedAncestor(WebTreeNode)
     * @see #getRoot()
     */
    public boolean isNodeRelated ( final N anotherNode )
    {
        return anotherNode != null && getRoot () == anotherNode.getRoot ();
    }

    /**
     * Returns the longest distance from this node to a leaf, if this node has no children returns {@code 0}.
     * This operation is much more expensive than {@link #getLevel()} because it must traverse the entire tree under this node.
     *
     * @return the depth of the tree whose root is this node
     * @see #getLevel()
     */
    public int getDepth ()
    {
        N last = null;
        final Enumeration<N> breadthFirst = breadthFirstEnumeration ();
        while ( breadthFirst.hasMoreElements () )
        {
            last = breadthFirst.nextElement ();
        }
        if ( last == null )
        {
            throw new Error ( "Nodes shouldn't be null" );
        }
        return last.getLevel () - getLevel ();
    }

    /**
     * Returns the distance from the root to this node, if this node is the root returns {@code 0}.
     *
     * @return the number of levels above this node
     * @see #getDepth()
     */
    public int getLevel ()
    {
        int levels = 0;
        N ancestor = ( N ) this;
        while ( ( ancestor = ancestor.getParent () ) != null )
        {
            levels++;
        }
        return levels;
    }

    /**
     * Returns {@link TreePath} from the root to this node.
     * The last element in the path is this node.
     *
     * @return {@link TreePath} from the root to this node
     */
    public TreePath getTreePath ()
    {
        return TreeUtils.getTreePath ( this );
    }

    /**
     * Returns the path from the root to this node.
     * The last element in the path is this node.
     *
     * @return an array of {@link TreeNode} objects giving the path, where the first element is root and the last element is this node
     */
    public TreeNode[] getPath ()
    {
        return TreeUtils.getPath ( this );
    }

    /**
     * Returns the user object path from the root to this node.
     * If some of the nodes in the path have {@code null} user objects, the returned path will contain {@code null}s.
     * Although it is not allowed for this node to have a {@code null} user object.
     *
     * @return the user object path from the root to this node
     * @throws IllegalStateException if this node to have a {@code null} user object
     */
    public T[] getUserObjectPath ()
    {
        final T userObject = getUserObject ();
        final Class type;
        if ( userObject != null )
        {
            type = userObject.getClass ();
        }
        else
        {
            throw new IllegalStateException ( "UserObject shouldn't be null" );
        }

        final TreeNode[] realPath = getPath ();
        final T[] path = ( T[] ) Array.newInstance ( type, realPath.length );
        for ( int counter = 0; counter < realPath.length; counter++ )
        {
            path[ counter ] = ( ( N ) realPath[ counter ] ).getUserObject ();
        }
        return path;
    }

    /**
     * Returns the root of the tree that contains this node.  The root is
     * the ancestor with a {@code null} parent.
     *
     * @return the root of the tree that contains this node
     * @see #isNodeAncestor(WebTreeNode)
     */
    public N getRoot ()
    {
        final N parent = getParent ();
        return parent != null ? parent.getRoot () : ( N ) this;
    }

    /**
     * Returns whether or not this node is the root of the tree.
     * The root is the only node in the tree with a {@code null} parent, every tree can only have one root.
     *
     * @return {@code true} if this node is the root of its tree, {@code false} otherwise
     */
    public boolean isRoot ()
    {
        return getParent () == null;
    }

    /**
     * Returns the node that follows this node in a preorder traversal of this node's tree.
     * Returns {@code null} if this node is the last node of the traversal.
     * This is not an efficient way to traverse the entire tree, use an {@link Enumeration} instead.
     *
     * @return the node that follows this node in a preorder traversal, or {@code null} if this node is last
     * @see #preorderEnumeration()
     */
    public N getNextNode ()
    {
        if ( getChildCount () == 0 )
        {
            // No children, so look for nextSibling
            N nextSibling = getNextSibling ();
            if ( nextSibling == null )
            {
                N aNode = getParent ();
                do
                {
                    if ( aNode == null )
                    {
                        return null;
                    }

                    nextSibling = aNode.getNextSibling ();
                    if ( nextSibling != null )
                    {
                        return nextSibling;
                    }

                    aNode = aNode.getParent ();
                }
                while ( true );
            }
            else
            {
                return nextSibling;
            }
        }
        else
        {
            return getChildAt ( 0 );
        }
    }

    /**
     * Returns the node that precedes this node in a preorder traversal of this node's tree.
     * Returns {@code null} if this node is the root of the tree.
     * This is an inefficient way to traverse the entire tree, use an {@link Enumeration} instead.
     *
     * @return the node that precedes this node in a preorder traversal, or {@code null} if this node is the first
     * @see #preorderEnumeration()
     */
    public N getPreviousNode ()
    {
        final N previousSibling;
        final N myParent = getParent ();
        if ( myParent == null )
        {
            return null;
        }
        previousSibling = getPreviousSibling ();
        if ( previousSibling != null )
        {
            if ( previousSibling.getChildCount () == 0 )
            {
                return previousSibling;
            }
            else
            {
                return previousSibling.getLastLeaf ();
            }
        }
        else
        {
            return myParent;
        }
    }

    /**
     * Creates and returns an {@link Enumeration} that traverses the subtree rooted at this node in preorder.
     * The first node returned by the {@link Enumeration#nextElement()} method is this node.
     * Modifying the tree by inserting, removing, or moving a node invalidates any {@link Enumeration}s created before the modification.
     *
     * @return an {@link Enumeration} that traverses the subtree rooted at this node in preorder
     * @see #postorderEnumeration()
     */
    public Enumeration<N> preorderEnumeration ()
    {
        return new PreorderEnumeration<N, T> ( ( N ) this );
    }

    /**
     * Creates and returns an {@link Enumeration} that traverses the subtree rooted at this node in postorder.
     * The first node returned by the {@link Enumeration#nextElement()} method is the leftmost leaf.
     * This is the same as a depth-first traversal.
     * Modifying the tree by inserting, removing, or moving a node invalidates any {@link Enumeration}s created before the modification.
     *
     * @return an {@link Enumeration} that traverses the subtree rooted at this node in postorder
     * @see #depthFirstEnumeration()
     * @see #preorderEnumeration()
     */
    public Enumeration<N> postorderEnumeration ()
    {
        return new PostorderEnumeration<N, T> ( ( N ) this );
    }

    /**
     * Creates and returns an {@link Enumeration} that traverses the subtree rooted at this node in breadth-first order.
     * The first node returned by the {@link Enumeration#nextElement()} method is this node.
     * Modifying the tree by inserting, removing, or moving a node invalidates any {@link Enumeration}s created before the modification.
     *
     * @return an {@link Enumeration} that traverses the subtree rooted at this node in breadth-first order
     * @see #depthFirstEnumeration()
     */
    public Enumeration<N> breadthFirstEnumeration ()
    {
        return new BreadthFirstEnumeration<N, T> ( ( N ) this );
    }

    /**
     * Creates and returns an {@link Enumeration} that traverses the subtree rooted at this node in depth-first order.
     * The first node returned by the {@link Enumeration#nextElement()} method is the leftmost leaf.
     * This is the same as a postorder traversal.
     * Modifying the tree by inserting, removing, or moving a node invalidates any {@link Enumeration}s created before the modification.
     *
     * @return an {@link Enumeration} that traverses the subtree rooted at this node in depth-first order
     * @see #breadthFirstEnumeration()
     * @see #postorderEnumeration()
     */
    public Enumeration<N> depthFirstEnumeration ()
    {
        return postorderEnumeration ();
    }

    /**
     * Creates and returns an {@link Enumeration} that follows the path from {@code ancestor} to this node.
     * The {@link Enumeration#nextElement()} method first returns {@code ancestor},
     * then the child of {@code ancestor} that is an ancestor of this node, and so on, and finally returns this node.
     * Creation of the {@link Enumeration} is O(m) where m is the number of nodes between this node and {@code ancestor}, inclusive.
     * Each {@code nextElement()} message is O(1).
     * Modifying the tree by inserting, removing, or moving a node invalidates any {@link Enumeration}s created before the modification.
     *
     * @param ancestor ancestor node to start path from
     * @return an {@link Enumeration} for following the path from an ancestor of this node to this one
     * @throws IllegalArgumentException if {@code ancestor} is not an ancestor of this node
     * @see #isNodeAncestor(WebTreeNode)
     * @see #isNodeDescendant(WebTreeNode)
     */
    public Enumeration pathFromAncestorEnumeration ( final N ancestor )
    {
        return new PathBetweenNodesEnumeration<N, T> ( ancestor, ( N ) this );
    }

    /**
     * Returns whether or not {@code anotherNode} is a child of this node.
     * If {@code anotherNode} is {@code null}, this method returns false.
     *
     * @param anotherNode node to confirm if it is child of this node or not
     * @return {@code true} if {@code anotherNode} is a child of this node, {@code false} if {@code anotherNode} is {@code null}
     */
    public boolean isNodeChild ( final N anotherNode )
    {
        return anotherNode != null && getChildCount () != 0 && anotherNode.getParent () == this;
    }

    /**
     * Returns this node's first child.
     * If this node has no children, throws {@link NoSuchElementException}.
     *
     * @return the first child of this node
     * @throws NoSuchElementException if this node has no children
     */
    public N getFirstChild ()
    {
        if ( getChildCount () == 0 )
        {
            throw new NoSuchElementException ( "node has no children" );
        }
        return getChildAt ( 0 );
    }

    /**
     * Returns this node's last child.
     * If this node has no children, throws {@link NoSuchElementException}.
     *
     * @return the last child of this node
     * @throws NoSuchElementException if this node has no children
     */
    public N getLastChild ()
    {
        if ( getChildCount () == 0 )
        {
            throw new NoSuchElementException ( "node has no children" );
        }
        return getChildAt ( getChildCount () - 1 );
    }

    /**
     * Returns the child in this node's child {@link List} that immediately follows {@code child}, which must be a child of this node.
     * If {@code child} is the last child, returns {@code null}.
     * This method performs a linear search of this node's children for {@code child} and is O(n) where n is the number of children.
     * To traverse the entire {@link List} of children use an {@link Enumeration} instead.
     *
     * @param child child to find another child after
     * @return the child of this node that immediately follows {@code child}
     * @throws IllegalArgumentException if {@code child} is {@code null} or is not a child of this node
     * @see #children()
     */
    public N getChildAfter ( final N child )
    {
        if ( child == null )
        {
            throw new IllegalArgumentException ( "argument is null" );
        }
        final int index = getIndex ( child );
        if ( index == -1 )
        {
            throw new IllegalArgumentException ( "node is not a child" );
        }
        return index < getChildCount () - 1 ? getChildAt ( index + 1 ) : null;
    }

    /**
     * Returns the child in this node's child {@link List} that immediately precedes {@code child}, which must be a child of this node.
     * If {@code child} is the first child, returns {@code null}.
     * This method performs a linear search of this node's children for {@code child} and is O(n) where n is the number of children.
     *
     * @param child child to find another child before
     * @return the child of this node that immediately precedes {@code child}
     * @throws IllegalArgumentException if {@code child} is {@code null} or is not a child of this node
     */
    public N getChildBefore ( final N child )
    {
        if ( child == null )
        {
            throw new IllegalArgumentException ( "argument is null" );
        }
        final int index = getIndex ( child );
        if ( index == -1 )
        {
            throw new IllegalArgumentException ( "argument is not a child" );
        }
        return index > 0 ? getChildAt ( index - 1 ) : null;
    }

    /**
     * Returns true if {@code anotherNode} is a sibling of (has the same parent as) this node.
     * If{@code anotherNode} is {@code null}, returns false.
     * Note that any node is always its own sibling.
     *
     * @param anotherNode node to test as sibling of this node
     * @return {@code true} if {@code anotherNode} is a sibling of this node, {@code false} otherwise
     */
    public boolean isNodeSibling ( final N anotherNode )
    {
        final boolean sibling;
        if ( anotherNode == null )
        {
            sibling = false;
        }
        else if ( anotherNode == this )
        {
            sibling = true;
        }
        else
        {
            final TreeNode myParent = getParent ();
            sibling = myParent != null && myParent == anotherNode.getParent ();
            if ( sibling && !getParent ().isNodeChild ( anotherNode ) )
            {
                throw new Error ( "sibling has different parent" );
            }
        }
        return sibling;
    }

    /**
     * Returns the number of siblings of this node.
     * If it has no parent or no siblings, this method returns {@code 1}.
     * Note that any node is always its own sibling.
     *
     * @return the number of siblings of this node
     */
    public int getSiblingCount ()
    {
        final N myParent = getParent ();
        return myParent != null ? myParent.getChildCount () : 1;
    }

    /**
     * Returns the next sibling of this node in the parent's children {@link List}.
     * Returns {@code null} if this node has no parent or is the parent's last child.
     * This method performs a linear search that is O(n) where n is the number of children.
     * To traverse the entire {@link List}, use the parent's child {@link Enumeration} instead.
     *
     * @return the sibling of this node that immediately follows this node
     * @see #children()
     */
    public N getNextSibling ()
    {
        final N retval;
        final N myParent = getParent ();
        if ( myParent == null )
        {
            retval = null;
        }
        else
        {
            retval = myParent.getChildAfter ( ( N ) this );
        }
        if ( retval != null && !isNodeSibling ( retval ) )
        {
            throw new Error ( "child of parent is not a sibling" );
        }
        return retval;
    }

    /**
     * Returns the previous sibling of this node in the parent's children {@link List}.
     * Returns {@code null} if this node has no parent or is the parent's first child.
     * This method performs a linear search that is O(n) where n is the number of children.
     *
     * @return the sibling of this node that immediately precedes this node
     */
    public N getPreviousSibling ()
    {
        final N retval;
        final N myParent = getParent ();
        if ( myParent == null )
        {
            retval = null;
        }
        else
        {
            retval = myParent.getChildBefore ( ( N ) this );
        }
        if ( retval != null && !isNodeSibling ( retval ) )
        {
            throw new Error ( "child of parent is not a sibling" );
        }
        return retval;
    }

    /**
     * Returns whether or not this node has no children.
     * To distinguish between nodes that have no children and nodes that cannot have children
     * (e.g. to distinguish files from empty directories), use this method in conjunction with {@link #getAllowsChildren()}
     *
     * @return {@code true} if this node has no children, {@code false} otherwise
     * @see #getAllowsChildren()
     */
    @Override
    public boolean isLeaf ()
    {
        return getChildCount () == 0;
    }

    /**
     * Finds and returns the first leaf that is a descendant of this node.
     * It is either this node or its first child's first leaf.
     * Returns this node if it is a leaf.
     *
     * @return the first leaf in the subtree rooted at this node
     * @see #isLeaf()
     * @see #isNodeDescendant(WebTreeNode)
     */
    public N getFirstLeaf ()
    {
        N node = ( N ) this;
        while ( !node.isLeaf () )
        {
            node = node.getFirstChild ();
        }
        return node;
    }

    /**
     * Finds and returns the last leaf that is a descendant of this node.
     * It is either this node or its last child's last leaf.
     * Returns this node if it is a leaf.
     *
     * @return the last leaf in the subtree rooted at this node
     * @see #isLeaf()
     * @see #isNodeDescendant(WebTreeNode)
     */
    public N getLastLeaf ()
    {
        N node = ( N ) this;
        while ( !node.isLeaf () )
        {
            node = node.getLastChild ();
        }
        return node;
    }

    /**
     * Returns the leaf after this node or {@code null} if this node is the last leaf in the tree.
     *
     * In this implementation of the {@code MutableNode} interface, this operation is very inefficient.
     * In order to determine the next node, this method first performs a linear search in the parent's
     * child {@link List} in order to find the current node.
     *
     * That implementation makes the operation suitable for short traversals from a known position.
     * But to traverse all of the leaves in the tree, you should use {@link #depthFirstEnumeration()}
     * to enumerate the nodes in the tree and use {@code isLeaf} on each node to determine which are leaves.
     *
     * @return the next leaf past this node
     * @see #depthFirstEnumeration()
     * @see #isLeaf()
     */
    public N getNextLeaf ()
    {
        final N nextSibling;
        final N myParent = getParent ();
        if ( myParent == null )
        {
            return null;
        }
        nextSibling = getNextSibling ();
        if ( nextSibling != null )
        {
            return nextSibling.getFirstLeaf ();
        }
        return myParent.getNextLeaf ();
    }

    /**
     * Returns the leaf before this node or {@code null} if this node is the first leaf in the tree.
     *
     * In this implementation of the {@code MutableNode} interface, this operation is very inefficient.
     * In order to determine the previous node, this method first performs a linear search in the parent's
     * child {@link List} in order to find the current node.
     *
     * That implementation makes the operation suitable for short traversals from a known position.
     * But to traverse all of the leaves in the tree, you should use {@link #depthFirstEnumeration()}
     * to enumerate the nodes in the tree and use {@code isLeaf} on each node to determine which are leaves.
     *
     * @return the leaf before this node
     * @see #depthFirstEnumeration()
     * @see #isLeaf()
     */
    public N getPreviousLeaf ()
    {
        final N previousSibling;
        final N myParent = getParent ();
        if ( myParent == null )
        {
            return null;
        }
        previousSibling = getPreviousSibling ();
        if ( previousSibling != null )
        {
            return previousSibling.getLastLeaf ();
        }
        return myParent.getPreviousLeaf ();
    }

    /**
     * Returns the total number of leaves that are descendants of this node.
     * If this node is a leaf, returns {@code 1}.
     * This method is O(n) where n is the number of descendants of this node.
     *
     * @return the number of leaves beneath this node
     * @see #isNodeAncestor(WebTreeNode)
     */
    public int getLeafCount ()
    {
        int count = 0;
        N node;
        final Enumeration<N> breadthFirst = breadthFirstEnumeration ();
        while ( breadthFirst.hasMoreElements () )
        {
            node = breadthFirst.nextElement ();
            if ( node.isLeaf () )
            {
                count++;
            }
        }
        if ( count < 1 )
        {
            throw new Error ( "tree has zero leaves" );
        }
        return count;
    }

    /**
     * Returns the result of sending {@code toString()} to this node's user object, or {@code null} if this node has no user object.
     *
     * @see #getUserObject()
     */
    @Override
    public String toString ()
    {
        return userObject != null ? userObject.toString () : null;
    }

    /**
     * Overridden to make clone public, returns a shallow copy of this node.
     * The new node has no parent or children and has a reference to the same user object, if any.
     *
     * @return a copy of this node
     */
    @Override
    public WebTreeNode<N, T> clone ()
    {
        final N newNode;
        try
        {
            newNode = ( N ) super.clone ();

            // Shallow copy, no parent or children
            newNode.children = null;
            newNode.parent = null;

        }
        catch ( final CloneNotSupportedException e )
        {
            // Won't happen because we implement Cloneable
            throw new Error ( e.toString () );
        }
        return newNode;
    }

    /**
     * Writes {@link WebTreeNode} into the specified {@link ObjectOutputStream}.
     *
     * @param outputStream {@link ObjectOutputStream}
     * @throws IOException if an I/O error occurs while writing to the underlying {@link java.io.OutputStream}
     */
    private void writeObject ( final ObjectOutputStream outputStream ) throws IOException
    {
        final Serializable[] tValues;
        outputStream.defaultWriteObject ();
        if ( userObject != null && userObject instanceof Serializable )
        {
            tValues = new Serializable[ 2 ];
            tValues[ 0 ] = "userObject";
            tValues[ 1 ] = ( Serializable ) userObject;
        }
        else
        {
            tValues = new Serializable[ 0 ];
        }
        outputStream.writeObject ( tValues );
    }

    /**
     * Reads {@link WebTreeNode} from the specified {@link ObjectInputStream}.
     *
     * @param inputStream {@link ObjectInputStream}
     * @throws IOException            if an I/O error occurs while reading from the underlying {@link java.io.InputStream}
     * @throws ClassNotFoundException if the class of a serialized object could not be found
     */
    private void readObject ( final ObjectInputStream inputStream ) throws IOException, ClassNotFoundException
    {
        final Serializable[] tValues;
        inputStream.defaultReadObject ();
        tValues = ( Serializable[] ) inputStream.readObject ();
        if ( tValues.length > 0 && tValues[ 0 ].equals ( "userObject" ) )
        {
            userObject = ( T ) tValues[ 1 ];
        }
    }

    /**
     *
     * @param <E>
     * @param <V>
     */
    private final class PreorderEnumeration<E extends WebTreeNode<E, V>, V> implements Enumeration<E>
    {
        protected final Stack<Enumeration<E>> stack;

        public PreorderEnumeration ( final E rootNode )
        {
            super ();
            stack = new Stack<Enumeration<E>> ();
            stack.push ( Collections.enumeration ( Collections.singletonList ( rootNode ) ) );
        }

        @Override
        public boolean hasMoreElements ()
        {
            return !stack.empty () && stack.peek ().hasMoreElements ();
        }

        @Override
        public E nextElement ()
        {
            final Enumeration<E> enumeration = stack.peek ();
            final E node = enumeration.nextElement ();
            final Enumeration<E> children = node.children ();
            if ( !enumeration.hasMoreElements () )
            {
                stack.pop ();
            }
            if ( children.hasMoreElements () )
            {
                stack.push ( children );
            }
            return node;
        }
    }

    /**
     *
     * @param <E>
     * @param <V>
     */
    private final class PostorderEnumeration<E extends WebTreeNode<E, V>, V> implements Enumeration<E>
    {
        protected E root;
        protected Enumeration<E> children;
        protected Enumeration<E> subtree;

        public PostorderEnumeration ( final E rootNode )
        {
            super ();
            root = rootNode;
            children = root.children ();
            subtree = EmptyEnumeration.instance ();
        }

        @Override
        public boolean hasMoreElements ()
        {
            return root != null;
        }

        @Override
        public E nextElement ()
        {
            final E retval;
            if ( subtree.hasMoreElements () )
            {
                retval = subtree.nextElement ();
            }
            else if ( children.hasMoreElements () )
            {
                subtree = new PostorderEnumeration ( children.nextElement () );
                retval = subtree.nextElement ();
            }
            else
            {
                retval = root;
                root = null;
            }
            return retval;
        }
    }

    /**
     *
     * @param <E>
     * @param <V>
     */
    private final class BreadthFirstEnumeration<E extends WebTreeNode<E, V>, V> implements Enumeration<E>
    {
        private Queue<Enumeration<E>> queue;

        public BreadthFirstEnumeration ( final E rootNode )
        {
            super ();
            queue = new Queue<Enumeration<E>> ();
            queue.enqueue ( Collections.enumeration ( Collections.singletonList ( rootNode ) ) );
        }

        @Override
        public boolean hasMoreElements ()
        {
            return !queue.isEmpty () && queue.firstObject ().hasMoreElements ();
        }

        @Override
        public E nextElement ()
        {
            final Enumeration<E> enumeration = queue.firstObject ();
            final E node = enumeration.nextElement ();
            final Enumeration<E> children = node.children ();
            if ( !enumeration.hasMoreElements () )
            {
                queue.dequeue ();
            }
            if ( children.hasMoreElements () )
            {
                queue.enqueue ( children );
            }
            return node;
        }

        // A simple queue with a linked list data structure.
        private final class Queue<Q>
        {
            private QNode<Q> head;    // null if empty
            private QNode<Q> tail;

            public void enqueue ( final Q object )
            {
                if ( head == null )
                {
                    head = tail = new QNode<Q> ( object, null );
                }
                else
                {
                    tail.next = new QNode<Q> ( object, null );
                    tail = tail.next;
                }
            }

            public Q dequeue ()
            {
                if ( head == null )
                {
                    throw new NoSuchElementException ( "No more elements" );
                }
                final Q retval = head.object;
                final QNode<Q> oldHead = head;
                head = head.next;
                if ( head == null )
                {
                    tail = null;
                }
                else
                {
                    oldHead.next = null;
                }
                return retval;
            }

            public Q firstObject ()
            {
                if ( head == null )
                {
                    throw new NoSuchElementException ( "No more elements" );
                }
                return head.object;
            }

            public boolean isEmpty ()
            {
                return head == null;
            }

            private final class QNode<O>
            {
                public O object;
                public QNode<O> next;    // null if end

                public QNode ( final O object, final QNode<O> next )
                {
                    this.object = object;
                    this.next = next;
                }
            }
        }
    }

    /**
     *
     * @param <E>
     * @param <V>
     */
    private final class PathBetweenNodesEnumeration<E extends WebTreeNode<E, V>, V> implements Enumeration<E>
    {
        protected Stack<E> stack;

        public PathBetweenNodesEnumeration ( final E ancestor, final E descendant )
        {
            super ();

            if ( ancestor == null || descendant == null )
            {
                throw new IllegalArgumentException ( "Ancestor and descendant cannot be null" );
            }

            E current;

            stack = new Stack<E> ();
            stack.push ( descendant );

            current = descendant;
            while ( current != ancestor )
            {
                current = current.getParent ();
                if ( current == null && descendant != ancestor )
                {
                    final String msg = "Node '%s' is not an ancestor of '%s'";
                    throw new IllegalArgumentException ( String.format ( msg, ancestor, descendant ) );
                }
                stack.push ( current );
            }
        }

        @Override
        public boolean hasMoreElements ()
        {
            return stack.size () > 0;
        }

        @Override
        public E nextElement ()
        {
            try
            {
                return stack.pop ();
            }
            catch ( final EmptyStackException e )
            {
                throw new NoSuchElementException ( "No more elements" );
            }
        }
    }
}