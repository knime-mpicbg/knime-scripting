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

import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplate;
import de.mpicbg.tds.knime.knutils.scripting.templatewizard.ScriptTemplateWizard;

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

    public static void main(String[] args) throws IOException {

        // Figure templates
        Map<URL, String> figurls = new HashMap<URL, String>();
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/figure-templates.txt"), "Matlab");
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/figure-templates.txt"), "Python");
        figurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/figure-templates.txt"), "R");

        File figureGalleryFile = new File("/Volumes/knime/scripting-templates_public/figure-template-gallery.html");
        exportToHtmlFile(figurls, figureGalleryFile);
        Desktop.getDesktop().edit(figureGalleryFile);
        System.err.println("file is " + figureGalleryFile);

        // Script templates
        Map<URL, String> scrurls = new HashMap<URL, String>();
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/script-templates.txt"), "Matlab");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/script-templates.txt"), "Python");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/snippet-templates.txt"), "R");
        scrurls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Groovy/Groovy-templates.txt"), "Groovy");

        File scriptGalleryFile = new File("/Volumes/knime/scripting-templates_public/script-template-gallery.html");
        exportToHtmlFile(scrurls, scriptGalleryFile);
        Desktop.getDesktop().edit(scriptGalleryFile);
        System.err.println("file is " + scriptGalleryFile);
    }


    public static File exportToHtmlFile(Map<URL, String> urls) throws IOException {
        File galleryFile = File.createTempFile("templateGallery", ".html");
        exportToHtmlFile(urls, galleryFile);
        return galleryFile;
    }


    public static void exportToHtmlFile(Map<URL, String> urls, File outputFileName) throws IOException {
        List<ExportTemplate> scriptTemplates = new ArrayList<ExportTemplate>();
        for (URL url : urls.keySet()) {
            List<ScriptTemplate> basicTemplates = ScriptTemplateWizard.parseTemplateFile(url);
            scriptTemplates.addAll(ExportTemplate.convert(urls.get(url), basicTemplates));
        }

        FileWriter outFile = new FileWriter(outputFileName);
        PrintWriter out = new PrintWriter(outFile);

        out.print("<html>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Scripting Template Gallery</h1>\n" +
                "\n" +
                "<p>This page lists all scripting templates available for the various scripting plot nodes in Knime.</p>\n");


        out.print("<table border=\"1\">\n" +
                "<tr>\n" +
                "<th>Name</th>\n" +
                "<th>Preview</th>\n" +
                "<th>Description</th>\n" +
                "<th>Category</th>\n" +
                "<th>Author</th>\n" +
                "<th>Language</th>\n" +
                "</tr>\n");

        for (ExportTemplate exportTemplate : scriptTemplates) {
            ScriptTemplate scriptTemplate = exportTemplate.template;
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
                        "<td>" + exportTemplate.scriptingLanguage + " </td>\n");

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


class ExportTemplate {

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
