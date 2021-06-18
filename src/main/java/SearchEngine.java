import java.io.IOException;
import java.nio.file.Path;

public class SearchEngine {
	private final InvertedIndex index;
	private final WordStemCollector stemCollector;
	private final IndexSearcher searcher;
	
	public SearchEngine(ArgumentMap argMap) {
		this.index = new InvertedIndex();
		this.stemCollector = new WordStemCollector( this.index );
		this.searcher = new IndexSearcher( index, argMap.hasFlag("-exact") );
	}
	
	public void parseFilesFrom(Path seed) throws IOException {
		stemCollector.collectStemsFrom(seed);
	}
	
	public void searchFrom(Path queryPath, boolean exact) throws IOException {
		searcher.search(queryPath, exact);
	}
	
	public void outputIndexTo(Path path) throws IOException {
		index.toJson(path);
	}
	
	public void outputWordCountsTo(Path path) throws IOException {
		index.stringCountsToJson(path);
	}
	
	public void outputResultsTo(Path path) throws IOException {
		searcher.outputToFile(path);
	}
}
