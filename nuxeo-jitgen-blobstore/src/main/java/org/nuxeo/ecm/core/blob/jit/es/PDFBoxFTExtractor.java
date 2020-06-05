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
package org.nuxeo.ecm.core.blob.jit.es;

import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFBoxFTExtractor implements PDFFulltextExtractor {

	public String getText(InputStream is) throws Exception {
		return getText(new PDFTextStripper(), is);
	}

	public static String getText(PDFTextStripper stripper, InputStream is) throws Exception {

		PDDocument doc = PDDocument.load(is);
		try {
			return stripper.getText(doc);
		} finally {
			doc.close();
		}
	}

}
