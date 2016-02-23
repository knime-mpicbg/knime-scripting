package de.mpicbg.knime.scripting.r;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactoryClassMapper;

import de.mpicbg.knime.scripting.r.node.snippet.RSnippetNodeFactory;

/**
 * class, to map the old factory class location to a new one
 * @author Antje Janosch
 *
 */
public class RNodeFactoryClassMapper extends NodeFactoryClassMapper {

	@Override
	public NodeFactory<? extends AbstractRScriptingNodeModel> mapFactoryClassName(String factoryClassName) {
		
		if(factoryClassName.equals("de.mpicbg.knime.scripting.r.RSnippetNodeFactory"))
			return new RSnippetNodeFactory();
		return null;
	}

	/**
	 * required constructor
	 */
	public RNodeFactoryClassMapper() {
		super();
	};
}
