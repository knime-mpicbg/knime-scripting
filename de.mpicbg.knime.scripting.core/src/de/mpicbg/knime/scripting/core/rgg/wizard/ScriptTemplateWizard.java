/*
 * Created by JFormDesigner on Sat Apr 17 17:46:04 CEST 2010
 */

package de.mpicbg.knime.scripting.core.rgg.wizard;

import de.mpicbg.knime.scripting.core.ScriptingNodeDialog;
import de.mpicbg.knime.scripting.core.TemplateCache;
import de.mpicbg.knime.scripting.core.utils.Template2Html;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * @author Holger Brandl
 */
public class ScriptTemplateWizard extends JSplitPane {

    public DefaultTreeModel categoryTreeModel;
    private ScriptTemplate curSelection;

    private List<UseTemplateListener> useTemplateListeners = new ArrayList<UseTemplateListener>();
    public List<ScriptTemplate> templates;

    private ScriptingNodeDialog parentDialog;

    public ScriptTemplateWizard(ScriptingNodeDialog parent, List<ScriptTemplate> templates) {
        this.templates = templates;
        this.parentDialog = parent;

        initComponents();

        categoryTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        categoryTree.setRootVisible(false);
        categoryTree.setModel(categoryTreeModel);

        repopulateTemplateTree(null);

        descContainerSplitPanel.setDividerLocation(0.0);

        // register the selection listener
        categoryTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent evt) {
                // Get all nodes whose selection status has changed

                TreePath selectionPath = categoryTree.getSelectionPath();
                if (selectionPath == null) { // this will be the case when the tree is beeing reloaded
                    setCurrentTemplate(null);
                    return;
                }

                Object selectedPathElement = selectionPath.getLastPathComponent();
                if (selectedPathElement == null) {
                    setCurrentTemplate(null);
                    return;
                }

                Object userObject = ((DefaultMutableTreeNode) selectedPathElement).getUserObject();

                if (userObject instanceof ScriptTemplate) {
                    ScriptTemplate scriptTemplate = (ScriptTemplate) userObject;
                    setCurrentTemplate(scriptTemplate);
                } else {
                    setCurrentTemplate(null);
                }
            }
        });
    }

    public static void main(String[] args) throws MalformedURLException {
        String templateFilePath = new String("https://raw.githubusercontent.com/knime-mpicbg/scripting-templates/master/knime-scripting-templates/R/figure-templates.txt");

        TemplateCache templateCache = TemplateCache.getInstance();

        List<URL> urlList = new ArrayList<URL>();
        urlList.add(new URL(templateFilePath));
        
        try {
        	Path dummyBundlePath = Files.createTempDirectory("test_");
        	Path dummyIdxFile = Files.createTempFile(dummyBundlePath, "", "");
            templateCache.addTemplateFile(templateFilePath, dummyBundlePath, dummyIdxFile);
            List<ScriptTemplate> templates = templateCache.getTemplates(templateFilePath);

            ScriptTemplateWizard templateWizard = new ScriptTemplateWizard(null, templates);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.getContentPane().add(templateWizard);

            frame.setSize(new Dimension(800, 700));
            frame.setVisible(true);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void repopulateTemplateTree(String searchTerm) {
        DefaultMutableTreeNode mutableTreeRoot = (DefaultMutableTreeNode) categoryTreeModel.getRoot();
        mutableTreeRoot.removeAllChildren();

        searchTerm = searchTerm == null ? null : searchTerm.toLowerCase();

        // populate the tree
        for (ScriptTemplate scriptTemplate : templates) {
            if (scriptTemplate == null)
                continue;

            if (searchTerm != null) {
                String description = scriptTemplate.getDescription().toLowerCase();
                String name = scriptTemplate.getName().toLowerCase();
                if ((description == null || !description.contains(searchTerm)) && (name == null || !name.contains(searchTerm))) {
                    continue;
                }
            }

            DefaultMutableTreeNode parent = mutableTreeRoot;
            for (String category : scriptTemplate.getCategories()) {
                parent = getOrCreateParentNode(parent, category);
                //parent.add(new DefaultMutableTreeNode(scriptTemplate));
            }
            parent.add(new DefaultMutableTreeNode(scriptTemplate));
        }

        categoryTreeModel.nodeStructureChanged((TreeNode) mutableTreeRoot);

        if (searchTerm != null) {
            expandAll(categoryTree, true);
        }

        categoryTree.invalidate();
    }

// If expand is true, expands all nodes in the tree.
// Otherwise, collapses all nodes in the tree.


    public void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();

        // Traverse tree from root
        expandAll(tree, new TreePath(root), expand);
    }


    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }


    public void addUseTemplateListener(UseTemplateListener listener) {
        if (!useTemplateListeners.contains(listener))
            useTemplateListeners.add(listener);
    }


    public List<UseTemplateListener> getUseTemplateListeners() {
        return useTemplateListeners;
    }


    private void setCurrentTemplate(ScriptTemplate scriptTemplate) {
        curSelection = scriptTemplate;

        descContainerSplitPanel.setDividerLocation(1.0);

        if (scriptTemplate != null) {
            authorLabel.setText(scriptTemplate.getAuthor());
            descriptionArea.setText(scriptTemplate.getDescription());
            templateArea.setText(scriptTemplate.getTemplate());
            hasUICheckBox.setSelected(scriptTemplate.isRGG());

            // load and hook in the preview image if possible
            if (scriptTemplate.getPreviewURL() != null) {
                try {
                    final BufferedImage image = ImageIO.read(new URL(scriptTemplate.getPreviewURL()));
                    previewImagePanel.setImage(image);
                    previewImagePanel.setTitle(scriptTemplate.getName());
                    descContainerSplitPanel.setDividerLocation(0.5);

                } catch (IOException e) {
                    //throw new RuntimeException(e);
                    previewImagePanel.setImage(null);
                }

            } else {
                previewImagePanel.setImage(null);
            }

        } else {
            authorLabel.setText("");
            descriptionArea.setText("");
            templateArea.setText("");
            hasUICheckBox.setSelected(false);
            previewImagePanel.setImage(null);
        }

        descriptionPanel.invalidate();

    }


    private DefaultMutableTreeNode getOrCreateParentNode(DefaultMutableTreeNode parent, String category) {
        
    	// is category already available?
		Enumeration<?> enumeration = parent.children();
    	while (enumeration.hasMoreElements()) {
    		DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
            if (childNode.getUserObject().equals(category)) return childNode;
    	}
    	
    	// category is not yet available
    	// add category
    	DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category);
    	parent.add(categoryNode);
    	return categoryNode;
    }

    private void useTemplateActionPerformed() {
        if (curSelection != null) {
            for (UseTemplateListener useTemplateListener : useTemplateListeners) {
                useTemplateListener.useTemplate(curSelection);
            }
        }
    }


    private void categoryTreeMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            useTemplateActionPerformed();
        }
    }


    private void searchTemplatesTextFieldActionPerformed() {
        String searchTerm = searchTemplatesTextField.getText();
        if (searchTerm.isEmpty()) {
            searchTerm = null;
        }

        repopulateTemplateTree(searchTerm);
    }

    /**
     * generates a html-file which provides a template script gallery
     */
    private void galleryButtonActionPerformed() {

        try {
            File galleryFile = Template2Html.exportToHtmlFile(templates);

            Desktop.getDesktop().edit(galleryFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Button "Refresh" pressed: Refresh templates from Preference settings
     *
     * @param e
     */
    private void refreshButtonActionPerformed(ActionEvent e) {
        parentDialog.updateUrlList(parentDialog.getTemplatesFromPreferences());
        templates = parentDialog.updateTemplates();
        repopulateTemplateTree(null);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        panel1 = new JPanel();
        panel7 = new JPanel();
        scrollPane1 = new JScrollPane();
        categoryTree = new JTree();
        searchPanel = new JPanel();
        label2 = new JLabel();
        searchTemplatesTextField = new JTextField();
        panel5 = new JPanel();
        refreshButton = new JButton();
        galleryButton = new JButton();
        templateDetailsPanel = new JPanel();
        useTemplate = new JButton();
        panel4 = new JPanel();
        tabbedPane1 = new JTabbedPane();
        descriptionPanel = new JPanel();
        panel2 = new JPanel();
        label1 = new JLabel();
        authorLabel = new JLabel();
        panel3 = new JPanel();
        checkBox1 = new JLabel();
        hasUICheckBox = new JCheckBox();
        tempDescContainer = new JPanel();
        descContainerSplitPanel = new JSplitPane();
        scrollPane3 = new JScrollPane();
        descriptionArea = new JTextArea();
        previewPanel = new JPanel();
        previewImagePanel = new PreviewImagePanel();
        scriptPanel = new JPanel();
        scrollPane2 = new JScrollPane();
        templateArea = new JTextArea();

        //======== this ========
        setDividerSize(2);
        setDividerLocation(180);

        //======== panel1 ========
        {
            panel1.setPreferredSize(new Dimension(250, 437));
            panel1.setLayout(new BorderLayout());

            //======== panel7 ========
            {
                panel7.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {
                    scrollPane1.setBorder(new TitledBorder(null, "Template Categories", TitledBorder.LEADING, TitledBorder.TOP));

                    //---- categoryTree ----
                    categoryTree.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            categoryTreeMouseClicked(e);
                        }
                    });
                    scrollPane1.setViewportView(categoryTree);
                }
                panel7.add(scrollPane1, BorderLayout.CENTER);

                //======== searchPanel ========
                {
                    searchPanel.setLayout(new BorderLayout());

                    //---- label2 ----
                    label2.setText("Search : ");
                    searchPanel.add(label2, BorderLayout.WEST);

                    //---- searchTemplatesTextField ----
                    searchTemplatesTextField.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            searchTemplatesTextFieldActionPerformed();
                        }
                    });
                    searchPanel.add(searchTemplatesTextField, BorderLayout.CENTER);
                }
                panel7.add(searchPanel, BorderLayout.SOUTH);
            }
            panel1.add(panel7, BorderLayout.CENTER);

            //======== panel5 ========
            {
                panel5.setLayout(new BorderLayout());

                //---- refreshButton ----
                refreshButton.setText("Refresh");
                refreshButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        refreshButtonActionPerformed(e);
                    }
                });
                panel5.add(refreshButton, BorderLayout.CENTER);

                //---- galleryButton ----
                galleryButton.setText("Gallery");
                galleryButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        galleryButtonActionPerformed();
                    }
                });
                panel5.add(galleryButton, BorderLayout.EAST);
            }
            panel1.add(panel5, BorderLayout.SOUTH);
        }
        setLeftComponent(panel1);

        //======== templateDetailsPanel ========
        {
            templateDetailsPanel.setLayout(new BorderLayout());

            //---- useTemplate ----
            useTemplate.setText("Use this template");
            useTemplate.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    useTemplateActionPerformed();
                }
            });
            templateDetailsPanel.add(useTemplate, BorderLayout.SOUTH);

            //======== panel4 ========
            {
                panel4.setPreferredSize(new Dimension(110, 180));
                panel4.setLayout(new BorderLayout());

                //======== tabbedPane1 ========
                {

                    //======== descriptionPanel ========
                    {
                        descriptionPanel.setMinimumSize(new Dimension(0, 100));
                        descriptionPanel.setPreferredSize(new Dimension(0, 100));
                        descriptionPanel.setBorder(null);
                        descriptionPanel.setLayout(new BorderLayout());

                        //======== panel2 ========
                        {
                            panel2.setLayout(new BorderLayout());

                            //---- label1 ----
                            label1.setText("Author :  ");
                            label1.setLabelFor(descriptionArea);
                            panel2.add(label1, BorderLayout.WEST);
                            panel2.add(authorLabel, BorderLayout.CENTER);

                            //======== panel3 ========
                            {
                                panel3.setLayout(new BorderLayout());

                                //---- checkBox1 ----
                                checkBox1.setText("Provides User-Interace :");
                                panel3.add(checkBox1, BorderLayout.CENTER);

                                //---- hasUICheckBox ----
                                hasUICheckBox.setEnabled(false);
                                panel3.add(hasUICheckBox, BorderLayout.EAST);
                            }
                            panel2.add(panel3, BorderLayout.SOUTH);
                        }
                        descriptionPanel.add(panel2, BorderLayout.SOUTH);

                        //======== tempDescContainer ========
                        {
                            tempDescContainer.setLayout(new BorderLayout());

                            //======== descContainerSplitPanel ========
                            {
                                descContainerSplitPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
                                descContainerSplitPanel.setDividerSize(4);
                                descContainerSplitPanel.setDividerLocation(150);

                                //======== scrollPane3 ========
                                {
                                    scrollPane3.setBorder(null);

                                    //---- descriptionArea ----
                                    descriptionArea.setEditable(false);
                                    descriptionArea.setFocusable(false);
                                    descriptionArea.setEnabled(false);
                                    descriptionArea.setTabSize(4);
                                    descriptionArea.setWrapStyleWord(true);
                                    descriptionArea.setAutoscrolls(false);
                                    descriptionArea.setLineWrap(true);
                                    descriptionArea.setDisabledTextColor(Color.black);
                                    descriptionArea.setBackground(SystemColor.window);
                                    scrollPane3.setViewportView(descriptionArea);
                                }
                                descContainerSplitPanel.setTopComponent(scrollPane3);

                                //======== previewPanel ========
                                {
                                    previewPanel.setBorder(new TitledBorder(null, "Preview (Double-click to enlarge)", TitledBorder.LEADING, TitledBorder.TOP));
                                    previewPanel.setLayout(new BorderLayout());
                                    previewPanel.add(previewImagePanel, BorderLayout.CENTER);
                                }
                                descContainerSplitPanel.setBottomComponent(previewPanel);
                            }
                            tempDescContainer.add(descContainerSplitPanel, BorderLayout.CENTER);
                        }
                        descriptionPanel.add(tempDescContainer, BorderLayout.CENTER);
                    }
                    tabbedPane1.addTab("Description", descriptionPanel);


                    //======== scriptPanel ========
                    {
                        scriptPanel.setBorder(null);
                        scriptPanel.setMinimumSize(null);
                        scriptPanel.setLayout(new BorderLayout());

                        //======== scrollPane2 ========
                        {
                            scrollPane2.setMinimumSize(null);

                            //---- templateArea ----
                            templateArea.setEditable(false);
                            templateArea.setTabSize(4);
                            templateArea.setWrapStyleWord(true);
                            templateArea.setLineWrap(true);
                            templateArea.setMinimumSize(null);
                            scrollPane2.setViewportView(templateArea);
                        }
                        scriptPanel.add(scrollPane2, BorderLayout.CENTER);
                    }
                    tabbedPane1.addTab("Source", scriptPanel);

                }
                panel4.add(tabbedPane1, BorderLayout.CENTER);
            }
            templateDetailsPanel.add(panel4, BorderLayout.CENTER);
        }
        setRightComponent(templateDetailsPanel);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel panel1;
    private JPanel panel7;
    private JScrollPane scrollPane1;
    private JTree categoryTree;
    private JPanel searchPanel;
    private JLabel label2;
    private JTextField searchTemplatesTextField;
    private JPanel panel5;
    private JButton refreshButton;
    private JButton galleryButton;
    private JPanel templateDetailsPanel;
    private JButton useTemplate;
    private JPanel panel4;
    private JTabbedPane tabbedPane1;
    private JPanel descriptionPanel;
    private JPanel panel2;
    private JLabel label1;
    private JLabel authorLabel;
    private JPanel panel3;
    private JLabel checkBox1;
    private JCheckBox hasUICheckBox;
    private JPanel tempDescContainer;
    private JSplitPane descContainerSplitPanel;
    private JScrollPane scrollPane3;
    private JTextArea descriptionArea;
    private JPanel previewPanel;
    private PreviewImagePanel previewImagePanel;
    private JPanel scriptPanel;
    private JScrollPane scrollPane2;
    private JTextArea templateArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
