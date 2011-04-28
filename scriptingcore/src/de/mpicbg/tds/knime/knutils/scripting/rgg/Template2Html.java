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
        Map<URL, String> urls = new HashMap<URL, String>();
//        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/R/figure-templates.txt"), "R");
//        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Matlab/figure-templates.txt"), "Matlab");
//        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Python/figure-templates.txt"), "Python");


        // Figure templates
        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/figure-templates.txt"), "Matlab");
        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/figure-templates.txt"), "Python");
        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/figure-templates.txt"), "R");
        // Comment these for producing the external list.
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/Matlab/TDS_figure-templates.txt"), "Matlab");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/Python/TDS_figure-templates.txt"), "Python");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/R/TDS_figure-templates.txt"), "R");


//        // Script templates
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Matlab/script-templates.txt"), "Matlab");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Python/script-templates.txt"), "Python");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/R/snippet-templates.txt"), "R");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_public/Groovy/Groovy-templates.txt"), "Groovy");
//        // Comment these for producing the external list.
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/Matlab/TDS_script-templates.txt"), "Matlab");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/Python/TDS_script-templates.txt"), "Python");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/R/TDS_snippet-templates.txt"), "R");
//        urls.put(new URL("http://idisk-srv1.mpi-cbg.de/knime/scripting-templates_tds/Groovy/TDS_Groovy-templates.txt"), "Groovy");


        File galleryFile = exportToHtmlFile(urls);

        Desktop.getDesktop().edit(galleryFile);

        System.err.println("file is " + galleryFile);
    }


    public static File exportToHtmlFile(Map<URL, String> urls) throws IOException {
        List<ExportTemplate> scriptTemplates = new ArrayList<ExportTemplate>();
        for (URL url : urls.keySet()) {
            List<ScriptTemplate> basicTemplates = ScriptTemplateWizard.parseTemplateFile(url);
            scriptTemplates.addAll(ExportTemplate.convert(urls.get(url), basicTemplates));
        }


        File galleryFile = File.createTempFile("templateGallery", ".html");
        FileWriter outFile = new FileWriter(galleryFile);
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
        return galleryFile;
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
