package net.semanticmetadata.lire.solr.requesthandler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import net.semanticmedatada.lire.solr.lsh.LSHHashTable;
import net.semanticmedatada.lire.solr.lsh.SurfInterestPoint;
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.ColorLayout;
import net.semanticmetadata.lire.imageanalysis.SurfFeature;
import net.semanticmetadata.lire.impl.SimpleResult;
import net.semanticmetadata.lire.impl.SurfDocumentBuilder;
import net.semanticmetadata.lire.indexing.hashing.BitSampling;
import net.semanticmetadata.lire.solr.SolrSurfFeatureHistogramBuilder;
import net.semanticmetadata.lire.solr.utils.PropertiesUtils;
import net.semanticmetadata.lire.solr.utils.QueryImageUtils;
import net.semanticmetadata.lire.solr.utils.SurfUtils;
import net.semanticmetadata.lire.utils.ImageUtils;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;

public class SimilarRequestHandler extends RequestHandlerBase {

	@Override
	public String getDescription() {
		return "LIRE Request Handler finds similar images.";
	}

	@Override
	public String getSource() {
		return "http://lire-project.net";
	}

	@Override
	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse res)
			throws Exception {
		String url = req.getParams().get("url");
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("You have to specify the url parameter.");
		}
		long time = System.currentTimeMillis();
		SolrIndexSearcher searcher = req.getSearcher();
    	searcher.setSimilarity(new BM25Similarity());
    	IndexReader reader = searcher.getIndexReader();
    	QueryParser qp = new QueryParser(LuceneUtils.LUCENE_VERSION, "su_ha", new WhitespaceAnalyzer(LuceneUtils.LUCENE_VERSION));
    	BooleanQuery.setMaxClauseCount(10000);
    	
    	// Read properties
    	Properties properties = PropertiesUtils.getProperties(searcher.getCore());
    	int numColorLayoutImages = Integer.parseInt(properties.getProperty("numColorLayoutImages"));
    	int numColorLayoutSimImages = Integer.parseInt(properties.getProperty("numColorLayoutSimImages"));
    	int numSurfSimImages = Integer.parseInt(properties.getProperty("numSurfSimImages"));
    	int numSimImages = Integer.parseInt(properties.getProperty("numSimilarImages"));
		res.add("LoadingMetaDataTime", System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		// Load image
		BufferedImage image = ImageIO.read(new URL(url).openStream());
		image = ImageUtils.trimWhiteSpace(image);
		res.add("ImageDownloadingTime", System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		
		// Extract image information
		
		/* CL */
		ColorLayout clFeat = new ColorLayout();
		clFeat.extract(image);
		// Create hashes
		BitSampling.readHashFunctions();
		int[] clHash = BitSampling.generateHashes(clFeat.getDoubleHistogram());
		
		/* SURF */
		SurfDocumentBuilder sb = new SurfDocumentBuilder();
    	Document suFeat = sb.createDocument(QueryImageUtils.resizeQueryImage(ImageIO.read(new URL(url).openStream()), properties), "image");
    	SolrSurfFeatureHistogramBuilder sh = new SolrSurfFeatureHistogramBuilder(null);
    	sh.setClusterFile(req.getCore().getDataDir() + "/clusters-surf.dat");
    	res.add("ImageProcessingTime", System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
    	
    	/* CL */
		BooleanQuery clQuery = createQuery(clHash, "cl_ha", 0.5d);
		TopDocs clDocs = searcher.search(clQuery, numColorLayoutImages);
		res.add("SearchCLTime", System.currentTimeMillis() - time);
		
		time = System.currentTimeMillis();
		// Re-rank color layout
        TreeSet<SimpleResult> clScoreDocs = new TreeSet<SimpleResult>();
        ColorLayout clTmpFeature = new ColorLayout();
        float clTmpDistance;
        float maxClDistance = -1;
        
        BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(searcher.getIndexReader(), "cl_hi"); // ***  #
        BytesRef bytesRef = new BytesRef();
        for (int i = 0; i < clDocs.scoreDocs.length; i++) {
            // using DocValues to retrieve the field values ...
            binaryValues.get(clDocs.scoreDocs[i].doc, bytesRef);
            clTmpFeature.setByteArrayRepresentation(bytesRef.bytes, bytesRef.offset, bytesRef.length);
            // Getting the document from the index.
            // This is the slow step based on the field compression of stored fields.
            clTmpDistance = clFeat.getDistance(clTmpFeature);
            if (clScoreDocs.size() < numColorLayoutSimImages) {
            	clScoreDocs.add(new SimpleResult(clTmpDistance, searcher.doc(clDocs.scoreDocs[i].doc), clDocs.scoreDocs[i].doc));
            	maxClDistance = clScoreDocs.last().getDistance();
            } else if (clTmpDistance < maxClDistance) {
            	// if it is nearer to the sample than at least one of the current set:
            	// remove the last one ...
            	clScoreDocs.remove(clScoreDocs.last());
            	// add the new one ...
            	clScoreDocs.add(new SimpleResult(clTmpDistance, searcher.doc(clDocs.scoreDocs[i].doc), clDocs.scoreDocs[i].doc));
            	// and set our new distance border ...
            	maxClDistance = clScoreDocs.last().getDistance();
            }
        }
        res.add("ReRankCLTime", System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		
        // load interest points from document
    	// has to be loaded before getVisualWords (this method delete surf interest points)
    	ArrayList<SurfInterestPoint> suPoints = new ArrayList<SurfInterestPoint>();
    	IndexableField[] queryFields = suFeat.getFields(DocumentBuilder.FIELD_NAME_SURF);
    	for (IndexableField queryField : queryFields) {
    		SurfFeature feature = new SurfFeature();
    		feature.setByteArrayRepresentation(queryField.binaryValue().bytes, queryField.binaryValue().offset, queryField.binaryValue().length);
			SurfInterestPoint sip = new SurfInterestPoint(feature.descriptor);
			suPoints.add(sip);
		}
    	
    	// Get Visual Words
    	suFeat = sh.getVisualWords(suFeat);
		
		/* SURF */
		Query suQuery = qp.parse(suFeat.getValues(DocumentBuilder.FIELD_NAME_SURF_VISUAL_WORDS)[0]);
		
		// Searching..
        // Surf
        TopDocs suDocs = searcher.search(suQuery, numSurfSimImages);
        res.add("SearchSurfTime", System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
        res.add("RawDocsCount", clDocs.scoreDocs.length + suDocs.scoreDocs.length + "");
        
        // initialization of LSHHashTable
        LSHHashTable.initialize();
        // Re-rank by surf method
        TreeSet<SimpleResult> resultScoreDocs = new TreeSet<SimpleResult>();
    	// Re-rank color layout
    	for (SimpleResult r : clScoreDocs) {
			rerank(suPoints, r.getDocument(), r.getIndexNumber(), reader, resultScoreDocs, numSimImages);
		}
    	// Re-rank surf (visual words)
    	for (int i = 0; i < suDocs.scoreDocs.length; i++) {
    		Document doc = reader.document(suDocs.scoreDocs[i].doc);
    		rerank(suPoints, doc, suDocs.scoreDocs[i].doc, reader, resultScoreDocs, numSimImages);
        }
    	time = System.currentTimeMillis() - time;
        res.add("ReRankSurfTime", time + "");
        
        LinkedList<HashMap<String, String>> result = new LinkedList<HashMap<String, String>>();
        for (SimpleResult r : resultScoreDocs) {
			HashMap<String, String> map = new HashMap<String, String>(2);
			map.put("id", r.getDocument().get("id"));
			map.put("d", "" + r.getDistance());
			result.add(map);
		}
        res.add("docs", result);
	}
	
	private void rerank(ArrayList<SurfInterestPoint> query, Document doc, int indexNumber, IndexReader reader, TreeSet<SimpleResult> resultScoreDocs, int numSimImages) throws IOException, ClassNotFoundException {
		
		float maxDistance = resultScoreDocs.isEmpty()? -1 : resultScoreDocs.last().getDistance();
		
		// load hash from document
		BinaryDocValues binaryValues = MultiDocValues.getBinaryValues(reader, "su_hi");
    	BytesRef bytesRef = new BytesRef();
    	binaryValues.get(indexNumber,bytesRef);
    	
    	ByteArrayInputStream docInputStream = new ByteArrayInputStream(bytesRef.bytes, bytesRef.offset, bytesRef.length);
    	ObjectInputStream docOis = new ObjectInputStream(docInputStream);
    	docOis.close();
    	LSHHashTable docTable = (LSHHashTable) docOis.readObject();
    	
    	float tmpScore = SurfUtils.getDistance(query, docTable);
        if (resultScoreDocs.size() < numSimImages) {
        	resultScoreDocs.add(new SimpleResult(tmpScore, doc, indexNumber));
        } else if (tmpScore < maxDistance) {
        	// if it is nearer to the sample than at least one of the current set:
        	// remove the last one ...
        	resultScoreDocs.remove(resultScoreDocs.last());
        	// add the new one ...
        	resultScoreDocs.add(new SimpleResult(tmpScore, doc, indexNumber));
        	// and set our new distance border ...
        	maxDistance = resultScoreDocs.last().getDistance();
        }
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
