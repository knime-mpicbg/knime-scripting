package de.mpicbg.knime.scripting.core;

import static de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel.createSnippetProperty;
import static de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel.createTemplateProperty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplateWizard;
import de.mpicbg.knime.scripting.core.rgg.wizard.UseTemplateListenerImpl;
import de.mpicbg.knime.scripting.core.utils.ScriptingUtils;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class ScriptingNodeDialog extends DefaultNodeSettingsPane {
	
	private boolean m_provideScriptTab = true;
	private boolean m_provideOptionsTab = true;
	private boolean m_provideOpenInOption = true;
	private boolean m_enableTemplateRepository = true;

    public static final String SCRIPT_TAB_NAME = "Script Editor";
    private static final String OPTIONS_TAB_NAME = "Chunk Settings";
    private static final String TEMPLATES_TAB_NAME = "Templates";

    private static final NodeLogger logger = NodeLogger.getLogger(ScriptingNodeDialog.class);

    // checkbox for "open external ..."
    private JCheckBox m_openIn;
    
    // models for chunk settings
    SpinnerNumberModel m_spinnerChunkIn;
    SpinnerNumberModel m_spinnerChunkOut;
    
    private String defaultScript;
    private List<String> urlList;
    
    private Path cacheFolder;
    private Path indexFile;

    // the two main user interface elements: the template editor/configurator and the template repository browser
    private ScriptProvider scriptProvider;
    public ScriptTemplateWizard templateWizard;
	

    /**
     * Will be set by templates that are deployed as actual knime nodes.
     */
    private ScriptTemplate hardwiredTemplate;
	


    /**
     * New pane for configuring ScriptedNode node dialog
     */
    public ScriptingNodeDialog(String defaultScript, ColumnSupport colSupport, boolean enableTemplateRepository) {
        this(defaultScript, colSupport, enableTemplateRepository, true, true);
    }

    /**
     * Scripting Dialog
     * @param defaultScript
     * @param colNameReformater
     * @param enableTemplateRepository - true if the Tab with the Template-Repository should be displayed
     * @param useOpenExternal - true, if the checkbox "open external" should be enabled
     * @param useChunkSettings - true, if chunk settings should be available
     */
    public ScriptingNodeDialog(String defaultScript,
    		ColumnSupport colSupport,
			boolean enableTemplateRepository, 
			boolean useOpenExternal,
			boolean useChunkSettings) {
    	// set default script
    	this.defaultScript = defaultScript;
        // construct the panel for script loading/authoring
        scriptProvider = new ScriptProvider(colSupport, isReconfigurable());       
        this.m_provideOpenInOption = useOpenExternal;
        this.m_enableTemplateRepository = enableTemplateRepository;
        this.m_provideOptionsTab = useChunkSettings;
        
        initComponents(SCRIPT_TAB_NAME);
	}
    
    /**
     * constructor, only providing the chunk settings
     * used by "Open In ..."
     */
    public ScriptingNodeDialog() {
    	
    	this.m_provideScriptTab = false;
    	this.m_enableTemplateRepository = false;
    	this.m_provideOpenInOption = false;
    	
    	initComponents(OPTIONS_TAB_NAME);
    }
    
    private void initComponents(String selectTab) {
    	// create scripting tab
    	if(m_provideScriptTab) {
    		
            // create the template repository browser tab (if enabled)
            if (m_enableTemplateRepository) {

                updateUrlList(getTemplatesFromPreferences());
                this.cacheFolder = getTemplateCachePath();
                this.indexFile = Paths.get(cacheFolder.toString(), ScriptingUtils.LOCAL_CACHE_INDEX);
                //List<ScriptTemplate> templates = updateTemplates();
                List<ScriptTemplate> templates = retrieveTemplates();

                //templateWizard = new ScriptTemplateWizard(templateResources);
                templateWizard = new ScriptTemplateWizard(this, templates);
                templateWizard.addUseTemplateListener(new UseTemplateListenerImpl(this));
                this.addTab(TEMPLATES_TAB_NAME, templateWizard);
                selectTab = TEMPLATES_TAB_NAME;
            }
            
            // Create scripting tab
	    	JPanel mainContainer = new JPanel(new BorderLayout());
	        
	        JPanel scriptDialogContainer = new JPanel(new BorderLayout());
	        scriptDialogContainer.add(scriptProvider, BorderLayout.CENTER);
	        
	        m_openIn = new JCheckBox("Open external");
	        m_openIn.addActionListener(new ActionListener() {
	
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					AbstractButton abstractButton = (AbstractButton) actionEvent.getSource();
			        boolean selected = abstractButton.getModel().isSelected();
			        paintBorder(selected);
				}
	        	
	        });
	        m_openIn.setBorderPainted(true);
	        m_openIn.setEnabled(m_provideOpenInOption);
	        mainContainer.add(m_openIn, BorderLayout.NORTH);
	        
	        mainContainer.add(scriptDialogContainer, BorderLayout.CENTER);
	        
	        this.addTab(SCRIPT_TAB_NAME, mainContainer);
    	}
        
        // chunk settings panel
        if(m_provideOptionsTab) {
	        JPanel optionsPanel = new JPanel();
	        populateOptionsPanel(optionsPanel);
	        this.addTab(OPTIONS_TAB_NAME, optionsPanel);
        }
        
        removeTab("Options");
        selectTab(selectTab);
    }

	private List<ScriptTemplate> retrieveTemplates() {
    	assert cacheFolder != null;
    	assert indexFile != null;
    	
        TemplateCache templateCache = TemplateCache.getInstance();

        List<ScriptTemplate> templates = new ArrayList<ScriptTemplate>();
        List<String> warnings = new ArrayList<String>();

        for (String filePath : urlList) {
        	if(templateCache.contains(filePath))
        		templates.addAll(templateCache.getTemplates(filePath));
        	else
        		warnings.add("No templates for " + filePath + " have been loaded");
/*        	else {
        		templateCache.addTemplateFile(filePath, bundlePath, indexFile);
        	}*/
            /*try {
            	templateCache.updateTemplateCache(filePath, cacheFolder, indexFile);
                templates.addAll(templateCache.getTemplates(filePath));
            } catch (IOException e) {
                warnings.add(e.getMessage());
            }*/
        }

        // show warning if files are empty or could not be read
        if (!warnings.isEmpty()) {
            logger.warn(warnings);
        }

        return templates;
	}

	private void populateOptionsPanel(JPanel optionsPanel) {
		
		// init components
		JLabel label1 = new JLabel("Chunk size to push incoming data (tables only):");
        JLabel label2 = new JLabel("Chunk size to pull result data (tables only):");
        m_spinnerChunkIn = new SpinnerNumberModel(AbstractScriptingNodeModel.CHUNK_IN_DFT, -1, Integer.MAX_VALUE, 1);
        m_spinnerChunkOut = new SpinnerNumberModel(AbstractScriptingNodeModel.CHUNK_OUT_DFT, -1, Integer.MAX_VALUE, 1);
        JSpinner spinner1 = new JSpinner(m_spinnerChunkIn);
        JSpinner spinner2 = new JSpinner(m_spinnerChunkOut);
        JButton resetButton = new JButton(""
        		+ "<html>"
        		+ "Reset settings"
        		+ "<br />"
        		+ "(Do not chunk data for transfer)"
        		+ "</html>");
        resetButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				m_spinnerChunkIn.setValue(AbstractScriptingNodeModel.CHUNK_IN_DFT);
				m_spinnerChunkOut.setValue(AbstractScriptingNodeModel.CHUNK_OUT_DFT);
			}
		});
        
        // put components into a grid
        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0,0,0,10);
        
        c.gridx = 0;
        c.gridy = 0;
        gridPanel.add(label1, c);
        c.gridx = 0;
        c.gridy = 1;
        gridPanel.add(label2, c);
        c.gridx = 1;
        c.gridy = 0;
        gridPanel.add(spinner1, c);
        c.gridx = 1;
        c.gridy = 1;
        gridPanel.add(spinner2, c);
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(25, 0, 0, 10);
        gridPanel.add(resetButton,c);
        
        optionsPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        optionsPanel.add(gridPanel);
	}

	/**
     * parses a preference string and fills list of URLs
     *
     * @param templatesFromPreferences
     */
    public void updateUrlList(String templatesFromPreferences) {
        TemplateCache templateCache = TemplateCache.getInstance();
        urlList = templateCache.parseConcatendatedURLs(templatesFromPreferences);
    }


    public void useTemplate(ScriptTemplate template) {
        selectScriptTab();
        scriptProvider.setContent(template.getTemplate(), template);
    }


    /**
     * @return <code>true</code> if the node-template should be acessible to the user. False if not.
     */
    protected boolean isReconfigurable() {
        return true;
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
                                           final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        
        if(m_provideScriptTab)
        	loadScriptSettings(settings, specs);

        if(m_provideOpenInOption) {
        	boolean openIn = false;
        	try {
    			openIn = settings.getBoolean(AbstractScriptingNodeModel.OPEN_IN);
        	} catch (InvalidSettingsException e) {}
        	m_openIn.setSelected(openIn);
        	paintBorder(openIn);
        }
        
        if(m_provideOptionsTab) {
	        int chunkIn = AbstractScriptingNodeModel.CHUNK_IN_DFT;
	        int chunkOut = AbstractScriptingNodeModel.CHUNK_OUT_DFT;
	        try {
				chunkIn = settings.getInt(AbstractScriptingNodeModel.CHUNK_IN);
				chunkOut = settings.getInt(AbstractScriptingNodeModel.CHUNK_OUT);
			} catch (InvalidSettingsException e) {}

	        m_spinnerChunkIn.setValue(chunkIn);
	        m_spinnerChunkOut.setValue(chunkOut);
        }
    }

	private void loadScriptSettings(NodeSettingsRO settings, PortObjectSpec[] specs) {
    	String serializedTemplate = settings.getString(AbstractScriptingNodeModel.SCRIPT_TEMPLATE, AbstractScriptingNodeModel.SCRIPT_TEMPLATE_DEFAULT);

        ScriptTemplate template = null;
        if (getHardwiredTemplate() != null && serializedTemplate.equals(AbstractScriptingNodeModel.SCRIPT_TEMPLATE_DEFAULT)) {
            template = getHardwiredTemplate();

        } else {
            if (StringUtils.isNotBlank(serializedTemplate)) {
                template = deserializeTemplate(serializedTemplate);
            }
        }

        if (getHardwiredTemplate() != null) {
            ScriptTemplate predefTemplate = getHardwiredTemplate();
            if (predefTemplate == null) {
                throw new RuntimeException("Hardwired nodes require a predefined template");
            }

            if (template != null) {
                predefTemplate.setPersistedConfig(template.getPersistedConfig());
                predefTemplate.setLinkedToScript(template.isLinkedToScript());
            }

            // use the hardwired template instead of the persisted one
            template = predefTemplate;
        }


        final ScriptTemplate finalTemplate = template;
        final String script = settings.getString(AbstractScriptingNodeModel.SCRIPT_PROPERTY, defaultScript);

        try {
            scriptProvider.updateInputModel(specs);
        } catch (NotConfigurableException e) {
            e.printStackTrace();
        }

        // if a template/script has been loaded: select the script tab
        // as the default script will be present for new nodes it only needs to be done in case
        // the script differs from the default script
       if(template != null || !script.equals(defaultScript)) selectTab(SCRIPT_TAB_NAME);
       
        scriptProvider.setContent(script, finalTemplate);
	}

	/**
     * border of the "open external" checkbox should be red if selected, none otherwise
     * @param openIn
     */
    private void paintBorder(boolean openIn) {
        if(openIn)
        	m_openIn.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.RED));
        else
        	m_openIn.setBorder(BorderFactory.createEmptyBorder());
	}

	public static ScriptTemplate deserializeTemplate(String serializedTemplate) {
        if (serializedTemplate == null || StringUtils.isBlank(serializedTemplate)) {
            return null;
        }
        
        // class ScriptTemplate has been relocated. To successful load templates saved before, check whether given class can be loaded
        // look for something like "<de.mpicbg.XXXX.ScriptTemplate"
        String searchPattern = "de\\.mpicbg\\..*\\.ScriptTemplate";
        Pattern p = Pattern.compile("<(" + searchPattern +")");
        Matcher m = p.matcher(serializedTemplate);
        if(m.find()) {
            String classLocation = m.group(1);
            try {
            	Class.forName(classLocation);
            } catch( ClassNotFoundException e ) {
            	serializedTemplate = serializedTemplate.replaceAll(searchPattern, ScriptTemplate.class.getCanonicalName());
            }
        }

        return ScriptTemplate.fromXML(serializedTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        
        if(m_provideScriptTab) {
	        SettingsModelString scriptProperty = createSnippetProperty(defaultScript);
	        scriptProperty.setStringValue(scriptProvider.getScript());
	        scriptProperty.saveSettingsTo(settings);
	        
	        ScriptTemplate template = scriptProvider.getTemplate();
	        SettingsModelString templateProperty = createTemplateProperty();

	        if (template != null) {

	            // save the state of the configuration dialog if there's a template and it is linked to the script
	            if (template.isLinkedToScript() && template.isRGG()) {
	                HashMap<String, Object> persistedUIConfig = scriptProvider.getTemplateConfigurator().persisteTemplateUIState();
	                template.setPersistedConfig(persistedUIConfig);
	            }

	            // save the template as xml (with is kinda hacky) but makes live much easier here
	            templateProperty.setStringValue(scriptProvider.getTemplate().toXML());

	        } else {
	            templateProperty.setStringValue("");
	        }
	        
	        templateProperty.saveSettingsTo(settings);
        }
    
        if(m_provideOpenInOption) {
	        SettingsModelBoolean openInProperty = AbstractScriptingNodeModel.createOpenInProperty();
	        openInProperty.setBooleanValue(m_openIn.isSelected());
	        openInProperty.saveSettingsTo(settings);
        }
        
        if(m_provideOptionsTab) {
	        SettingsModelIntegerBounded smChunkIn = AbstractScriptingNodeModel.createChunkInProperty();
	        smChunkIn.setIntValue(m_spinnerChunkIn.getNumber().intValue());
	        smChunkIn.saveSettingsTo(settings);
	        
	        SettingsModelIntegerBounded smChunkOut = AbstractScriptingNodeModel.createChunkOutProperty();
	        smChunkOut.setIntValue(m_spinnerChunkOut.getNumber().intValue());
	        smChunkOut.saveSettingsTo(settings);
        }
    }

    public void selectScriptTab() {
        selectTab(SCRIPT_TAB_NAME);
    }


    public ScriptTemplate getHardwiredTemplate() {
        return hardwiredTemplate;
    }


    public void setHardwiredTemplate(ScriptTemplate predefinedTemplate) {
        scriptProvider.getTemplateConfigurator().enableConfigureTemplateButton(false);
        this.hardwiredTemplate = predefinedTemplate;
    }

    /**
     * retrieve string value from preference settings 'templates'
     * @return
     */
    public abstract String getTemplatesFromPreferences();
    
    /**
     * @return path of the bundle within workspace/.metadata/plugins/
     */
	protected abstract Path getTemplateCachePath();

    public List<ScriptTemplate> updateTemplates() {
    	
    	assert cacheFolder != null;
    	assert indexFile != null;
    	
        TemplateCache templateCache = TemplateCache.getInstance();

        List<ScriptTemplate> templates = new ArrayList<ScriptTemplate>();
        List<String> warnings = new ArrayList<String>();

        // for each URL try to reload the file
        // if reloading fails due to missing access, leave the template cache untouched
        for (String filePath : urlList) {
            try {
            	templateCache.updateTemplateCache(filePath, cacheFolder, indexFile);
            } catch (IOException e) {
                warnings.add(e.getMessage());
            } finally {
            	templates.addAll(templateCache.getTemplates(filePath));
            }
        }

        // show warning if files are empty or could not be read
        if (!warnings.isEmpty()) {
            logger.warn(warnings);
        }

        return templates;
    }
}
