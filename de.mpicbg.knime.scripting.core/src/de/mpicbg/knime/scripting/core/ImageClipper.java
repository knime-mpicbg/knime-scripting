package de.mpicbg.knime.scripting.core;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;


/**
 * Document me!
 *
 * @author Holger Brandl
 */

//    public static DataFlavor myFlavor = java.awt.datatransfer.DataFlavor.imageFlavor;


public class ImageClipper implements ClipboardOwner, Transferable {

    private int[] pix;
    private BufferedImage image;

    public static DataFlavor myFlavor = java.awt.datatransfer.DataFlavor.imageFlavor;


    public void copyToClipboard(BufferedImage panelImage) {
        image = panelImage;
        int imgWid = image.getWidth();
        int imgHt = image.getHeight();

        pix = new int[imgWid * imgHt];
        PixelGrabber pixGrab = new PixelGrabber(image, 0, 0, imgWid, imgHt, pix, 0, imgWid);
        try {
            pixGrab.grabPixels();  //could throw an InterruptedException
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        // copy the image into the system clipboard
        Clipboard myClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        myClipboard.setContents(this, this);
    }


    public void lostOwnership(Clipboard clipboard, Transferable contents) {
//            System.out.println("Lost ownership of system clipboard");
    }


    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{myFlavor};
    }


    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return myFlavor.equals(flavor);
    }


    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!myFlavor.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }

        return image;
    }
}
