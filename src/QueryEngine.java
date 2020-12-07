
import java.io.File;

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

public class QueryEngine {
	
	public QueryEngine () {}
	
	private void buildIndex() {
		String inputFilePath = "wiki-subset-20140602";
		ClassLoader classLoader = getClass().getClassLoader();
        File[] files = new File(classLoader.getResource(inputFilePath).getFile()).listFiles();
        
        showFiles(files);
	}
	
	public void showFiles(File[] files) {
		int n = 0;
	    for (File file : files) {
	        if (file.isDirectory()) {
	            System.out.println("Directory: " + file.getName());
	            showFiles(file.listFiles()); // Calls same method again.
	        } else {
//	            System.out.println("File: " + file.getName());
	            n++;
	        }
	    }
	    System.out.println(n);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		QueryEngine qe = new QueryEngine();
		qe.buildIndex();
	}

}