/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 */
package org.nuxeo.data.gen.pdf.itext.filter;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class JpegFilter extends AbstractFilter implements PDFOutputFilter {

	public static final String NAME = "jpeg";

	@Override
	public String getFilterName() {
		return NAME;
	}

	@Override
	public void render(ByteArrayInputStream pdf, OutputStream out) throws Exception {

		PDDocument doc = PDDocument.load(pdf);
		doc.setAllSecurityToBeRemoved(true);
		PDFRenderer renderer = new PDFRenderer(doc);
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF));
		hints.add(new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE));
		renderer.setRenderingHints(hints);
				
		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		JPEGImageWriteParam jpgWriteParam = (JPEGImageWriteParam) jpgWriter.getDefaultWriteParam();

		jpgWriteParam.setOptimizeHuffmanTables(true);
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(0.5f);
		jpgWriteParam.setCompressionType("JPEG");
		jpgWriteParam.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
		jpgWriteParam.setController(null);

		BufferedImage bim = renderer.renderImageWithDPI(0, getDPI(), ImageType.RGB);
		MemoryCacheImageOutputStream outStream = new MemoryCacheImageOutputStream(out);
		jpgWriter.setOutput(outStream);

		jpgWriter.removeAllIIOWriteProgressListeners();
		jpgWriter.removeAllIIOWriteWarningListeners();
		jpgWriter.write(null, new IIOImage(bim, null, null), jpgWriteParam);

		doc.close();
		jpgWriter.dispose();
		outStream.flush();
		outStream.close();
		out.flush();
	}

	@Override
	public String getFileExtension() {
		return ".jpg";
	}

}