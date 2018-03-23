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

package com.alee.laf.menu;

import com.alee.managers.style.*;
import com.alee.painter.DefaultPainter;
import com.alee.painter.Painter;
import com.alee.painter.PainterSupport;
import com.alee.api.jdk.Consumer;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import java.awt.*;

/**
 * Custom UI for {@link JRadioButtonMenuItem} component.
 *
 * @author Mikle Garin
 */

public class WebRadioButtonMenuItemUI extends BasicRadioButtonMenuItemUI implements ShapeSupport, MarginSupport, PaddingSupport
{
    /**
     * Component painter.
     */
    @DefaultPainter ( RadioButtonMenuItemPainter.class )
    protected IRadioButtonMenuItemPainter painter;

    /**
     * Returns an instance of the {@link WebRadioButtonMenuItemUI} for the specified component.
     * This tricky method is used by {@link UIManager} to create component UIs when needed.
     *
     * @param c component that will use UI instance
     * @return instance of the {@link WebRadioButtonMenuItemUI}
     */
    public static ComponentUI createUI ( final JComponent c )
    {
        return new WebRadioButtonMenuItemUI ();
    }

    @Override
    public void installUI ( final JComponent c )
    {
        // Installing UI
        super.installUI ( c );

        // Applying skin
        StyleManager.installSkin ( menuItem );
    }

    @Override
    public void uninstallUI ( final JComponent c )
    {
        // Uninstalling applied skin
        StyleManager.uninstallSkin ( menuItem );

        // Uninstalling UI
        super.uninstallUI ( c );
    }

    @Override
    public Shape getShape ()
    {
        return PainterSupport.getShape ( menuItem, painter );
    }

    @Override
    public Insets getMargin ()
    {
        return PainterSupport.getMargin ( menuItem );
    }

    @Override
    public void setMargin ( final Insets margin )
    {
        PainterSupport.setMargin ( menuItem, margin );
    }

    @Override
    public Insets getPadding ()
    {
        return PainterSupport.getPadding ( menuItem );
    }

    @Override
    public void setPadding ( final Insets padding )
    {
        PainterSupport.setPadding ( menuItem, padding );
    }

    /**
     * Returns menu item painter.
     *
     * @return menu item painter
     */
    public Painter getPainter ()
    {
        return PainterSupport.getPainter ( painter );
    }

    /**
     * Sets menu item painter.
     * Pass null to remove menu item painter.
     *
     * @param painter new menu item painter
     */
    public void setPainter ( final Painter painter )
    {
        PainterSupport.setPainter ( menuItem, new Consumer<IRadioButtonMenuItemPainter> ()
        {
            @Override
            public void accept ( final IRadioButtonMenuItemPainter newPainter )
            {
                WebRadioButtonMenuItemUI.this.painter = newPainter;
            }
        }, this.painter, painter, IRadioButtonMenuItemPainter.class, AdaptiveRadioButtonMenuItemPainter.class );
    }

    @Override
    public int getBaseline ( final JComponent c, final int width, final int height )
    {
        return PainterSupport.getBaseline ( c, this, painter, width, height );
    }

    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior ( final JComponent c )
    {
        return PainterSupport.getBaselineResizeBehavior ( c, this, painter );
    }

    @Override
    public void paint ( final Graphics g, final JComponent c )
    {
        if ( painter != null )
        {
            painter.paint ( ( Graphics2D ) g, c, this, new Bounds ( c ) );
        }
    }

    @Override
    public Dimension getPreferredSize ( final JComponent c )
    {
        return PainterSupport.getPreferredSize ( c, painter );
    }
}