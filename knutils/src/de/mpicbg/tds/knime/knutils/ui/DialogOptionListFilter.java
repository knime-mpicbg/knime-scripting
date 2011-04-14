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
 * -------------------------------------------------------------------
 *
 * History
 *   16.11.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package de.mpicbg.tds.knime.knutils.ui;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObjectSpec;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.List;


/**
 * Provides a component for column filtering. This component for the default dialog allows to enter a list of columns to
 * include from the set of available columns.
 *
 * @author M. Berthold, University of Konstanz
 */
public class DialogOptionListFilter extends DialogComponent {

    /**
     * Index of the port to take the table (spec) from.
     */
    private final int m_inPortIndex;

    private final TwoPaneSelectionPanel<String> m_columnFilter;

    /**
     * Table spec that was sent last into the filter component.
     */
    private DataTableSpec m_specInFilter;


    /**
     * Creates a new filter column panel with three component which are the include list, button panel to shift elements
     * between the two lists, and the exclude list. The include list then will contain all values to filter. The allowed
     * types filters out every column which is not compatible with the allowed type.
     *
     * @param model       a string array model that stores the value
     * @param inPortIndex the index of the port whose table is filtered
     */
    public DialogOptionListFilter(final SettingsModelFilterString model, final int inPortIndex) {
        this(model, inPortIndex, new DefaultListCellRenderer());
    }


    public DialogOptionListFilter(final SettingsModelFilterString model, final int inPortIndex, ListCellRenderer cellRenderer) {
        super(model);

        m_inPortIndex = inPortIndex;
        m_specInFilter = null;

        m_columnFilter = new TwoPaneSelectionPanel(false, cellRenderer);
        getComponentPanel().add(m_columnFilter);

        // when the user input changes we need to update the model.
        m_columnFilter.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateModel();
            }
        });
        // update the components, when the value in the model changes
//        getModel().prependChangeListener(new ChangeListener() {
        getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        // to be in sync with the settings model (clear settings model)
        updateModel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        // update component only if content is out of sync
        final SettingsModelFilterString filterModel =
                (SettingsModelFilterString) getModel();
        final List<String> compIncl = m_columnFilter.getIncludedColumnSet();
        final List<String> compExcl = m_columnFilter.getExcludedColumnSet();
        final List<String> modelIncl = filterModel.getIncludeList();
        final List<String> modelExcl = filterModel.getExcludeList();
        final boolean modelKeepAll = filterModel.isKeepAllSelected();

        boolean update =
                (compIncl.size() != modelIncl.size())
                        || (compExcl.size() != modelExcl.size());

        if (!update) {
            // update if the current spec and the spec we last updated with
            // are different
            final PortObjectSpec currPOSpec = getLastTableSpec(m_inPortIndex);
            if (currPOSpec == null) {
                update = false;
            } else {
                if (!(currPOSpec instanceof DataTableSpec)) {
                    throw new RuntimeException("Wrong type of PortObject for"
                            + " ColumnFilterPanel, expecting DataTableSpec!");
                }
                final DataTableSpec currSpec = (DataTableSpec) currPOSpec;
                update = (!currSpec.equalStructure(m_specInFilter));
            }
        }
        if (!update) {
            // one way check, because size is equal
            update = !modelIncl.containsAll(compIncl);
        }
        if (!update) {
            // one way check, because size is equal
            update = !modelExcl.containsAll(compExcl);
        }
        if (update) {
            m_specInFilter = (DataTableSpec) getLastTableSpec(m_inPortIndex);
            if (m_specInFilter == null) {
                // the component doesn't take a null spec. Create an empty one
                m_specInFilter = new DataTableSpec();
            }

            m_columnFilter.update(filterModel.getExcludeList(), filterModel.getIncludeList());
        }

        // also update the enable status
        setEnabledComponents(filterModel.isEnabled());
    }


    /**
     * transfers the settings from the component into the settings model.
     */
    private void updateModel() {
        final SettingsModelFilterString filterModel =
                (SettingsModelFilterString) getModel();
        final List<String> inclList = m_columnFilter.getIncludedColumnSet();
        final List<String> exclList = m_columnFilter.getExcludedColumnSet();

        filterModel.setNewValues(inclList, exclList, false);
    }


    /**
     * We store the values from the panel in the model now.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        // just in case we didn't get notified about the last change...
        updateModel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // currently we open the dialog even with an empty spec - causing the
        // panel to be empty.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_columnFilter.setEnabled(enabled);
    }


    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public void setIncludeTitle(final String title) {
        m_columnFilter.setIncludeTitle(title);
    }


    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public void setExcludeTitle(final String title) {
        m_columnFilter.setExcludeTitle(title);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_columnFilter.setToolTipText(text);
    }
}
