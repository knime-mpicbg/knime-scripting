package de.mpicbg.knime.knutils;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Row iterator of the range filter table. Wraps a given row iterator and forwards
 * only rows that are approved by a given set of filters (RowFilterIterator taken as example to create this class)
 * <p/>
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 11/30/11
 * Time: 8:34 AM
 */
public class RowMultiFilterIterator extends RowIterator {

    // the filter
    private final List<RowFilter> m_filters;

    // the original row iterator we are wrapping
    private final RowIterator m_orig;

    // always holds the next matching row.
    private DataRow m_nextRow;

    // the number of rows read from the original. If m_nextRow is not null it
    // is the row number of that row in the original table.
    private int m_rowNumber;

    // If true the filter will not be asked - every row will be included in the result.
    // set if the exception "IncludeFromNowOn" occurs
    private boolean m_includeRest;

    // if true, row should be kept if all filters matches
    // otherwise row should be kept if at least one filter matches
    private final boolean m_allFilters;

    // if true, return matching rows otherwise return row which do not match
    private final boolean m_include;

    // the exec mon for cancel/progress, may use default one
    private final ExecutionMonitor m_exec;

    // the row count in the original table
    private final int m_totalCountInOrig;

    public RowMultiFilterIterator(final BufferedDataTable origTable, final List<RowFilter> filters, final ExecutionMonitor exec, final boolean retainIfAllMatch, final boolean retainMatchingRows) {
        m_filters = filters;
        m_orig = origTable.iterator();

        m_totalCountInOrig = ((BufferedDataTable) origTable).getRowCount();
        m_exec = exec == null ? new ExecutionMonitor() : exec;

        m_rowNumber = 0;
        m_nextRow = null;

        m_allFilters = retainIfAllMatch;
        m_include = retainMatchingRows;

        m_includeRest = false;

        // get the next row to return - for the next call to next()
        m_nextRow = getNextMatch();

    }

    private DataRow getNextMatch() {
        // iterate as long as there is no match (or 'end of table' or 'include from now on' or 'execution cancled')
        while (true) {
            // check if execution has been canceled
            try {
                m_exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new RuntimeCanceledExecutionException(cee);
            }
            // we must not cause any trouble.
            if (!m_orig.hasNext()) {
                return null;
            }
            m_exec.setProgress(m_rowNumber / (double) m_totalCountInOrig);

            DataRow next = m_orig.next();
            if (m_includeRest) {
                m_rowNumber++;
                return next;
            } else {
                // consult the filters whether to include this row
                try {
                    int matchCount = 0;
                    for (RowFilter curFilter : m_filters) {
                        if (curFilter.matches(next, m_rowNumber)) matchCount++;
                    }

                    boolean rowMatches = false;
                    // all filters are matched
                    if (m_allFilters && matchCount == m_filters.size()) rowMatches = true;
                    // at least one filter matches
                    if (!m_allFilters && matchCount > 0) rowMatches = true;

                    if (rowMatches == m_include) return next;

                    // else fall through and get the next row from the orig table.
                } catch (EndOfTableException eote) {
                    // filter: there are now more matching rows. Reached our EOT.
                    m_nextRow = null;
                    return null;
                } catch (IncludeFromNowOn ifno) {
                    // filter: include all rows from now on
                    m_includeRest = true;
                    return next;
                } finally {
                    m_rowNumber++;
                }
            }

        }
    }

    @Override
    public boolean hasNext() {
        return (m_nextRow != null);
    }

    @Override
    public DataRow next() {
        if (m_nextRow == null) {
            throw new NoSuchElementException("The row filter iterator proceeded beyond the last row.");
        }
        DataRow tmp = m_nextRow;
        // always keep the next row in m_nextRow.
        m_nextRow = getNextMatch();
        return tmp;
    }

    /**
     * Runtime exception that's thrown when the execution monitor's
     * {@link ExecutionMonitor#checkCanceled} method throws a
     * {@link CanceledExecutionException}.
     */
    public static final class RuntimeCanceledExecutionException extends
            RuntimeException {

        /**
         * Inits object.
         *
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(
                final CanceledExecutionException cee) {
            super(cee.getMessage(), cee);
        }

        /**
         * Get reference to causing exception.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public CanceledExecutionException getCause() {
            return (CanceledExecutionException) super.getCause();
        }
    }
}


