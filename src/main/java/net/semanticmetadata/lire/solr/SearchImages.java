/*
 * This file is part of the LIRE project: http://www.semanticmetadata.net/lire
 * LIRE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * LIRE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LIRE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * We kindly ask you to refer the any or one of the following publications in
 * any publication mentioning or employing Lire:
 *
 * Lux Mathias, Savvas A. Chatzichristofis. Lire: Lucene Image Retrieval –
 * An Extensible Java CBIR Library. In proceedings of the 16th ACM International
 * Conference on Multimedia, pp. 1085-1088, Vancouver, Canada, 2008
 * URL: http://doi.acm.org/10.1145/1459359.1459577
 *
 * Lux Mathias. Content Based Image Retrieval with LIRE. In proceedings of the
 * 19th ACM International Conference on Multimedia, pp. 735-738, Scottsdale,
 * Arizona, USA, 2011
 * URL: http://dl.acm.org/citation.cfm?id=2072432
 *
 * Mathias Lux, Oge Marques. Visual Information Retrieval using Java and LIRE
 * Morgan & Claypool, 2013
 * URL: http://www.morganclaypool.com/doi/abs/10.2200/S00468ED1V01Y201301ICR025
 *
 * Copyright statement:
 * --------------------
 * (c) 2002-2013 by Mathias Lux (mathias@juggle.at)
 *     http://www.semanticmetadata.net/lire, http://www.lire-project.net
 */

package net.semanticmetadata.lire.solr;

import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import org.apache.commons.codec.binary.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * This file is part of LIRE, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 07.07.13
 */

public class SearchImages {
    private static String baseURL = "http://gtxzilla.itec.uni-klu.ac.at:9000/solr/lire/get";
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        // http://localhost:8080/solr/lire/query?q=hashes%3A1152++hashes%3A605++hashes%3A96++hashes%3A275++&wt=xml
        String hashes = "1152  605  96  275  2057  3579  3950  2831  2367  3169  3292  974  2465  1573  2933  3125  314  2158  3532  974  2198  2315  3013  3302  3316  1467  2213  818  3  1083  18  2604  327  1370  593  3677  464  79  256  984  2496  1124  855  2091  780  1941  1887  1145  1396  4016  2406  2227  1532  2598  215  1375  171  2516  1698  368  2350  3799  223  1471  2083  1051  3015  3789  3374  1442  3991  3575  1452  751  428  3103  1182  2241  474  275  3678  3970  559  3394  2662  2361  2048  1083  181  1483  3903  3331  2363  756  558  2838  3984  1878  2667  3333  1473  2136  3499  3873  1437  3091  1287  948  46  3660  3003  1572  1185  2231  2622  257  3538  3632  3989  1180  3928  3144  1492  3941  3253  3498  2721  1036  22  1020  725  1431  3821  2248  2542  3659  2849  524  2967  1  2493  3620  2951  3584  1641  3873  2087  1506  1489  3064";
        String[] split = hashes.split(" ");
        String query = "";
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s.trim().length() > 0) query += " cl_ha:" + s.trim();
        }
        URL u = new URL(baseURL + "?q=" + URLEncoder.encode(query, "utf-8") + "&wt=xml&rows=500");
        InputStream in = u.openStream();
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        SolrResponseHandler dh = new SolrResponseHandler();
        saxParser.parse(in, dh);
        ArrayList<ResultItem> results = dh.getResults();
        // re-rank:

    }


}

class ResultItem {
    private LireFeature feature;
    private String id;

    ResultItem(LireFeature feature, String id) {
        this.feature = feature;
        this.id = id;
    }
}

class SolrResponseHandler extends DefaultHandler {
    boolean isInDocument;
    boolean isInHistogram;
    boolean isInId;
    int countResults = 0;
    StringBuilder hist = new StringBuilder(256);
    StringBuilder id = new StringBuilder(256);
    private ArrayList<ResultItem> results = new ArrayList<ResultItem>(500);

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.startsWith("doc")) {
            isInDocument = true;
        } else if (isInDocument && qName.startsWith("str")) {
            if (attributes.getValue("name") != null && attributes.getValue("name").equals("cl_hi")) {
                isInHistogram = true;
                hist.delete(0, hist.length());
            } else if (attributes.getValue("name") != null && attributes.getValue("name").equals("id")) {
                isInId = true;
                id.delete(0, id.length());
            }

        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.startsWith("doc")) {
            isInDocument = false;
            countResults++;
            System.out.println(id.toString() + ", " + hist.toString());
            ColorLayout cl = new ColorLayout();
            cl.setByteArrayRepresentation(Base64.decodeBase64(hist.toString()));
            results.add(new ResultItem(cl, id.toString()));
        } else if (qName.startsWith("str")) {
            isInHistogram = false;
            isInId = false;
        } else if (qName.startsWith("result")) {
            System.out.println(countResults + " results found");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (isInHistogram) hist.append(ch, start, length);
        if (isInId) id.append(ch, start, length);
    }

    ArrayList<ResultItem> getResults() {
        return results;
    }
}
