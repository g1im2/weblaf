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

package com.alee.painter;

import com.alee.api.jdk.Consumer;
import com.alee.laf.WebLookAndFeel;
import com.alee.managers.style.Bounds;
import com.alee.managers.style.BoundsType;
import com.alee.managers.style.PainterShapeProvider;
import com.alee.managers.style.StyleManager;
import com.alee.managers.style.data.ComponentStyle;
import com.alee.painter.decoration.AbstractDecorationPainter;
import com.alee.utils.LafUtils;
import com.alee.utils.ReflectUtils;
import com.alee.utils.SwingUtils;
import com.alee.utils.general.Pair;
import com.alee.utils.swing.WeakComponentData;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.lang.ref.WeakReference;

/**
 * This class provides utilities for linking {@link Painter}s with component UIs.
 * It was added to simplify {@link Painter}s usage within UI classes tied to specific {@link ComponentUI} implementations.
 * Without this utility class a lot of code copy-paste would be required between all different UI implementations.
 *
 * {@link Painter}s do not suffer from that issue since they are implemented differently - each specific Painters has its own interface
 * unlike {@link ComponentUI}s which are not based on interfaces but always abstract classes, like {@link javax.swing.plaf.ButtonUI} one.
 *
 * @author Mikle Garin
 */

public final class PainterSupport
{
    /**
     * Installed painters.
     * todo These should be moved into {@link StyleManager} and preserver in {@link com.alee.managers.style.StyleData}
     *
     * @see #installPainter(JComponent, Painter)
     * @see #uninstallPainter(JComponent, Painter)
     */
    private static final WeakComponentData<JComponent, Pair<Painter, PainterListener>> installedPainters =
            new WeakComponentData<JComponent, Pair<Painter, PainterListener>> ( "PainterSupport.painter", 200 );

    /**
     * Margins saved per-component instance.
     * todo These settings should be completely moved into {@link AbstractPainter} upon multiple painters elimination
     *
     * @see #getMargin(JComponent)
     * @see #setMargin(JComponent, Insets)
     */
    private static final WeakComponentData<JComponent, Insets> margins =
            new WeakComponentData<JComponent, Insets> ( "PainterSupport.margin", 200 );

    /**
     * Paddings saved per-component instance.
     * todo These settings should be completely moved into {@link AbstractPainter} upon multiple painters elimination
     */
    private static final WeakComponentData<JComponent, Insets> paddings =
            new WeakComponentData<JComponent, Insets> ( "PainterSupport.padding", 200 );

    /**
     * Returns either the specified painter if it is not an adapted painter or the adapted painter.
     * Used by component UIs to retrieve painters adapted for their specific needs.
     *
     * @param painter painter to process
     * @param <P>     desired painter type
     * @return either the specified painter if it is not an adapted painter or the adapted painter
     */
    public static <P extends Painter> P getPainter ( final Painter painter )
    {
        return ( P ) ( painter != null && painter instanceof AdaptivePainter ? ( ( AdaptivePainter ) painter ).getPainter () : painter );
    }

    /**
     * Sets component painter.
     * {@code null} can be provided to uninstall painter.
     *
     * @param component            component painter should be installed into
     * @param setter               runnable that updates actual painter field
     * @param oldPainter           previously installed painter
     * @param painter              painter to install
     * @param specificClass        specific painter class
     * @param specificAdapterClass specific painter adapter class
     * @param <P>                  specific painter class type
     */
    public static <P extends SpecificPainter> void setPainter ( final JComponent component, final Consumer<P> setter,
                                                                final P oldPainter, final Painter painter, final Class<P> specificClass,
                                                                final Class<? extends P> specificAdapterClass )
    {
        // Creating adaptive painter if required
        final P newPainter = getApplicablePainter ( painter, specificClass, specificAdapterClass );

        // Properly updating painter
        uninstallPainter ( component, oldPainter );
        setter.accept ( newPainter );
        installPainter ( component, newPainter );

        // Firing painter change event
        SwingUtils.firePropertyChanged ( component, WebLookAndFeel.PAINTER_PROPERTY, oldPainter, newPainter );
    }

    /**
     * Returns the specified painter if it can be assigned to proper painter type.
     * Otherwise returns newly created adapter painter that wraps the specified painter.
     * Used by component UIs to adapt general-type painters for their specific-type needs.
     *
     * @param painter      processed painter
     * @param properClass  proper painter class
     * @param adapterClass adapter painter class
     * @param <P>          proper painter type
     * @return specified painter if it can be assigned to proper painter type, new painter adapter if it cannot be assigned
     */
    private static <P extends SpecificPainter> P getApplicablePainter ( final Painter painter, final Class<P> properClass,
                                                                        final Class<? extends P> adapterClass )
    {
        if ( painter == null )
        {
            return null;
        }
        else
        {
            if ( ReflectUtils.isAssignable ( properClass, painter.getClass () ) )
            {
                return ( P ) painter;
            }
            else
            {
                return ( P ) ReflectUtils.createInstanceSafely ( adapterClass, painter );
            }
        }
    }

    /**
     * Installs painter into the specified component.
     * It is highly recommended to call this method only from EDT.
     * todo Move this code into {@link AbstractPainter#install(JComponent, ComponentUI)}
     *
     * @param component component painter is applied to
     * @param painter   painter to install
     */
    private static void installPainter ( final JComponent component, final Painter painter )
    {
        // Simply ignore this call if empty painter is set or component doesn't exist
        if ( component != null && painter != null )
        {
            // Installing painter
            if ( !installedPainters.contains ( component ) )
            {
                // Installing painter
                painter.install ( component, LafUtils.getUI ( component ) );

                // Applying initial component settings
                final Boolean opaque = painter.isOpaque ();
                if ( opaque != null )
                {
                    LookAndFeel.installProperty ( component, WebLookAndFeel.OPAQUE_PROPERTY, opaque ? Boolean.TRUE : Boolean.FALSE );
                }

                // Creating weak references to use them inside the listener
                // Otherwise we will force it to keep strong reference to component and painter if we use them directly
                final WeakReference<JComponent> c = new WeakReference<JComponent> ( component );
                final WeakReference<Painter> p = new WeakReference<Painter> ( painter );

                // Adding painter listener
                final PainterListener listener = new PainterListener ()
                {
                    @Override
                    public void repaint ()
                    {
                        // Forcing component to be repainted
                        c.get ().repaint ();
                    }

                    @Override
                    public void repaint ( final int x, final int y, final int width, final int height )
                    {
                        // Forcing component to be repainted
                        c.get ().repaint ( x, y, width, height );
                    }

                    @Override
                    public void revalidate ()
                    {
                        // Forcing layout updates
                        c.get ().revalidate ();
                    }

                    @Override
                    public void updateOpacity ()
                    {
                        // Updating component opacity according to painter
                        final Painter painter = p.get ();
                        if ( painter != null )
                        {
                            final Boolean opaque = painter.isOpaque ();
                            if ( opaque != null )
                            {
                                LookAndFeel.installProperty ( c.get (), WebLookAndFeel.OPAQUE_PROPERTY,
                                        opaque ? Boolean.TRUE : Boolean.FALSE );
                            }
                        }
                    }
                };
                painter.addPainterListener ( listener );

                // Saving painter-listener pair
                installedPainters.set ( component, new Pair<Painter, PainterListener> ( painter, listener ) );
            }
            else
            {
                // Inform about painters usage issue
                final String msg = "Another painter is already installed on component: %s";
                throw new PainterException ( String.format ( msg, component ) );
            }
        }
    }

    /**
     * Uninstalls painter from the specified component.
     * It is highly recommended to call this method only from EDT.
     * todo Move this code into {@link AbstractPainter#uninstall(JComponent, ComponentUI)}
     *
     * @param component component painter is uninstalled from
     * @param painter   painter to uninstall
     */
    private static void uninstallPainter ( final JComponent component, final Painter painter )
    {
        // Simply ignore this call if painter or component doesn't exist
        if ( component != null && painter != null )
        {
            if ( installedPainters.contains ( component ) )
            {

                final Pair<Painter, PainterListener> installed = installedPainters.get ( component );
                final Painter installedPainter = installed.getKey ();
                if ( installedPainter == painter )
                {
                    // Removing custom listener
                    installedPainter.removePainterListener ( installed.getValue () );

                    // Uninstalling painter
                    installedPainter.uninstall ( component, LafUtils.getUI ( component ) );

                    // Removing painter reference
                    installedPainters.clear ( component );
                }
                else
                {
                    // Inform about painters usage issue
                    final String msg = "Wrong painter uninstall was requested for component: %s";
                    throw new PainterException ( String.format ( msg, component ) );
                }
            }
            else
            {
                // Inform about painters usage issue
                final String msg = "There are no painters installed on component: %s";
                throw new PainterException ( String.format ( msg, component ) );
            }
        }
    }

    /**
     * Returns component border insets or {@code null} if component doesn't have borders.
     * {@code null} is basically the same as an empty [0,0,0,0] border insets.
     *
     * @param component component to retrieve border insets from
     * @return component border insets or {@code null} if component doesn't have borders
     */
    public static Insets getInsets ( final Component component )
    {
        if ( component instanceof JComponent )
        {
            return ( ( JComponent ) component ).getInsets ();
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns current component margin.
     * Might return {@code null} which is basically the same as an empty [0,0,0,0] margin.
     *
     * @param component component to retrieve margin from
     * @return current component margin
     */
    public static Insets getMargin ( final JComponent component )
    {
        /*if ( component instanceof MarginSupport )
        {
            return ( ( MarginSupport ) component ).getMargin ();
        }
        else
        {
            final ComponentUI ui = LafUtils.getUI ( component );
            if ( ui instanceof MarginSupport )
            {
                return ( ( MarginSupport ) ui ).getMargin ();
            }
            else
            {
                return null;
            }
        }*/
        return margins.get ( component );
    }

    /**
     * Sets new component margin.
     * {@code null} can be provided to set an empty [0,0,0,0] margin.
     *
     * @param component component to set margin for
     * @param margin    new margin
     */
    public static void setMargin ( final JComponent component, final Insets margin )
    {
        // Updating margin cache
        final Insets oldMargin = margins.set ( component, margin );

        // Notifying everyone about component margin changes
        SwingUtils.firePropertyChanged ( component, WebLookAndFeel.LAF_MARGIN_PROPERTY, oldMargin, margin );
    }

    /**
     * Returns current component padding.
     * Might return {@code null} which is basically the same as an empty [0,0,0,0] padding.
     *
     * @param component component to retrieve padding from
     * @return current component padding
     */
    public static Insets getPadding ( final JComponent component )
    {
        return paddings.get ( component );
    }

    /**
     * Sets new padding.
     * {@code null} can be provided to set an empty [0,0,0,0] padding.
     *
     * @param component component to set padding for
     * @param padding   new padding
     */
    public static void setPadding ( final JComponent component, final Insets padding )
    {
        // Updating padding cache
        final Insets oldPadding = paddings.set ( component, padding );

        // Notifying everyone about component padding changes
        SwingUtils.firePropertyChanged ( component, WebLookAndFeel.LAF_PADDING_PROPERTY, oldPadding, padding );
    }

    /**
     * Returns component shape according to its painter.
     *
     * @param component component painter is applied to
     * @param painter   component painter
     * @return component shape according to its painter
     */
    public static Shape getShape ( final JComponent component, final Painter painter )
    {
        if ( painter != null && painter instanceof PainterShapeProvider )
        {
            return ( ( PainterShapeProvider ) painter ).provideShape ( component, BoundsType.margin.bounds ( component ) );
        }
        else
        {
            return BoundsType.margin.bounds ( component );
        }
    }

    /**
     * Returns component baseline for the specified component size, measured from the top of the component bounds.
     * A return value less than {@code 0} indicates this component does not have a reasonable baseline.
     * This method is primarily meant for {@code java.awt.LayoutManager}s to align components along their baseline.
     *
     * @param component aligned component
     * @param ui        aligned component UI
     * @param painter   aligned component painter
     * @param width     approximate component width
     * @param height    approximate component height
     * @return component baseline within the specified bounds, measured from the top of the bounds
     */
    public static int getBaseline ( final JComponent component, final ComponentUI ui,
                                    final Painter painter, final int width, final int height )
    {
        // Default baseline
        int baseline = -1;

        // Painter baseline support
        if ( painter != null )
        {
            // Creating appropriate bounds for painter
            final Bounds componentBounds = new Bounds ( new Dimension ( width, height ) );

            // Retrieving baseline provided by painter
            baseline = painter.getBaseline ( component, ui, componentBounds );
        }

        // Border baseline support
        // Taken from JPanel baseline implementation
        if ( baseline == -1 )
        {
            final Border border = component.getBorder ();
            if ( border instanceof AbstractBorder )
            {
                baseline = ( ( AbstractBorder ) border ).getBaseline ( component, width, height );
            }
        }

        return baseline;
    }

    /**
     * Returns enum indicating how the baseline of the component changes as the size changes.
     *
     * @param component aligned component
     * @param ui        aligned component UI
     * @param painter   aligned component painter
     * @return enum indicating how the baseline of the component changes as the size changes
     */
    public static Component.BaselineResizeBehavior getBaselineResizeBehavior ( final JComponent component, final ComponentUI ui,
                                                                               final Painter painter )
    {
        // Default behavior
        Component.BaselineResizeBehavior behavior = Component.BaselineResizeBehavior.OTHER;

        // Painter baseline behavior support
        if ( painter != null )
        {
            // Retrieving baseline behavior provided by painter
            return painter.getBaselineResizeBehavior ( component, ui );
        }

        // Border baseline behavior support
        // Taken from JPanel baseline implementation
        if ( behavior == Component.BaselineResizeBehavior.OTHER )
        {
            final Border border = component.getBorder ();
            if ( border instanceof AbstractBorder )
            {
                behavior = ( ( AbstractBorder ) border ).getBaselineResizeBehavior ( component );
            }
        }

        return behavior;
    }

    /**
     * Returns component preferred size or {@code null} if there is no preferred size.
     *
     * @param component component painter is applied to
     * @param painter   component painter
     * @return component preferred size or {@code null} if there is no preferred size
     */
    public static Dimension getPreferredSize ( final JComponent component, final Painter painter )
    {
        return getPreferredSize ( component, null, painter );
    }

    /**
     * Returns component preferred size or {@code null} if there is no preferred size.
     * todo Probably get rid of this method and force painters to determine full preferred size?
     *
     * @param component component painter is applied to
     * @param preferred component preferred size
     * @param painter   component painter
     * @return component preferred size or {@code null} if there is no preferred size
     */
    public static Dimension getPreferredSize ( final JComponent component, final Dimension preferred, final Painter painter )
    {
        return getPreferredSize ( component, preferred, painter, false );
    }

    /**
     * Returns component preferred size or {@code null} if there is no preferred size.
     *
     * @param component        component painter is applied to
     * @param preferred        component preferred size
     * @param painter          component painter
     * @param ignoreLayoutSize whether or not layout preferred size should be ignored
     * @return component preferred size or {@code null} if there is no preferred size
     */
    public static Dimension getPreferredSize ( final JComponent component, final Dimension preferred, final Painter painter,
                                               final boolean ignoreLayoutSize )
    {
        // Event Dispatch Thread check
        WebLookAndFeel.checkEventDispatchThread ();

        // Painter's preferred size
        Dimension ps = SwingUtils.max ( preferred, painter != null ? painter.getPreferredSize () : null );

        // Layout preferred size
        if ( !ignoreLayoutSize )
        {
            final LayoutManager layout = component.getLayout ();
            if ( layout != null )
            {
                ps = SwingUtils.max ( ps, layout.preferredLayoutSize ( component ) );
            }
        }

        return ps;
    }

    /**
     * Returns whether or not component uses decoratable painter.
     *
     * @param component component to process
     * @return true if component uses decoratable painter, false otherwise
     */
    public static boolean isDecoratable ( final Component component )
    {
        if ( component instanceof JComponent )
        {
            final JComponent jComponent = ( JComponent ) component;
            final ComponentStyle style = StyleManager.getSkin ( jComponent ).getStyle ( jComponent );
            final Painter painter = style != null ? style.getPainter ( jComponent ) : null;
            return painter != null && painter instanceof AbstractDecorationPainter;
            // todo Add additional decoration conditions? For: && ((AbstractDecorationPainter)painter)...
        }
        else
        {
            return false;
        }
    }
}