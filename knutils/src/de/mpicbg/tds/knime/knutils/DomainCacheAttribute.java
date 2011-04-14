package de.mpicbg.tds.knime.knutils;

import org.knime.core.data.*;

import java.util.HashSet;
import java.util.Set;


/**
 * An atttribute implementation that allows for an automatic creation of a proper attribute domain.
 *
 * @author Holger Brandl
 */
public class DomainCacheAttribute extends Attribute {

    private Set<DataCell> domain = new HashSet<DataCell>();


    public DomainCacheAttribute(String attributeName, DataType type) {
        super(attributeName, type);
    }


    public DataColumnSpec getColumnSpec() {
        DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(getName(), getType());

        if (!domain.isEmpty()) {
            DataColumnDomain dataColumnDomain = new DataColumnDomainCreator(domain).createDomain();
            columnSpecCreator.setDomain(dataColumnDomain);
        }

        return columnSpecCreator.createSpec();
    }


    @Override
    protected DataCell postProcessCreatedCell(DataCell cell) {
        domain.add(cell);

        return cell;
    }
}
