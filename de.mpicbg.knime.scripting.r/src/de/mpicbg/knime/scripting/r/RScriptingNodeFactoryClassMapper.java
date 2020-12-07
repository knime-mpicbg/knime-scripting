package de.mpicbg.knime.scripting.r;

import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.MapNodeFactoryClassMapper;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;

import de.mpicbg.knime.scripting.r.generic.ConvertToGenericRFactory;
import de.mpicbg.knime.scripting.r.generic.ConvertToTableFactory;
import de.mpicbg.knime.scripting.r.generic.GenericOpenInRNodeFactory;
import de.mpicbg.knime.scripting.r.generic.GenericRSnippetFactory;
import de.mpicbg.knime.scripting.r.generic.GenericRSnippetSourceFactory;
import de.mpicbg.knime.scripting.r.misc.RPlotWithImPortNodeFactory;
import de.mpicbg.knime.scripting.r.misc.ScatterPlotGridFactory;
import de.mpicbg.knime.scripting.r.node.plot.RPlotNodeFactory;
import de.mpicbg.knime.scripting.r.node.snippet21.RSnippetNodeFactory21;

/**
 * Due to relocating node classes, it's necessary to define a map with the old locations mapping to the new ones
 * 
 * @author Antje Janosch
 *
 */
public class RScriptingNodeFactoryClassMapper extends MapNodeFactoryClassMapper {
	
	@Override
    protected Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getMapInternal() {
        final Map<String, Class<? extends NodeFactory<? extends NodeModel>>> map = new HashMap<>();
        // set the mappings 
        map.put("de.mpicbg.knime.scripting.r.RPlotNodeFactory", RPlotNodeFactory.class);
        map.put("de.mpicbg.knime.scripting.r.RSnippetNodeFactory", RSnippetNodeFactory.class);
        map.put("de.mpicbg.knime.scripting.r.RSnippetNodeFactory21", RSnippetNodeFactory21.class);
        
        map.put("de.mpicbg.knime.scripting.r.OpenInRNodeFactory", OpenInRNodeFactory.class);
        map.put("de.mpicbg.knime.scripting.r.generic.ConvertToGenericRFactory", ConvertToGenericRFactory.class);
        map.put("de.mpicbg.knime.scripting.r.generic.GenericRSnippetFactory", GenericRSnippetFactory.class);
        map.put("de.mpicbg.knime.scripting.r.generic.GenericRSnippetSourceFactory", GenericRSnippetSourceFactory.class);
        map.put("de.mpicbg.tds.knime.scripting.r.RPlotWithImPortNodeFactory", RPlotWithImPortNodeFactory.class);
        map.put("de.mpicbg.tds.knime.scripting.r.templatenodes.rgg.ScatterPlotGridFactory", ScatterPlotGridFactory.class);
        map.put("de.mpicbg.knime.scripting.r.generic.ConvertToTableFactory", ConvertToTableFactory.class);
        map.put("de.mpicbg.knime.scripting.r.generic.GenericOpenInRNodeFactory", GenericOpenInRNodeFactory.class);
        
        return map;
    }

}
