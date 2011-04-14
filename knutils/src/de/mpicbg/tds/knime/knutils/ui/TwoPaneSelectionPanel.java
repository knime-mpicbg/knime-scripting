/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 */
package de.mpicbg.tds.knime.knutils.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;


/**
 * Panel is used to select/filter a certain number of columns.
 * <p/>
 * <p/>
 * You can add a property change listener to this class that is notified when the include list changes.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class TwoPaneSelectionPanel<Option> extends JPanel {

    /**
     * Include list.
     */
    private final JList m_inclList;

    /**
     * Include model.
     */
    private final DefaultListModel includeOptions;

    /**
     * Exclude list.
     */
    private final JList m_exclList;

    /**
     * Exclude model.
     */
    private final DefaultListModel availableOptions;

    /**
     * Highlight all search hits in the include model.
     */
    private final JCheckBox m_markAllHitsIncl;

    /**
     * Highlight all search hits in the exclude model.
     */
    private final JCheckBox m_markAllHitsExcl;

    /**
     * Remove all button.
     */
    private final JButton m_remAllButton;

    /**
     * Remove button.
     */
    private final JButton m_remButton;

    /**
     * Add all button.
     */
    private final JButton m_addAllButton;

    /**
     * Add button.
     */
    private final JButton m_addButton;

    /**
     * Search Field in include list.
     */
    private final JTextField m_searchFieldIncl;

    /**
     * Search Button for include list.
     */
    private final JButton m_searchButtonIncl;

    /**
     * Search Field in exclude list.
     */
    private final JTextField m_searchFieldExcl;

    /**
     * Search Button for exclude list.
     */
    private final JButton m_searchButtonExcl;

    /**
     * List of DataCellColumnSpecss to keep initial ordering of DataCells.
     */
    private final LinkedHashSet<Option> optionOrder =
            new LinkedHashSet<Option>();

    /**
     * Border of the include panel, keep it so we can change the title.
     */
    private final TitledBorder m_includeBorder;

    /**
     * Border of the include panel, keep it so we can change the title.
     */
    private final TitledBorder m_excludeBorder;

    private final HashSet<Option> hiddenOptions = new HashSet<Option>();

    private List<ChangeListener> m_listeners;

    /**
     * Line border for include columns.
     */
    private static final Border INCLUDE_BORDER =
            BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /**
     * Line border for exclude columns.
     */
    private static final Border EXCLUDE_BORDER =
            BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    private boolean keepAddOrder = false;


    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter.
     *
     * @param showKeepAllBox true, if an check box to keep all columns is shown
     * @param cellRenderer
     */
    public TwoPaneSelectionPanel(final boolean showKeepAllBox, ListCellRenderer cellRenderer) {
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(20));

        m_addButton = new JButton("add >>");
        m_addButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_addAllButton = new JButton("add all >>");
        m_addAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remButton = new JButton("<< remove");
        m_remButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remAllButton = new JButton("<< remove all");
        m_remAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemoveAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(20));
        buttonPan.add(Box.createGlue());

        // include list
        includeOptions = new DefaultListModel();
        m_inclList = new JList(includeOptions);
        m_inclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onRemIt();
                    me.consume();
                }
            }
        });
        final JScrollPane jspIncl = new JScrollPane(m_inclList);
        jspIncl.setMinimumSize(new Dimension(130, 155));
        jspIncl.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        m_searchFieldIncl = new JTextField(8);
        m_searchButtonIncl = new JButton("Search");
        ActionListener actionListenerIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSearch(m_inclList, includeOptions, m_searchFieldIncl,
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_searchFieldIncl.addActionListener(actionListenerIncl);
        m_searchButtonIncl.addActionListener(actionListenerIncl);
        JPanel inclSearchPanel = new JPanel(new BorderLayout());
        inclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        inclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        inclSearchPanel.add(m_searchFieldIncl, BorderLayout.CENTER);
        inclSearchPanel.add(m_searchButtonIncl, BorderLayout.EAST);
        m_markAllHitsIncl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_inclList.clearSelection();
                onSearch(m_inclList, includeOptions, m_searchFieldIncl,
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_markAllHitsIncl.addActionListener(actionListenerAllIncl);
        inclSearchPanel.add(m_markAllHitsIncl, BorderLayout.PAGE_END);
        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(
                INCLUDE_BORDER, " Selection ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(jspIncl, BorderLayout.CENTER);

        // exclude list
        availableOptions = new DefaultListModel();
        m_exclList = new JList(availableOptions);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_exclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onAddIt();
                    me.consume();
                }
            }
        });
        setListCellRenderer(cellRenderer);
        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setMinimumSize(new Dimension(130, 155));
        jspExcl.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        m_searchFieldExcl = new JTextField(8);
        m_searchButtonExcl = new JButton("Search");
        ActionListener actionListenerExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSearch(m_exclList, availableOptions, m_searchFieldExcl,
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_searchFieldExcl.addActionListener(actionListenerExcl);
        m_searchButtonExcl.addActionListener(actionListenerExcl);
        JPanel exclSearchPanel = new JPanel(new BorderLayout());
        exclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        exclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        exclSearchPanel.add(m_searchFieldExcl, BorderLayout.CENTER);
        exclSearchPanel.add(m_searchButtonExcl, BorderLayout.EAST);
        m_markAllHitsExcl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_exclList.clearSelection();
                onSearch(m_exclList, availableOptions, m_searchFieldExcl,
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_markAllHitsExcl.addActionListener(actionListenerAllExcl);
        exclSearchPanel.add(m_markAllHitsExcl, BorderLayout.PAGE_END);
        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(
                EXCLUDE_BORDER, " Options ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        JPanel buttonPan2 = new JPanel(new GridLayout());
        Border border = BorderFactory.createTitledBorder(" ");
        buttonPan2.setBorder(border);
        buttonPan2.add(buttonPan);


        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        JPanel all = new JPanel();
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
        all.add(center);

        super.setLayout(new GridLayout(1, 1));
        super.add(all);
    }


    /**
     * Enables or disables all components on this panel. {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        enabledComponents(enabled);
    }


    private void enabledComponents(final boolean newEnabled) {
        m_searchFieldIncl.setEnabled(newEnabled);
        m_searchButtonIncl.setEnabled(newEnabled);
        m_searchFieldExcl.setEnabled(newEnabled);
        m_searchButtonExcl.setEnabled(newEnabled);
        m_inclList.setEnabled(newEnabled);
        m_exclList.setEnabled(newEnabled);
        m_markAllHitsIncl.setEnabled(newEnabled);
        m_markAllHitsExcl.setEnabled(newEnabled);
        m_remAllButton.setEnabled(newEnabled);
        m_remButton.setEnabled(newEnabled);
        m_addAllButton.setEnabled(newEnabled);
        m_addButton.setEnabled(newEnabled);
    }


    /**
     * Adds a listener which gets informed whenever the column filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }

        m_listeners.add(listener);
    }


    /**
     * Removes the given listener from this filter column panel.
     *
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }


    /**
     * Removes all column filter change listener.
     */
    public void removeAllColumnFilterChangeListener() {
        if (m_listeners != null) {
            m_listeners.clear();
        }
    }


    private void fireFilteringChangedEvent() {
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }


    /**
     * Called by the 'remove >>' button to exclude the selected elements from the include list.
     */
    private void onRemIt() {
        // add all selected elements from the include to the exclude list
        Object[] o = m_inclList.getSelectedValues();
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        for (Enumeration<?> e = availableOptions.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        boolean changed = false;
        for (Object anO : o) {
            changed |= includeOptions.removeElement(anO);
        }
        availableOptions.removeAllElements();
        for (Option c : optionOrder) {
            if (hash.contains(c)) {
                availableOptions.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }


    /**
     * Called by the 'remove >>' button to exclude all elements from the include list.
     */
    private void onRemoveAll() {
        boolean changed = includeOptions.elements().hasMoreElements();
        includeOptions.removeAllElements();
        availableOptions.removeAllElements();
        for (Option c : optionOrder) {
            if (!hiddenOptions.contains(c)) {
                availableOptions.addElement(c);
            }
        }

        if (changed) {
            fireFilteringChangedEvent();
        }
    }


    /**
     * Called by the '<< add' button to include the selected elements from the exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        Object[] o = m_exclList.getSelectedValues();
        Collection<Object> hash = keepAddOrder ? new HashSet<Object>() : new ArrayList<Object>();

        for (Enumeration<?> e = includeOptions.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }

        hash.addAll(Arrays.asList(o));

        boolean changed = false;

        includeOptions.removeAllElements();

        if (keepAddOrder) {
            for (int i = 0; i < o.length; i++) {
                changed |= availableOptions.removeElement(o[i]);
            }
            for (Option c : optionOrder) {
                if (hash.contains(c)) {
                    includeOptions.addElement(c);
                }
            }

        } else {
            for (Object o1 : hash) {
                changed |= availableOptions.removeElement(o1);
                includeOptions.addElement(o1);
            }
        }

        if (changed) {
            fireFilteringChangedEvent();
        }
    }


    /**
     * Called by the '<< add all' button to include all elements from the exclude list.
     */
    private void onAddAll() {
        boolean changed = availableOptions.elements().hasMoreElements();
        includeOptions.removeAllElements();
        availableOptions.removeAllElements();
        for (Option c : optionOrder) {
            if (!hiddenOptions.contains(c)) {
                includeOptions.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }


    /**
     * Updates this filter panel by removing all current selections from the include and exclude list. The include list
     * will contains all column names from the spec afterwards.
     *
     * @param options the spec to retrieve the option-names from
     */
    public void update(List<Option> options, List<Option> includes) {
        assert (options != null);
        optionOrder.clear();

        includeOptions.removeAllElements();
        availableOptions.removeAllElements();
        hiddenOptions.clear();

        for (Option option : options) {
            optionOrder.add(option);

            availableOptions.addElement(option);
        }


        include(includes);
        repaint();
    }


    public void include(List<Option> includes) {
        for (Option include : includes) {
            if (availableOptions.contains(include)) {
                availableOptions.removeElement(include);
            }

            includeOptions.addElement(include);
        }
    }


    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     */
    public List<Option> getExcludedColumnSet() {
        return getColumnList(availableOptions);
    }


    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public List<Option> getIncludedColumnSet() {
        return getColumnList(includeOptions);
    }


    /**
     * Helper for the get***ColumnList methods.
     *
     * @param model The list from which to retrieve the elements
     */
    private List<Option> getColumnList(final ListModel model) {
        final List<Option> list = new ArrayList<Option>();

        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            list.add((Option) o);
        }

        return list;
    }


    /**
     * This method is called when the user wants to search the given {@link JList} for the text of the given {@link
     * JTextField}.
     *
     * @param list        the list to search in
     * @param model       the list model on which the list is based on
     * @param searchField the text field with the text to search for
     * @param markAllHits if set to <code>true</code> the method will mark all occurrences of the given search text in
     *                    the given list. If set to <code>false</code> the method will mark the next occurrences of the
     *                    search text after the current marked list element.
     */
    private static void onSearch(final JList list,
                                 final DefaultListModel model, final JTextField searchField,
                                 final boolean markAllHits) {
        if (list == null || model == null || searchField == null) {
            return;
        }
        final String searchStr = searchField.getText().trim();
        if (model.isEmpty() || searchStr.equals("")) {
            list.clearSelection();
            return;
        }
        if (markAllHits) {
            int[] searchHits = getAllSearchHits(list, searchStr);
            list.clearSelection();
            if (searchHits.length > 0) {
                list.setSelectedIndices(searchHits);
                list.scrollRectToVisible(list.getCellBounds(searchHits[0],
                        searchHits[0]));
            }
        } else {
            int start = Math.max(0, list.getSelectedIndex() + 1);
            if (start >= model.getSize()) {
                start = 0;
            }
            int f = searchInList(list, searchStr, start);
            if (f >= 0) {
                list.scrollRectToVisible(list.getCellBounds(f, f));
                list.setSelectedIndex(f);
            }
        }
    }

    /*
     * Finds in the list any occurrence of the argument string (as substring).
     */


    private static int searchInList(final JList list, final String str,
                                    final int startIndex) {
        // this method was (slightly modified) copied from
        // JList#getNextMatch
        ListModel model = list.getModel();
        int max = model.getSize();
        String prefix = str;
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();

        int index = startIndex;
        do {
            Object o = model.getElementAt(index);

            if (o != null) {
                String string;
                if (o instanceof String) {
                    string = ((String) o).toUpperCase();
                } else {
                    string = o.toString();
                    if (string != null) {
                        string = string.toUpperCase();
                    }
                }

                if (string != null && string.indexOf(prefix) >= 0) {
                    return index;
                }
            }
            index = (index + 1 + max) % max;
        } while (index != startIndex);

        return -1;
    }


    /**
     * Uses the {@link #searchInList(JList, String, int)} method to get all occurrences of the given string in the given
     * list and returns the index off all occurrences as a <code>int[]</code>.
     *
     * @param list the list to search in
     * @param str  the string to search for
     * @return <code>int[]</code> with the indices off all objects from the given list which match the given string. If
     *         no hits exists the method returns an empty <code>int[]</code>.
     * @see #searchInList(JList, String, int)
     */
    private static int[] getAllSearchHits(final JList list, final String str) {

        ListModel model = list.getModel();
        int max = model.getSize();
        final ArrayList<Integer> hits = new ArrayList<Integer>(max);
        int index = 0;
        do {
            int tempIndex = searchInList(list, str, index);
            // if the search returns no hit or returns a hit before the
            // current search position exit the while loop
            if (tempIndex < index || tempIndex < 0) {
                break;
            }
            index = tempIndex;
            hits.add(new Integer(index));
            // increase the index to start the search from the next position
            // after the current hit
            index++;
        } while (index < max);

        if (hits.size() > 0) {
            final int[] resultArray = new int[hits.size()];
            for (int i = 0, length = hits.size(); i < length; i++) {
                resultArray[i] = hits.get(i).intValue();
            }
            return resultArray;
        }
        return new int[0];
    }


    /**
     * Set the renderer that is used for both list in this panel.
     *
     * @param renderer the new renderer being used
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     */
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        m_inclList.setCellRenderer(renderer);
        m_exclList.setCellRenderer(renderer);
    }

//


    /**
     * Removes the given columns form either include or exclude list and notifies all listeners. Does not throw an
     * exception if the argument contains <code>null</code> elements or is not contained in any of the lists.
     *
     * @param options the columns to remove
     */
    public final void hideColumns(final Option... options) {
        boolean changed = false;
        for (Option column : options) {
            if (includeOptions.contains(column)) {
                hiddenOptions.add(column);
                changed |= includeOptions.removeElement(column);
            } else if (availableOptions.contains(column)) {
                hiddenOptions.add(column);
                changed |= availableOptions.removeElement(column);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }


    /**
     * Re-adds all remove/hidden columns to the exclude list.
     */
    public final void resetHiding() {
        if (hiddenOptions.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(hiddenOptions);
        for (Enumeration<?> e = availableOptions.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        availableOptions.removeAllElements();
        for (Option c : optionOrder) {
            if (hash.contains(c)) {
                availableOptions.addElement(c);
            }
        }
        hiddenOptions.clear();
    }


    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public final void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }


    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public final void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }


    /**
     * Setter for the original "Remove All" button.
     *
     * @param text the new button title
     */
    public void setRemoveAllButtonText(final String text) {
        m_remAllButton.setText(text);
    }


    /**
     * Setter for the original "Add All" button.
     *
     * @param text the new button title
     */
    public void setAddAllButtonText(final String text) {
        m_addAllButton.setText(text);
    }


    /**
     * Setter for the original "Remove" button.
     *
     * @param text the new button title
     */
    public void setRemoveButtonText(final String text) {
        m_remButton.setText(text);
    }


    /**
     * Setter for the original "Add" button.
     *
     * @param text the new button title
     */
    public void setAddButtonText(final String text) {
        m_addButton.setText(text);
    }
}
