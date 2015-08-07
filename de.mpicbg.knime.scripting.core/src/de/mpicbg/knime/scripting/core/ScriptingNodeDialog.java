package de.mpicbg.knime.scripting.core;

import static de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel.createSnippetProperty;
import static de.mpicbg.knime.scripting.core.AbstractScriptingNodeModel.createTemplateProperty;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplate;
import de.mpicbg.knime.scripting.core.rgg.wizard.ScriptTemplateWizard;
import de.mpicbg.knime.scripting.core.rgg.wizard.UseTemplateListenerImpl;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class ScriptingNodeDialog extends DefaultNodeSettingsPane {

    public static final String SCRIPT_TAB_NAME = "Script Editor";

    private static final NodeLogger logger = NodeLogger.getLogger(ScriptingNodeDialog.class);

    private JTable table;
    private int outTableDefCounter = 1;
    private JCheckBox m_appendColsCB;
    private JCheckBox m_openIn;

    private String defaultScript;
    private List<String> urlList;

    // the two main user interface elements: the template editor/configurator and the template repository browser
    private ScriptProvider scriptProvider;
    public ScriptTemplateWizard templateWizard;


    public static final String SCRIPT_PROPERTY = "node.script";
    public static String SCRIPT_TEMPLATE = "node.template";

    public static final String SCRIPT_TEMPLATE_DEFAULT = "";

	public static final String OPEN_IN = "open.in";
	public static final boolean OPEN_IN_DFT = false;

    /**
     * Will be set by tempaltes that are deployed as acutal knime nodes.
     */
    private ScriptTemplate hardwiredTemplate;


    /**
     * New pane for configuring ScriptedNode node dialog
     */
    public ScriptingNodeDialog(String defaultScript, ColNameReformater colNameReformater, boolean enableTemplateRepository) {
        this.defaultScript = defaultScript;

        // construct the panel for script loading/authoring
        scriptProvider = new ScriptProvider(colNameReformater, isReconfigurable());
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        
        JPanel scriptDialogContainer = new JPanel(new BorderLayout());
        scriptDialogContainer.add(scriptProvider, BorderLayout.CENTER);
        
        m_openIn = new JCheckBox("Open external");
        mainContainer.add(m_openIn, BorderLayout.NORTH);
        
        mainContainer.add(scriptDialogContainer, BorderLayout.CENTER);
        
        this.addTabAt(0, SCRIPT_TAB_NAME, mainContainer);

        // create the template repository browser tab (if enabled)
        if (enableTemplateRepository) {

            updateUrlList(getTemplatesFromPreferences());
            List<ScriptTemplate> templates = updateTemplates();

            //templateWizard = new ScriptTemplateWizard(templateResources);
            templateWizard = new ScriptTemplateWizard(this, templates);
            templateWizard.addUseTemplateListener(new UseTemplateListenerImpl(this));
            this.addTabAt(1, "Templates", templateWizard);
        }

        removeTab("Options");
        selectTab(SCRIPT_TAB_NAME);
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

        String serializedTemplate = settings.getString(SCRIPT_TEMPLATE, SCRIPT_TEMPLATE_DEFAULT);


        ScriptTemplate template = null;
        if (getHardwiredTemplate() != null && serializedTemplate.equals(SCRIPT_TEMPLATE_DEFAULT)) {
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
        final String script = settings.getString(SCRIPT_PROPERTY, defaultScript);

        // update the ui but not from within the application thread
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {

        try {
            scriptProvider.updateInputModel(specs);
        } catch (NotConfigurableException e) {
            e.printStackTrace();
        }

        scriptProvider.setContent(script, finalTemplate);
        
        boolean openIn = false;
        try {
			openIn = settings.getBoolean(OPEN_IN);
		} catch (InvalidSettingsException e) {
		}

        m_openIn.setSelected(openIn);
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

        return (ScriptTemplate) new XStream(new DomDriver()).fromXML(serializedTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);

        SettingsModelString scriptProperty = createSnippetProperty(defaultScript);
        scriptProperty.setStringValue(scriptProvider.getScript());
        scriptProperty.saveSettingsTo(settings);

//        settings.addString(SCRIPT_PROPERTY, scriptProvider.getScript());

        ScriptTemplate template = scriptProvider.getTemplate();
        SettingsModelString templateProperty = createTemplateProperty();

        if (template != null) {

            // save the state of the configuration dialog if there's a template and it is linked to the script
            if (template.isLinkedToScript() && template.isRGG()) {
                HashMap<String, Object> persistedUIConfig = scriptProvider.getTemplateConfigurator().persisteTemplateUIState();
                template.setPersistedConfig(persistedUIConfig);
            }

            // save the template as xml (with is kinda hacky) but makes live much easier here
            templateProperty.setStringValue(new XStream().toXML(scriptProvider.getTemplate()));

        } else {
            templateProperty.setStringValue("");
        }

        templateProperty.saveSettingsTo(settings);
        
        SettingsModelBoolean openInProperty = AbstractScriptingNodeModel.createOpenInProperty();
        openInProperty.setBooleanValue(m_openIn.isSelected());
        openInProperty.saveSettingsTo(settings);
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

    public abstract String getTemplatesFromPreferences();

    public List<ScriptTemplate> updateTemplates() {
        TemplateCache templateCache = TemplateCache.getInstance();

        List<ScriptTemplate> templates = new ArrayList<ScriptTemplate>();
        List<String> warnings = new ArrayList<String>();

        for (String filePath : urlList) {
            try {
                templates.addAll(templateCache.updateTemplateCache(filePath));
            } catch (IOException e) {
                warnings.add(e.getMessage());
            }
        }

        // show warning if files are empty or could not be read
        if (!warnings.isEmpty()) {
            logger.warn(warnings);
        }

        return templates;
    }
}
