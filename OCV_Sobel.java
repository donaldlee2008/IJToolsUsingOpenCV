import ij.*;
import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import static ij.plugin.filter.ExtendedPlugInFilter.KEEP_PREVIEW;
import static ij.plugin.filter.PlugInFilter.DOES_16;
import static ij.plugin.filter.PlugInFilter.DOES_32;
import static ij.plugin.filter.PlugInFilter.DOES_8G;
import static ij.plugin.filter.PlugInFilter.DONE;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import java.awt.AWTEvent;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/*
 * The MIT License
 *
 * Copyright 2016 Takehito Nishida.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Sobel (OpenCV3.1).
 */
public class OCV_Sobel implements ij.plugin.filter.ExtendedPlugInFilter, DialogListener
{
    // constant var.
    private static final int FLAGS = DOES_8G | DOES_16 | DOES_32 | KEEP_PREVIEW;
    
    /*
     Various border types, image boundaries are denoted with '|'

     * BORDER_ISOLATED:      can not use
     * BORDER_REFLECT:       fedcba|abcdefgh|hgfedcb
     * BORDER_REFLECT_101:   gfedcb|abcdefgh|gfedcba
     * BORDER_REPLICATE:     aaaaaa|abcdefgh|hhhhhhh
     * BORDER_WRAP:          can not use
     * BORDER_TRANSPARENT    can not use
     */
    private static final int[] INT_BORDERTYPE = { Core.BORDER_REFLECT, Core.BORDER_REFLECT101, Core.BORDER_REPLICATE };
    private static final String[] STR_BORDERTYPE = { "BORDER_REFLECT", "BORDER_REFLECT101", "BORDER_REPLICATE" };

    // staic var.
    private static int dx = 1; // order of the derivative x.
    private static int dy = 1; // order of the derivative y.
    private static int ksize = 3; // size of the extended Sobel kernel; it must be 1, 3, 5, or 7.
    private static double scale = 1; // optional scale factor for the computed derivative values.
    private static double delta = 0; // optional delta value that is added to the results prior to storing them in dst.
    private static int indBorderType = 1; // border types
 
    @Override
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr)
    {
        GenericDialog gd = new GenericDialog(command.trim() + " ...");
        
        gd.addNumericField("dx", dx, 0);
        gd.addNumericField("dy", dy, 0);
        gd.addNumericField("ksize", ksize, 0);
        gd.addNumericField("scale", scale, 4);
        gd.addNumericField("delta", delta, 4);
        gd.addChoice("borderType", STR_BORDERTYPE, STR_BORDERTYPE[indBorderType]);
        gd.addPreviewCheckbox(pfr);
        gd.addDialogListener(this);

        gd.showDialog();

        if (gd.wasCanceled())
        {
            return DONE;
        }
        else
        {
            return IJ.setupDialog(imp, FLAGS);
        }
    }
    
    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent awte)
    {    
        dx = (int)gd.getNextNumber();
        dy = (int)gd.getNextNumber();
        ksize = (int)gd.getNextNumber();
        scale = (double)gd.getNextNumber();
        delta = (double)gd.getNextNumber();
        indBorderType = (int)gd.getNextChoiceIndex();

        if(dx < 0) { IJ.showStatus("'0 <= dx' is necessary."); return false; }
        if(dy < 0) { IJ.showStatus("'0 <= dy' is necessary."); return false; }
        if(dx <= 0 && dy <= 0) { IJ.showStatus("Either dx or dy is greater than zero."); return false; }
        if(ksize != 1 && ksize != 3 && ksize != 5 && ksize != 7) { IJ.showStatus("'ksize must be 1, 3, 5, or 7."); return false; }
        if(Double.isNaN(scale) || Double.isNaN(delta)) { IJ.showStatus("ERR : NaN"); return false; } 
        
        IJ.showStatus("OCV_Sobel");
        return true;
    }
    
    @Override
    public void setNPasses(int nPasses)
    {
        // do nothing
    }

    @Override
    public int setup(String arg, ImagePlus imp)
    {
        if(!OCV__LoadLibrary.isLoad())
        {
            IJ.error("Library is not loaded.");
            return DONE;
        }

        if (imp == null)
        {
            IJ.noImage();
            return DONE;
        }
        else
        {
            return FLAGS;
        }
    }

    @Override
    public void run(ImageProcessor ip)
    {        
        if(ip.getBitDepth() == 8)
        {
            // srcdst
            int imw = ip.getWidth();
            int imh = ip.getHeight();
            byte[] srcdst_bytes = (byte[])ip.getPixels();
            
            // mat
            Mat src_mat = new Mat(imh, imw, CvType.CV_8UC1);            
            Mat dst_mat = new Mat(imh, imw, CvType.CV_8UC1);
            
            // run
            src_mat.put(0, 0, srcdst_bytes);
            Imgproc.Sobel(src_mat, dst_mat, src_mat.depth(), dx, dy, ksize, scale, delta, INT_BORDERTYPE[indBorderType]);
            dst_mat.get(0, 0, srcdst_bytes);
        }
        else if(ip.getBitDepth() == 16)
        {
            // srcdst
            int imw = ip.getWidth();
            int imh = ip.getHeight();
            short[] srcdst_shorts = (short[])ip.getPixels();
            
            // mat
            Mat src_mat = new Mat(imh, imw, CvType.CV_16S);            
            Mat dst_mat = new Mat(imh, imw, CvType.CV_16S);
            
            // run
            src_mat.put(0, 0, srcdst_shorts);
             Imgproc.Sobel(src_mat, dst_mat, src_mat.depth(), dx, dy, ksize, scale, delta, INT_BORDERTYPE[indBorderType]);
            dst_mat.get(0, 0, srcdst_shorts);        
        }
          else if(ip.getBitDepth() == 32)
        {
            // srcdst
            int imw = ip.getWidth();
            int imh = ip.getHeight();
            float[] srcdst_floats = (float[])ip.getPixels();
            
            // mat
            Mat src_mat = new Mat(imh, imw, CvType.CV_32F);            
            Mat dst_mat = new Mat(imh, imw, CvType.CV_32F);
            
            // run
            src_mat.put(0, 0, srcdst_floats);
            Imgproc.Sobel(src_mat, dst_mat, src_mat.depth(), dx, dy, ksize, scale, delta, INT_BORDERTYPE[indBorderType]);
            dst_mat.get(0, 0, srcdst_floats);        
        }
        else
        {
            IJ.error("Wrong image format");
        }
    }
}
