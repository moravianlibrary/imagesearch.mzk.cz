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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.JCD;
import net.semanticmetadata.lire.imageanalysis.LireFeature;
import net.semanticmetadata.lire.imageanalysis.OpponentHistogram;
import net.semanticmetadata.lire.imageanalysis.PHOG;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.impl.SurfDocumentBuilder;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import net.semanticmetadata.lire.solr.utils.PropertiesUtils;
import net.semanticmetadata.lire.solr.utils.QueryImageUtils;
import net.semanticmetadata.lire.solr.utils.SurfUtils;
import net.semanticmetadata.lire.utils.ImageUtils;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * This file is part of LIRE, a Java library for content based image retrieval.
 *
 * @author Mathias Lux, mathias@juggle.at, 07.07.13
 */

public class LireRequestHandler extends RequestHandlerBase {
    private static HashMap<String, Class> fieldToClass = new HashMap<String, Class>(5);
    private long time = 0;
    private int countRequests = 0;
    private int defaultNumberOfResults = 60;
    /**
     * number of candidate results retrieved from the index. The higher this number, the slower,
     * the but more accurate the retrieval will be.
     */
    private int candidateResultNumber = 2000;

    static {
        fieldToClass.put("cl_ha", ColorLayout.class);
        fieldToClass.put("ph_ha", PHOG.class);
        fieldToClass.put("oh_ha", OpponentHistogram.class);
        fieldToClass.put("eh_ha", EdgeHistogram.class);
        fieldToClass.put("jc_ha", JCD.class);

        // one time hash function read ...
        try {
            BitSampling.readHashFunctions();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(NamedList args) {
        super.init(args);
    }

    /**
     * Handles three types of requests.
     * <ol>
     * <li>search by already extracted images.</li>
     * <li>search by an image URL.</li>
     * <li>Random results.</li>
     * </ol>
     *
     * @param req
     * @param rsp
     * @throws Exception
     */
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        // (1) check if the necessary parameters are here
        if (req.getParams().get("hashes") != null) { // we are searching for hashes ...
            handleHashSearch(req, rsp);
        } else if (req.getParams().get("url") != null) { // we are searching for an image based on an URL
            handleUrlSearch(req, rsp);
        } else if (req.getParams().get("id") != null) { // we are searching for an image based on an URL
            handleIdSearch(req, rsp);
        } else if (req.getParams().get("extract") != null) { // we are trying to extract from an image URL.
            handleExtract(req, rsp);
        } else { // lets return random results.
            handleRandomSearch(req, rsp);
        }
    }
    
    private void surfUrlSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, ParseException {
    	String url = req.getParams().get("url");
    	int rows = req.getParams().getInt("rows");
    	String mode = req.getParams().get("mode");
    	SolrIndexSearcher searcher = req.getSearcher();
    	searcher.setSimilarity(new BM25Similarity());
    	IndexReader reader = searcher.getIndexReader();
    	QueryParser qp = new QueryParser(LuceneUtils.LUCENE_VERSION, "su_ha", new WhitespaceAnalyzer(LuceneUtils.LUCENE_VERSION));
    	BooleanQuery.setMaxClauseCount(10000);
    	Properties properties = PropertiesUtils.getProperties(searcher.getCore());
    	
    	SurfDocumentBuilder sb = new SurfDocumentBuilder();
    	Document query = sb.createDocument(QueryImageUtils.resizeQueryImage(ImageIO.read(new URL(url).openStream()), properties), "image");
    	
    	if ("vw".equals(mode)) {
    		SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
        	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
        	// Get Visual Words
        	query = sh.getVisualWords(query);
    		
        	String queryString = query.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0];
        	Query tq = qp.parse(queryString);
        	TopDocs docs = searcher.search(tq, rows);
        	
        	LinkedList<HashMap<String, Comparable>> list = new LinkedList<HashMap<String, Comparable>>();
    		
    		for (int i = 0; i < docs.scoreDocs.length; i++) {
                float d = 1f / docs.scoreDocs[i].score;
                HashMap<String, Comparable> m = new HashMap<String, Comparable>(2);
                m.put("d", d);
                m.put("id", reader.document(docs.scoreDocs[i].doc).get("id"));
                list.add(m);
            }
    		
            rsp.add("docs", list);
    	} else {
    		// load interest points from document
        	ArrayList<SurfInterestPoint> queryPoints = new ArrayList<SurfInterestPoint>();
        	IndexableField[] queryFields = query.getFields(DocumentBuilder.FIELD_NAME_SURF);
        	for (IndexableField queryField : queryFields) {
        		SurfFeature feature = new SurfFeature();
        		feature.setByteArrayRepresentation(queryField.binaryValue().bytes, queryField.binaryValue().offset, queryField.binaryValue().length);
    			SurfInterestPoint sip = new SurfInterestPoint(feature.descriptor);
    			queryPoints.add(sip);
    		}
        	// sort for faster compare
        	Collections.sort(queryPoints);

        	SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
        	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
        	// Get Visual Words
        	query = sh.getVisualWords(query);
        	
        	String queryString = query.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0];
        	Query tq = qp.parse(queryString);
        	TopDocs docs = searcher.search(tq, Integer.parseInt(properties.getProperty("numVisualWordsImages")));
        	
        	TreeSet<SimpleResult> resultScoreDocs = new TreeSet<SimpleResult>();
        	float maxDistance = -1f;
            float tmpScore;
        	
        	for (int i = 0; i < docs.scoreDocs.length; i++) {
        		Document doc = reader.document(docs.scoreDocs[i].doc);
        		
        		// load interest points from document
            	ArrayList<SurfInterestPoint> docPoints = new ArrayList<SurfInterestPoint>();
            	IndexableField[] docFields = doc.getFields("su_hi");
            	for (IndexableField docField : docFields) {
            		SurfFeature feature = new SurfFeature();
            		feature.setByteArrayRepresentation(docField.binaryValue().bytes, docField.binaryValue().offset, docField.binaryValue().length);
        			SurfInterestPoint sip = new SurfInterestPoint(feature.descriptor);
        			docPoints.add(sip);
        		}
            	
                tmpScore = SurfUtils.getDistance(docPoints, queryPoints);
                if (resultScoreDocs.size() < rows) {
                    resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
                    maxDistance = resultScoreDocs.last().getDistance();
                } else if (tmpScore < maxDistance) {
//                    if it is nearer to the sample than at least one of the current set:
//                    remove the last one ...
                    resultScoreDocs.remove(resultScoreDocs.last());
//                    add the new one ...
                    resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
//                    and set our new distance border ...
                    maxDistance = resultScoreDocs.last().getDistance();
                }
            }
        	
        	LinkedList<HashMap<String, Comparable>> list = new LinkedList<HashMap<String, Comparable>>();
        	
        	for (SimpleResult result : resultScoreDocs) {
        		HashMap<String, Comparable> m = new HashMap<String, Comparable>(2);
        		m.put("d", result.getDistance());
        		m.put("id", result.getDocument().get("id"));
        		list.add(m);
    		}
    		
            rsp.add("docs", list);
    	}
    	
    }
    
    private void surfIdSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, ParseException {
    	String id = req.getParams().get("id");
    	int rows = req.getParams().getInt("rows");
    	String mode = req.getParams().get("mode");
    	SolrIndexSearcher searcher = req.getSearcher();
    	searcher.setSimilarity(new BM25Similarity());
    	IndexReader reader = searcher.getIndexReader();
    	QueryParser qp = new QueryParser(LuceneUtils.LUCENE_VERSION, "su_ha", new WhitespaceAnalyzer(LuceneUtils.LUCENE_VERSION));
    	BooleanQuery.setMaxClauseCount(10000);
    	Properties properties = PropertiesUtils.getProperties(searcher.getCore());
    	
    	TopDocs hits = searcher.search(new TermQuery(new Term("id", id)), 1);
    	
    	if (hits.scoreDocs.length > 0) {
    		Document queriedDoc = reader.document(hits.scoreDocs[0].doc);
    		
    		Document query = new Document();
    		query.add(new StringField(DocumentBuilder.FIELD_NAME_IDENTIFIER, queriedDoc.getValues("id")[0], Field.Store.YES));
    		
    		IndexableField[] features = queriedDoc.getFields("su_hi");
    		for (IndexableField field : features) {
				query.add(new StoredField(DocumentBuilder.FIELD_NAME_SURF, field.binaryValue()));
			}
    		
    		if ("vw".equals(mode)) {
        		SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
            	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
            	// Get Visual Words
            	query = sh.getVisualWords(query);
        		
            	String queryString = query.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0];
            	Query tq = qp.parse(queryString);
            	TopDocs docs = searcher.search(tq, rows);
            	
            	LinkedList<HashMap<String, Comparable>> list = new LinkedList<HashMap<String, Comparable>>();
        		
        		for (int i = 0; i < docs.scoreDocs.length; i++) {
                    float d = 1f / docs.scoreDocs[i].score;
                    HashMap<String, Comparable> m = new HashMap<String, Comparable>(2);
                    m.put("d", d);
                    m.put("id", reader.document(docs.scoreDocs[i].doc).get("id"));
                    list.add(m);
                }
        		
                rsp.add("docs", list);
        	} else {
        		// load interest points from document
            	ArrayList<SurfInterestPoint> queryPoints = new ArrayList<SurfInterestPoint>();
            	IndexableField[] queryFields = query.getFields(DocumentBuilder.FIELD_NAME_SURF);
            	for (IndexableField queryField : queryFields) {
            		SurfFeature feature = new SurfFeature();
            		feature.setByteArrayRepresentation(queryField.binaryValue().bytes, queryField.binaryValue().offset, queryField.binaryValue().length);
        			SurfInterestPoint sip = new SurfInterestPoint(feature.descriptor);
        			queryPoints.add(sip);
        		}
            	// sort for faster compare
            	Collections.sort(queryPoints);

            	SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
            	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
            	// Get Visual Words
            	query = sh.getVisualWords(query);
            	
            	String queryString = query.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0];
            	Query tq = qp.parse(queryString);
            	TopDocs docs = searcher.search(tq, Integer.parseInt(properties.getProperty("numVisualWordsImages")));
            	
            	TreeSet<SimpleResult> resultScoreDocs = new TreeSet<SimpleResult>();
            	float maxDistance = -1f;
                float tmpScore;
            	
            	for (int i = 0; i < docs.scoreDocs.length; i++) {
            		Document doc = reader.document(docs.scoreDocs[i].doc);
            		
            		// load interest points from document
                	ArrayList<SurfInterestPoint> docPoints = new ArrayList<SurfInterestPoint>();
                	IndexableField[] docFields = doc.getFields("su_hi");
                	for (IndexableField docField : docFields) {
                		SurfFeature feature = new SurfFeature();
                		feature.setByteArrayRepresentation(docField.binaryValue().bytes, docField.binaryValue().offset, docField.binaryValue().length);
            			SurfInterestPoint sip = new SurfInterestPoint(feature.descriptor);
            			docPoints.add(sip);
            		}
                	
                    tmpScore = SurfUtils.getDistance(docPoints, queryPoints);
                    if (resultScoreDocs.size() < rows) {
                        resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
                        maxDistance = resultScoreDocs.last().getDistance();
                    } else if (tmpScore < maxDistance) {
//                        if it is nearer to the sample than at least one of the current set:
//                        remove the last one ...
                        resultScoreDocs.remove(resultScoreDocs.last());
//                        add the new one ...
                        resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
//                        and set our new distance border ...
                        maxDistance = resultScoreDocs.last().getDistance();
                    }
                }
            	
            	LinkedList<HashMap<String, Comparable>> list = new LinkedList<HashMap<String, Comparable>>();
            	
            	for (SimpleResult result : resultScoreDocs) {
            		HashMap<String, Comparable> m = new HashMap<String, Comparable>(2);
            		m.put("d", result.getDistance());
            		m.put("id", result.getDocument().get("id"));
            		list.add(m);
        		}
        		
                rsp.add("docs", list);
        	}
    		
    		/*SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
        	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
        	// Get Visual Words
        	query = sh.getVisualWords(query);
    		
        	String queryString = query.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0];
        	Query tq = qp.parse(queryString);
        	TopDocs docs = searcher.search(tq, rows);
        	
        	LinkedList<HashMap<String, Comparable>> list = new LinkedList<HashMap<String, Comparable>>();
    		
    		for (int i = 0; i < docs.scoreDocs.length; i++) {
                float d = 1f / docs.scoreDocs[i].score;
                HashMap<String, Comparable> m = new HashMap<String, Comparable>(2);
                m.put("d", d);
                m.put("id", reader.document(docs.scoreDocs[i].doc).get("id"));
                list.add(m);
            }
    		
            rsp.add("docs", list);*/
    	}
    }
    
    /**
     * Handles the get parameters id, field and rows.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParseException 
     */
    private void handleIdSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException, ParseException {
        SolrIndexSearcher searcher = req.getSearcher();
        String paramId = req.getParams().get("id");
        String paramField = "cl_ha";
        int paramRows = defaultNumberOfResults;
        
        if (req.getParams().get("field") != null)
            paramField = req.getParams().get("field");
        
        if (req.getParams().getInt("rows") != null)
            paramRows = req.getParams().getInt("rows");
        
        // SURF method
        if ("su_ha".equals(paramField)) {
        	surfIdSearch(req, rsp);
        	return;
        }
        
        try {
            TopDocs hits = searcher.search(new TermQuery(new Term("id", paramId)), 1);
            
            LireFeature queryFeature = (LireFeature) fieldToClass.get(paramField).newInstance();
            rsp.add("QueryField", paramField);
            rsp.add("QueryFeature", queryFeature.getClass().getName());

            if (hits.scoreDocs.length > 0) {
                // Using DocValues to get the actual data from the index.
                BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), paramField.replace("_ha", "_hi")); // ***  #
                if (binaryValues == null)
                    System.err.println("Could not find the DocValues of the query document. Are they in the index?");
                BytesRef bytesRef = new BytesRef();
                binaryValues.get(hits.scoreDocs[0].doc, bytesRef);
//                Document d = searcher.getIndexReader().document(hits.scoreDocs[0].doc);
                String histogramFieldName = paramField.replace("_ha", "_hi");
                queryFeature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
                // Re-generating the hashes to save space (instead of storing them in the index)
                int[] hashes = BitSampling.generateHashes(queryFeature.getDoubleHistogram());
                // just use 50% of the hashes for search ...
                BooleanQuery query = createQuery(hashes, paramField, 0.5d);
                doSearch(rsp, searcher, paramField, paramRows, query, queryFeature);
            } else {
                rsp.add("Error", "Did not find an image with the given id " + req.getParams().get("id"));
            }
        } catch (Exception e) {
            rsp.add("Error", "There was an error with your search for the image with the id " + req.getParams().get("id")
                    + ": " + e.getMessage());
        }
    }

    /**
     * Returns a random set of documents from the index. Mainly for testing purposes.
     *
     * @param req
     * @param rsp
     * @throws IOException
     */
    private void handleRandomSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
        SolrIndexSearcher searcher = req.getSearcher();
        DirectoryReader indexReader = searcher.getIndexReader();
        double maxDoc = indexReader.maxDoc();
        int paramRows = defaultNumberOfResults;
        if (req.getParams().getInt("rows") != null)
            paramRows = req.getParams().getInt("rows");
        LinkedList list = new LinkedList();
        while (list.size() < paramRows) {
            HashMap m = new HashMap(2);
            Document d = indexReader.document((int) Math.floor(Math.random() * maxDoc));
            m.put("id", d.getValues("id")[0]);
            //m.put("title", d.getValues("title")[0]);
            list.add(m);
        }
        rsp.add("docs", list);
    }

    /**
     * Searches for an image given by an URL. Note that (i) extracting image features takes time and
     * (ii) not every image is readable by Java.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ParseException 
     */
    private void handleUrlSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException, ParseException {
        SolrParams params = req.getParams();
        String paramUrl = params.get("url");
        String paramField = "cl_ha";
        if (req.getParams().get("field") != null)
            paramField = req.getParams().get("field");
        int paramRows = defaultNumberOfResults;
        if (params.get("rows") != null)
            paramRows = params.getInt("rows");
        
        // SURF method
        if ("su_ha".equals(paramField)) {
        	surfUrlSearch(req, rsp);
        	return;
        }
        
        LireFeature feat = null;
        BooleanQuery query = null;
        // wrapping the whole part in the try
        try {
            BufferedImage img = ImageIO.read(new URL(paramUrl).openStream());
            img = ImageUtils.trimWhiteSpace(img);
            // getting the right feature per field:
            if (paramField == null) feat = new EdgeHistogram();
            else {
                if (paramField.equals("cl_ha")) feat = new ColorLayout();
                else if (paramField.equals("jc_ha")) feat = new JCD();
                else if (paramField.equals("ph_ha")) feat = new PHOG();
                else if (paramField.equals("oh_ha")) feat = new OpponentHistogram();
                else feat = new EdgeHistogram();
            }
            feat.extract(img);
            int[] hashes = BitSampling.generateHashes(feat.getDoubleHistogram());
            // just use 50% of the hashes for search ...
            query = createQuery(hashes, paramField, 0.5d);
        } catch (Exception e) {
            rsp.add("Error", "Error reading image from URL: " + paramUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
        // search if the feature has been extracted.
        if (feat != null) doSearch(rsp, req.getSearcher(), paramField, paramRows, query, feat);
    }

    private void handleExtract(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, InstantiationException, IllegalAccessException {
        SolrParams params = req.getParams();
        String paramUrl = params.get("extract");
        String paramField = "cl_ha";
        if (req.getParams().get("field") != null)
            paramField = req.getParams().get("field");
//        int paramRows = defaultNumberOfResults;
//        if (params.get("rows") != null)
//            paramRows = params.getInt("rows");
        LireFeature feat = null;
//        BooleanQuery query = null;
        // wrapping the whole part in the try
        try {
            BufferedImage img = ImageIO.read(new URL(paramUrl).openStream());
            img = ImageUtils.trimWhiteSpace(img);
            // getting the right feature per field:
            if (paramField == null) feat = new EdgeHistogram();
            else {
                if (paramField.equals("cl_ha")) feat = new ColorLayout();
                else if (paramField.equals("jc_ha")) feat = new JCD();
                else if (paramField.equals("ph_ha")) feat = new PHOG();
                else if (paramField.equals("oh_ha")) feat = new OpponentHistogram();
                else feat = new EdgeHistogram();
            }
            feat.extract(img);
            rsp.add("histogram", Base64.encodeBase64String(feat.getByteArrayRepresentation()));
//            int[] hashes = BitSampling.generateHashes(feat.getDoubleHistogram());
//            just use 50% of the hashes for search ...
//            query = createQuery(hashes, paramField, 0.5d);
        } catch (Exception e) {
//            rsp.add("Error", "Error reading image from URL: " + paramUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
        // search if the feature has been extracted.
//        if (feat != null) doSearch(rsp, req.getSearcher(), paramField, paramRows, query, feat);
    }

    /**
     * Search based on the given image hashes.
     *
     * @param req
     * @param rsp
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void handleHashSearch(SolrQueryRequest req, SolrQueryResponse rsp) throws IOException, IllegalAccessException, InstantiationException {
        SolrParams params = req.getParams();
        SolrIndexSearcher searcher = req.getSearcher();
        // get the params needed:
        // hashes=x y z ...
        // feature=<base64>
        // field=<cl_ha|ph_ha|...>

        String[] hashes = params.get("hashes").trim().split(" ");
        byte[] featureVector = Base64.decodeBase64(params.get("feature"));
        String paramField = "cl_ha";
        if (req.getParams().get("field") != null)
            paramField = req.getParams().get("field");
        int paramRows = defaultNumberOfResults;
        if (params.getInt("rows") != null)
            paramRows = params.getInt("rows");
        // create boolean query:
//        System.out.println("** Creating query.");
        BooleanQuery query = new BooleanQuery();
        for (int i = 0; i < hashes.length; i++) {
            // be aware that the hashFunctionsFileName of the field must match the one you put the hashes in before.
            hashes[i] = hashes[i].trim();
            if (hashes[i].length() > 0) {
                query.add(new BooleanClause(new TermQuery(new Term(paramField, hashes[i].trim())), BooleanClause.Occur.SHOULD));
//                System.out.println("** " + field + ": " + hashes[i].trim());
            }
        }
//        System.out.println("** Doing search.");

        // query feature
        LireFeature queryFeature = (LireFeature) fieldToClass.get(paramField).newInstance();
        queryFeature.setByteArrayRepresentation(featureVector);

        // get results:
        doSearch(rsp, searcher, paramField, paramRows, query, queryFeature);
    }

    /**
     * Actual search implementation based on (i) hash based retrieval and (ii) feature based re-ranking.
     *
     * @param rsp
     * @param searcher
     * @param field
     * @param maximumHits
     * @param query
     * @param queryFeature
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void doSearch(SolrQueryResponse rsp, SolrIndexSearcher searcher, String field, int maximumHits, BooleanQuery query, LireFeature queryFeature) throws IOException, IllegalAccessException, InstantiationException {
        // temp feature instance
        LireFeature tmpFeature = queryFeature.getClass().newInstance();
        // Taking the time of search for statistical purposes.
        time = System.currentTimeMillis();
        TopDocs docs = searcher.search(query, candidateResultNumber);
        time = System.currentTimeMillis() - time;
        rsp.add("RawDocsCount", docs.scoreDocs.length + "");
        rsp.add("RawDocsSearchTime", time + "");
        // re-rank
        time = System.currentTimeMillis();
        TreeSet<SimpleResult> resultScoreDocs = new TreeSet<SimpleResult>();
        float maxDistance = -1f;
        float tmpScore;

        String name = field.replace("_ha", "_hi");
        Document d;
        // iterating and re-ranking the documents.
        BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), name); // ***  #
        BytesRef bytesRef = new BytesRef();
        for (int i = 0; i < docs.scoreDocs.length; i++) {
            // using DocValues to retrieve the field values ...
            binaryValues.get(docs.scoreDocs[i].doc, bytesRef);
            tmpFeature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // Getting the document from the index.
            // This is the slow step based on the field compression of stored fields.
//            tmpFeature.setByteArrayRepresentation(d.getBinaryValue(name).bytes, d.getBinaryValue(name).offset, d.getBinaryValue(name).length);
            tmpScore = queryFeature.getDistance(tmpFeature);
            if (resultScoreDocs.size() < maximumHits) {
                resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
                maxDistance = resultScoreDocs.last().getDistance();
            } else if (tmpScore < maxDistance) {
//                if it is nearer to the sample than at least one of the current set:
//                remove the last one ...
                resultScoreDocs.remove(resultScoreDocs.last());
//                add the new one ...
                resultScoreDocs.add(new SimpleResult(tmpScore, searcher.doc(docs.scoreDocs[i].doc), docs.scoreDocs[i].doc));
//                and set our new distance border ...
                maxDistance = resultScoreDocs.last().getDistance();
            }
        }
//        System.out.println("** Creating response.");
        time = System.currentTimeMillis() - time;
        rsp.add("ReRankSearchTime", time + "");
        LinkedList list = new LinkedList();
        for (Iterator<SimpleResult> it = resultScoreDocs.iterator(); it.hasNext(); ) {
            SimpleResult result = it.next();
            HashMap m = new HashMap(2);
            m.put("d", result.getDistance());
            m.put("id", result.getDocument().get("id"));
            m.put("title", result.getDocument().get("title"));
//            m.put(field, result.getDocument().get(field));
//            m.put(field.replace("_ha", "_hi"), result.getDocument().getBinaryValue(field));
            list.add(m);
        }
        rsp.add("docs", list);
        // rsp.add("Test-name", "Test-val");
    }

    @Override
    public String getDescription() {
        return "LIRE Request Handler to add images to an index and search them. Search images by id, by url and by extracted features.";
    }

    @Override
    public String getSource() {
        return "http://lire-project.net";
    }

    @Override
    public NamedList<Object> getStatistics() {
        // Change stats here to get an insight in the admin console.
        NamedList<Object> statistics = super.getStatistics();
        statistics.add("Number of Requests", countRequests);
        return statistics;
    }

    private BooleanQuery createQuery(int[] hashes, String paramField, double size) {
        List<Integer> hList = new ArrayList<Integer>(hashes.length);
        for (int i = 0; i < hashes.length; i++) {
            hList.add(hashes[i]);
        }
        Collections.shuffle(hList);
        BooleanQuery query = new BooleanQuery();
        int numHashes = (int) Math.min(hashes.length, Math.floor(hashes.length * size));
        if (numHashes < 5) numHashes = hashes.length;
        for (int i = 0; i < numHashes; i++) {
            // be aware that the hashFunctionsFileName of the field must match the one you put the hashes in before.
            query.add(new BooleanClause(new TermQuery(new Term(paramField, Integer.toHexString(hashes[i]))), BooleanClause.Occur.SHOULD));
        }
        return query;
    }
    
}
