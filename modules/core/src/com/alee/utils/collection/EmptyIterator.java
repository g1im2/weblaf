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

package com.alee.utils.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * {@link Iterator} implementation that has no elements.
 *
 * @param <E> elements type
 * @author Mikle Garin
 */

public final class EmptyIterator<E> implements Iterator<E>
{
    /**
     * {@link EmptyIterator} singleton instance.
     */
    private static Iterator instance;

    /**
     * Returns {@link EmptyIterator} instance.
     *
     * @param <E> elements type
     * @return {@link EmptyIterator} instance
     */
    public static synchronized <E> Iterator<E> instance ()
    {
        if ( instance == null )
        {
            instance = new EmptyIterator ();
        }
        return instance;
    }

    /**
     * We do not want anyone to construct {@link EmptyIterator} directly, use {@link #instance()} instead.
     */
    private EmptyIterator ()
    {
        super ();
    }

    @Override
    public boolean hasNext ()
    {
        return false;
    }

    @Override
    public E next () throws NoSuchElementException
    {
        throw new UnsupportedOperationException ( "EmptyIterator does not contain any elements" );
    }

    @Override
    public void remove ()
    {
        throw new UnsupportedOperationException ( "EmptyIterator does not contain any elements" );
    }
}