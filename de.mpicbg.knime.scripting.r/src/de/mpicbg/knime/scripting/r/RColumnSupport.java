package de.mpicbg.knime.scripting.r;

import de.mpicbg.knime.scripting.core.ColumnSupport;
import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeModel;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;

/**
 * Defines the way data items are rendered when the user selects them from the variable list while writing a template.
 * Defines which column types are supported
 * 
 * @author Antje Janosch
 *
 */
public class RColumnSupport implements ColumnSupport {

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String reformat(String name, DataType type, boolean altDown) {
        if (altDown) {
            if (name.contains(" ")) {
                return RSnippetNodeModel.R_INVAR_BASE_NAME + "$\"" + name + "\"";
            } else {
                return RSnippetNodeModel.R_INVAR_BASE_NAME + "$" + name + "";
            }

        } else {
            return "\"" + name + "\"";
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSupported(DataType dtype) {
		if(dtype.isCompatible(DoubleValue.class)) return true;
		if(dtype.isCompatible(BooleanValue.class)) return true;
		if(dtype.isCompatible(IntValue.class)) return true;
		if(dtype.isCompatible(StringValue.class)) return true;
		return false;
	}
}
