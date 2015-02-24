/**
 * 
 */
package de.mpicbg.knime.scripting.core;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * @author niederle
 *
 */
public class ResizableCardLayout extends CardLayout {

	/** 
	 * adapts preferred size to content of current visible component
	 */
	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Component current = findCurrentComponent(parent);
        if (current != null) {
            Insets insets = parent.getInsets();
            Dimension pref = current.getPreferredSize();
            pref.width += insets.left + insets.right;
            pref.height += insets.top + insets.bottom;
            return pref;
        }
        return super.preferredLayoutSize(parent);
	}

	/**
	 * retrieves the currently visible component
	 * @param parent
	 * @return
	 */
	private Component findCurrentComponent(Container parent) {
		for (Component comp : parent.getComponents()) {
            if (comp.isVisible()) {
                return comp;
            }
        }
        return null;
	}

}
