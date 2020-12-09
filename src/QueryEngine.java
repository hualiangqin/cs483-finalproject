import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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

public class QueryEngine {
	String indexDir = "index_lemma/";
	String indexDir1 = "index/";
	String test = "questions.txt";
	ClassLoader classLoader = getClass().getClassLoader();
	
	public QueryEngine () {}
	
	public static void main(String[] args) throws IOException, URISyntaxException {
		
		// choose this if want to run test on lemmatized index and query or not
		Boolean lemma = false;
//		Boolean lemma = true;
		
		// choose default BM25 scoring function or tf-idf
		Boolean tfidf = false;
//		Boolean tfidf = true;
		
		if (args.length == 2) {
			if (args[0].equals("true")) {
				lemma = true;
			}
			if (args[0].equals("true")) {
				tfidf = true;
			}
		}
		
		runTest(lemma, tfidf);

	}
	
	public static void runTest(Boolean lemma, Boolean tfidf) throws IOException, URISyntaxException {
		QueryEngine qe = new QueryEngine();
		HashMap<String, String> answerQuestion = qe.readQuestion();
		int top100 = 0;
		int top1 = 0;
		double mrr = 0.0;
		ArrayList<String> correctAns = new ArrayList<String>();
		ArrayList<String> wrongAns = new ArrayList<String>();
		for (String answer: answerQuestion.keySet()) {
			String query = answerQuestion.get(answer);
			ScoreDoc[] hits = qe.getHits(query, new StandardAnalyzer(), lemma, tfidf);
	        List<ResultClass> ans = qe.returnResults(hits, lemma);
			ArrayList<String> titles = qe.getTitles(ans);
			if (titles.contains(answer)) {
				top100++;
				for (int i=0; i<titles.size(); i++) {
					if (titles.get(i).equals(answer)) {
						mrr += 1/(i+1);
						if (i<=10) {
//							correctAns.add(answer);
						}else {
//							wrongAns.add(answer);
							System.out.println("Correct: " + answer);
							System.out.println("Wrong: " + titles.get(0));
							System.out.println();
						}
						break;
					}
				}
			}
			if (titles.get(0).equals(answer)) {
				top1++;
			}
		}
		
		if (lemma) {
			System.out.println("Using lemmatized index and query");
		}else {
			System.out.println("Using index and query without lemmatization");
		}
		
		if (tfidf) {
			System.out.println("Changed default BM25 scoring function to tf-idf similarity function");
		}else {
			System.out.println("Using default BM25 scoring function");
		}
		
		mrr = mrr/100;
		System.out.println(top100 + " question have its answer in the top 100 ranked potential answers");
		System.out.println(top1 + " question have its answer ranked at top 1");
		System.out.println("MRR scorce: " + mrr);
		
//		System.out.println(correctAns);
//		System.out.println(wrongAns);
	}
	
	public ArrayList<String> getTitles(List<ResultClass> ansList) {
		ArrayList<String> titles = new ArrayList<String>();
		for (ResultClass r: ansList) {
//        	System.out.println(r.DocName.get("title") + " " + r.docScore);
			titles.add(r.DocName.get("title"));
        }
		return titles;
	}
	
	public HashMap<String, String> readQuestion() {
		HashMap<String, String> answerQuestion = new HashMap<String, String>();
		try {
			File file = new File(classLoader.getResource(test).getFile());
			Scanner scanner = new Scanner(file);
			String category = "";
			String clue = "";
			String answer = "";
			while (scanner.hasNext()) {
				category = scanner.nextLine();
				clue = scanner.nextLine();
				answer = scanner.nextLine();
				scanner.nextLine();
				answerQuestion.put(answer, clue + " " + category);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return answerQuestion;
	}

    private List<ResultClass> returnResults(ScoreDoc[] hits, Boolean lemma) throws IOException, URISyntaxException {
    	
        if (hits.length > 0) {
        	Directory dir;
        	if (lemma) {
        		dir =  FSDirectory.open(Paths.get(classLoader.getResource(indexDir).toURI()));
        	}else {
        		dir =  FSDirectory.open(Paths.get(classLoader.getResource(indexDir1).toURI()));
        	}
        	IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
        	
	        List<ResultClass> doc_score_list = new ArrayList<ResultClass>();
	            for (int i = 0; i < hits.length; ++i) {
	                ResultClass objResultClass= new ResultClass();
	                Document d = searcher.doc(hits[i].doc);
	                objResultClass.DocName = d;
	                objResultClass.docScore = hits[i].score;
	                doc_score_list.add(objResultClass);
	            }
	        reader.close();
	        return doc_score_list;
        }else {
        	return null;
        }
    }
    
    public static String tokenizeString(Analyzer analyzer, String string) {
        List<String> result = new ArrayList<String>();
        try {
        	TokenStream stream  = analyzer.tokenStream(null, new StringReader(string));
        	stream.reset();
        	while (stream.incrementToken()) {
        		result.add(stream.getAttribute(CharTermAttribute.class).toString());
        	}
        	stream.close();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        String[] resArray = new String[result.size()];
        return String.join(" ", result.toArray(resArray));
    }
    
    public String lemmatizeQuery(String query) {
    	String result = "";
    	Processor proc = new CoreNLPProcessor(true, true, false, 0, 10000);
    	org.clulab.processors.Document doc = proc.mkDocument(query, false);
		proc.tagPartsOfSpeech(doc);
		proc.lemmatize(doc);
    	for (Sentence sentence: doc.sentences()) {
    		if (sentence.lemmas().isDefined()) {
    			result += String.join(" ", sentence.lemmas().get());
    		}
    	}
		return result;
    }
	
	private ScoreDoc[] getHits(String query, StandardAnalyzer analyzer, Boolean lemma, Boolean tfidf){
    	ScoreDoc[] hits = {};
    	
    	try {
    		String querystr = tokenizeString(analyzer, query);
    		Directory dir;
    		if (lemma) {
        		querystr = lemmatizeQuery(querystr);
        		dir =  FSDirectory.open(Paths.get(classLoader.getResource(indexDir).toURI()));
    		}else {
    			dir =  FSDirectory.open(Paths.get(classLoader.getResource(indexDir1).toURI()));
    		} 
			QueryParser parser = new QueryParser("document", analyzer);
			parser.setSplitOnWhitespace(true);
			Query q = parser.parse(querystr);
			
			int hitsPerPage = 100;
	        IndexReader reader = DirectoryReader.open(dir);
	        IndexSearcher searcher = new IndexSearcher(reader);
	        if (tfidf) {
	        	searcher.setSimilarity(new ClassicSimilarity());
	        }
	        TopDocs docs = searcher.search(q, hitsPerPage);
	        hits = docs.scoreDocs;
	        
	        reader.close();
			
		} catch (ParseException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
    	
    	return hits;
    }
	

}