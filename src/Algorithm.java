import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Item;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.Sequence;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.database.SequenceStatsGenerator;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.Predictor;
import ca.pfv.spmf.algorithms.sequenceprediction.ipredict.predictor.CPT.CPTPlus.CPTPlusPredictor;

public class Algorithm {

	public static void main(String[] arg) throws IOException, ClassNotFoundException, Exception {

		// Uncomment this line to train the model
		// Comment this line to only load the trained model
		loadAndTrain();

		CPTPlusPredictor predictionModel = loadTrainedModel();

		// Now we will make a prediction.
		// We want to predict what would occur after the sequence <1, 2>.
		// We first create the sequence
		Sequence sequence = new Sequence(0);
		sequence.addItem(new Item(442));
		sequence.addItem(new Item(423));
		sequence.addItem(new Item(347));
		// sequence.addItem(new Item(347));
		// sequence.addItem(new Item(347));

		// We also need to specify the size of the prediction (how many symbols to 
		// predict). Here we will predict 5 symbol.
		// int size = sequence.size();
		int size = 5;

		// Then we perform the prediction
		Sequence thePrediction = predictionModel.customePredict(sequence,size);
		System.out.println(
				"For the sequence <(" + sequence.toString() + ")>, the prediction for the next " + size + " symbol is:"
						+ thePrediction);

		// Print the count table by ascending order
		// printSortedCountTable(predictionModel);

		// ------------------------------------------------------------------------------//
		// End of main program //
	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = Algorithm.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}

	// Print the training sequences to the console
	public static void printTrainingSequences(SequenceDatabase trainingSet) throws Exception {
		for (Sequence sequence : trainingSet.getSequences()) {
			System.out.println(sequence.toString());
		}
		System.out.println();
	}

	// ========================= OPTIONAL ========================== //
	// ============================================================= //
	// ******* IF we want to save the trained model to a file ******* ///
	public static void saveTrainedModel(CPTPlusPredictor predictionModel) throws Exception {
		ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream("model.ser"));
		stream.writeObject(predictionModel);
		stream.close();
	}

	// ****** Then, we can also load the trained model from the file ****** ///
	public static CPTPlusPredictor loadTrainedModel() throws Exception {
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream("model.ser"));
		CPTPlusPredictor predictionModel = (CPTPlusPredictor) stream.readObject();
		stream.close();
		return predictionModel;
	}

	// If we want to see why that prediction was made, we can also
	// ask to see the count table of the prediction algorithm. The
	// count table is a structure that stores the score for each symbols
	// for the last prediction that was made. The symbol with the highest
	// score was the prediction.
	public static void printCountTable(CPTPlusPredictor predictionModel) {
		System.out.println();
		System.out.println("To make the prediction, the scores were calculated as follows:");
		Map<Integer, Float> countTable = predictionModel.getCountTable();
		for (Entry<Integer, Float> entry : countTable.entrySet()) {
			System.out.println("symbol (" + entry.getKey() + ")\t score: " + entry.getValue());
		}
	}

	public static void printSortedCountTable(CPTPlusPredictor predictionModel) {
		System.out.println();
		System.out.println("To make the prediction, the scores were calculated as follows:");
		Map<Integer, Float> sorted = sortByValue(predictionModel.getCountTable());
		for (Entry<Integer, Float> entry : sorted.entrySet()) {
			System.out.println("symbol (" + entry.getKey() + ")\t score: " + entry.getValue());
		}
	}

	public static void loadAndTrain() throws IOException, ClassNotFoundException, Exception {
		// Load the set of training sequences
		String inputPath = fileToPath("Dataset.txt");
		SequenceDatabase trainingSet = new SequenceDatabase();
		trainingSet.loadFileSPMFFormat(inputPath, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
		// trainingSet.loadFileCustomFormat(inputPath, Integer.MAX_VALUE, 0,
		// Integer.MAX_VALUE);
		// Print statistics about the training sequences
		SequenceStatsGenerator.prinStats(trainingSet, " training sequences ");

		// The following line is to set optional parameters for the prediction model.
		// We want to
		// activate the CCF and CBS strategies which generally improves its performance
		// (see paper)
		String optionalParameters = "CCF:true CBS:true CCFmin:1 CCFmax:6 CCFsup:2 splitMethod:0 splitLength:4 minPredictionRatio:1.0 noiseRatio:1.0";
		// Here is a brief description of the parameter used in the above line:
		// CCF:true --> activate the CCF strategy
		// CBS:true --> activate the CBS strategy
		// CCFmax:6 --> indicate that the CCF strategy will not use pattern having more
		// than 6 items
		// CCFsup:2 --> indicate that a pattern is frequent for the CCF strategy if it
		// appears in at least 2 sequences
		// splitMethod:0 --> 0 : indicate to not split the training sequences 1:
		// indicate to split the sequences
		// splitLength:4 --> indicate to split sequence to keep only 4 items, if
		// splitting is activated
		// minPredictionRatio:1.0 --> the amount of sequences or part of sequences that
		// should match to make a prediction, expressed as a ratio
		// noiseRatio:1.0 --> ratio of items to remove in a sequence per level (see
		// paper).

		// Train the prediction model
		CPTPlusPredictor predictionModel = new CPTPlusPredictor("CPT+", optionalParameters);
		predictionModel.Train(trainingSet.getSequences());
		// Uncomment this line to print the training sequences to the console but it may be very long
		// printTrainingSequences(trainingSet);
		saveTrainedModel(predictionModel);
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
		list.sort(Entry.comparingByValue());

		// Collections.reverseOrder() for reverse ordering
		// list.sort(Collections.reverseOrder(Entry.comparingByValue()));

		Map<K, V> result = new LinkedHashMap<>();
		for (Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}
}
