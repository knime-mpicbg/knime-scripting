/*
 * Module Name: scriptingcore
 * This module is a plugin for the KNIME platform <http://www.knime.org/>
 *
 * Copyright (c) 2011.
 * Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Detailed terms and conditions are described in the license.txt.
 *     also see <http://www.gnu.org/licenses/>.
 */

package de.mpicbg.tds.knime.knutils.scripting.rgg;

import de.mpicbg.tds.knime.knutils.scripting.ScriptTemplateFile;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Template2Html {

    private Map<URL, String> templateUrls;

    public Template2Html(Map<URL, String> figurls) {
        this.templateUrls = figurls;
    }

    public static void main(String[] args) throws IOException {

        // Figure templates
        Map<URL, String> figurls = new HashMap<URL, String>();
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/figure-templates.txt"), "Matlab");
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/figure-templates.txt"), "Python");
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/figure-templates.txt"), "R");

        Template2Html templateUrls = new Template2Html(figurls);
        templateUrls.exportTemplates(args[0] + "/figure-template-gallery.html");

        // Script templates
        Map<URL, String> scrurls = new HashMap<URL, String>();
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/script-templates.txt"), "Matlab");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/script-templates.txt"), "Python");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/snippet-templates.txt"), "R");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Groovy/Groovy-templates.txt"), "Groovy");

        templateUrls = new Template2Html(scrurls);
        templateUrls.exportTemplates(args[0] + "/script-template-gallery.html");
    }

    public void exportTemplates(String destinationFile) throws IOException {
        File figureGalleryFile = new File(destinationFile);
        writeToHtmlFile(getTemplateList(templateUrls), figureGalleryFile);
        Desktop.getDesktop().edit(figureGalleryFile);
        System.err.println("file is " + figureGalleryFile);
    }


    public static File exportToHtmlFile(List<ScriptTemplate> scriptTemplates) throws IOException {
        File galleryFile = File.createTempFile("templateGallery", ".html");

        writeToHtmlFile(scriptTemplates, galleryFile);
        return galleryFile;
    }

    public static List<ScriptTemplate> getTemplateList(Map<URL, String> urls) throws IOException {

        List scriptTemplates = new ArrayList<ScriptTemplate>();
        for (URL url : urls.keySet()) {
            ScriptTemplateFile scriptTemplateFile = new ScriptTemplateFile(url);
            if (!scriptTemplateFile.isEmpty()) {
                scriptTemplateFile.setScriptingLanguage(urls.get(url));
                scriptTemplates.addAll(scriptTemplateFile.templates);
            }
        }
        return scriptTemplates;
    }

    public static void writeToHtmlFile(List<ScriptTemplate> scriptTemplates, File outputFileName) throws IOException {
        FileWriter outFile = new FileWriter(outputFileName);
        PrintWriter out = new PrintWriter(outFile);

        out.print("<html>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Scripting Template Gallery</h1>\n" +
                "\n" +
                "<p>This page lists all scripting templates available for the various scripting nodes in Knime.</p>\n");


        out.print("<table border=\"1\">\n" +
                "<tr>\n" +
                "<th>Name</th>\n" +
                "<th>Preview</th>\n" +
                "<th>Description</th>\n" +
                "<th>Category</th>\n" +
                "<th>Author</th>\n" +
                "<th>Language</th>\n" +
                "</tr>\n");

        for (ScriptTemplate scriptTemplate : scriptTemplates) {
            if (scriptTemplate != null) {
                out.print("<tr>\n" + "<td width=\"200\">" + scriptTemplate.getName() + "</td>\n");

                String previewURL = scriptTemplate.getPreviewURL();
                if (previewURL != null) {
                    out.print("<td> <a href=\"" + previewURL + "\"><img src=\"" + previewURL + "\" width=\"300\" height=\"200\"/> </a> </td>\n");
                } else {
                    out.print("<td></td>");
                }

                out.print("<td>" + scriptTemplate.getDescription() + "</td>\n" +
                        "<td>" + scriptTemplate.getCategories().toString().replace("[", "").replace("]", "").trim() + "</td>\n" +
                        "<td>" + scriptTemplate.getAuthor() + "</td>\n" +
                        "<td>" + scriptTemplate.getScriptingLanguage() + " </td>\n");

                out.print("</tr>\n");
            }
        }


        // write table footer
        out.print("</table>");

        out.print("\n" +
                "</body>\n" +
                "</html>");

        out.flush();
        out.close();
    }
}


/*class ExportTemplate {

    String scriptingLanguage;
    ScriptTemplate template;


    ExportTemplate(String scriptingLanguage, ScriptTemplate template) {
        this.scriptingLanguage = scriptingLanguage;
        this.template = template;
    }


    public static List<ExportTemplate> convert(String scriptingLanguage, List<ScriptTemplate> templates) {
        List<ExportTemplate> exTemplates = new ArrayList<ExportTemplate>();
        for (ScriptTemplate scriptTemplate : templates) {
            exTemplates.add(new ExportTemplate(scriptingLanguage, scriptTemplate));
        }

        return exTemplates;
    }
}
    */