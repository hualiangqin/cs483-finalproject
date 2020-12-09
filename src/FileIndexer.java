/*
 * Author: Ryan Qin
 * Description: this file use Lucene to build up the index in a directory
 * before each document is written by indexWriter, the document is lemmatized by CoreNLP 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.clulab.discourse.rstparser.DiscourseTree;
import org.clulab.processors.fastnlp.FastNLPProcessor;
import org.clulab.processors.Processor;
import org.clulab.processors.Sentence;
import org.clulab.processors.corenlp.CoreNLPProcessor;
import org.clulab.struct.CorefMention;
import org.clulab.struct.DirectedGraphEdgeIterator;

public class FileIndexer {    
	
	public static void main(String[] args) throws Exception {        
		String indexDir = "index/";
        File dataDir = new File("");
    	FileIndexer indexer = new FileIndexer();        
        indexer.index(indexDir, dataDir);
    }
    
    private void index(String indexDir, File dataDir) throws Exception { 
    	ClassLoader classLoader = getClass().getClassLoader();
        Directory dir =  FSDirectory.open(Paths.get(classLoader.getResource(indexDir).toURI()));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
    	IndexWriter indexWriter = new IndexWriter(dir, config);   
    	
		String inputFilePath = "wiki-subset-20140602";
        File[] files = new File(classLoader.getResource(inputFilePath).getFile()).listFiles();
		for (File file : files) {
    		indexFileWithIndexWriter(indexWriter, file);
		}
//		File file = new File(classLoader.getResource("wiki-subset-20140602/enwiki-20140602-pages-articles.xml-0005.txt").getFile());
//		indexFileWithIndexWriter(indexWriter, file);
		
		System.out.println("index finished");
        indexWriter.close();             
    }
    
    public String lemmatizeString(String raw) {
    	String result = "";
//    	Processor proc = new FastNLPProcessor(true, false, false, 1);
    	Processor proc = new CoreNLPProcessor(true, true, false, 0, 10000);
    	org.clulab.processors.Document doc = proc.mkDocument(raw, false);
		proc.tagPartsOfSpeech(doc);
		proc.lemmatize(doc);
    	for (Sentence sentence: doc.sentences()) {
    		if (sentence.lemmas().isDefined()) {
    			result += String.join(" ", sentence.lemmas().get());
    		}
    	}
    	
    	return result;
    }
    
    private void indexFileWithIndexWriter(IndexWriter indexWriter, File file) throws IOException {
    	System.out.println("Indexing file:... " + file.getName());
		HashMap<String, String> wikiPages = readFile(file);
    	
		for (String title: wikiPages.keySet()) {
			Document doc = new Document();
			//get the lemma of the document
			String lemmaString = lemmatizeString(wikiPages.get(title));
	        doc.add(new TextField("document", lemmaString, Field.Store.YES));

	        // use a string field for doc Name because we don't want it tokenized
	        doc.add(new StringField("title", title, Field.Store.YES));
	        indexWriter.addDocument(doc);
		}
        
    }
    
    public HashMap<String, String> readFile(File file) {
		HashMap<String, String> wikiPages = new HashMap<String, String>();
		try {
			Scanner reader = new Scanner(file);
			String title = "";
			String document = "";
			while (reader.hasNext()) {
				String line = reader.nextLine();
				if (line.startsWith("[[") && line.endsWith("]]")) {
					// meaning that we already have title and now we hit the next one
					if (!title.equals("")) {
						wikiPages.put(title, document);
					}
					// update the title and document
					title = line.substring(2, line.length()-2);
					document = "";
				}else {
					// update the document
					document += line;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return wikiPages;
	}
}