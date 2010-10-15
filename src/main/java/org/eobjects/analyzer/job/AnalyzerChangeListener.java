package org.eobjects.analyzer.job;


public interface AnalyzerChangeListener {

	public void onAdd(ExploringAnalyzerJobBuilder<?> analyzerJobBuilder);
	
	public void onAdd(RowProcessingAnalyzerJobBuilder<?> analyzerJobBuilder);

	public void onRemove(ExploringAnalyzerJobBuilder<?> analyzerJobBuilder);
	
	public void onRemove(RowProcessingAnalyzerJobBuilder<?> analyzerJobBuilder);
}
