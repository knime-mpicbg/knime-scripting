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
        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/R/figure-templates.txt"), "R");
        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Matlab/figure-templates.txt"), "Matlab");
        urls.put(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Python/figure-templates.txt"), "Python");
//
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
