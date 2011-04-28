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
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Template2Html {

    public static void main(String[] args) throws IOException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new URL("http://idisk.mpi-cbg.de/~brandl/scripttemplates/screenmining/R/figure-templates.txt"));
        urls.add(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Matlab/figure-templates.txt"));
        urls.add(new URL("http://dl.dropbox.com/u/18607042/knime-sripting-templates/Python/figure-templates.txt"));
//


        List<ScriptTemplate> scriptTemplates = new ArrayList<ScriptTemplate>();
        for (URL url : urls) {
            scriptTemplates.addAll(ScriptTemplateWizard.parseTemplateFile(url));
        }

        System.err.println(scriptTemplates.toString());

        File galleryFile = File.createTempFile("templateGallery", ".html");

        FileWriter outFile = new FileWriter(galleryFile);
        PrintWriter out = new PrintWriter(outFile);


        out.print("<html>\n" +
                "<body>\n" +
                "\n" +
                "<h1>My First Heading</h1>\n" +
                "\n" +
                "<p>Hallo Antje.</p>\n");


        out.print("<table border=\"1\">\n" +
                "<tr>\n" +
                "<th>Name</th>\n" +
                "<th>Category</th>\n" +
                "<th>Author</th>\n" +
                "<th>Description</th>\n" +
                "<th>Language</th>\n" +
                "<th>Preview</th>\n" +
                "</tr>\n");
        //    write table header
        for (ScriptTemplate scriptTemplate : scriptTemplates) {
            out.print("<tr>\n" +
                    "<td>" + scriptTemplate.getName() + "</td>\n" +
                    "<td>" + scriptTemplate.getCategories().toString().replace("[", "").replace("]", "").trim() + "</td>\n" +
                    "<td>" + scriptTemplate.getAuthor() + "</td>\n" +
                    "<td>" + scriptTemplate.getDescription() + "</td>\n" +
                    "<td> R </td>\n");


            String previewURL = scriptTemplate.getPreviewURL();
            if (previewURL != null) {
                out.print("<td> <a href=\"" + previewURL + "\"><img src=\"" + previewURL + "\" width=\"300\" height=\"200\"/> </a> </td>\n");
            } else {
                out.print("<td>No Preview</td>");
            }

            out.print("</tr>\n");
        }


        // write table footer
        out.print("</table>");

        out.print("\n" +
                "</body>\n" +
                "</html>");

        out.flush();
        out.close();

        Desktop.getDesktop().edit(galleryFile);

    }


}
