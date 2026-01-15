import java.util.HashMap;
import java.util.Map;

public class PairedTextAnalyser{

	private final TextAnalyser firstAnalyser;
	private final TextAnalyser secondAnalyser;
	private final Map<String, Integer> jointMap;
	private final double conditionalEntropy;
	private final double jointEntropy;

	public Map<Character, Integer> getFirstAlphabet() {
		return firstAnalyser.getAlphabet();
	}

	public Map<Character, Integer> getSecondAlphabet() {
		return secondAnalyser.getAlphabet();
	}

	public Map<String, Integer> getJointMap() {
		return jointMap;
	}

	public String getFirstNormalized() {
		return firstAnalyser.getNormalized();
	}

	public String getSecondNormalized() {
		return secondAnalyser.getNormalized();
	}

	public double getConditionalEntropy() {
		return conditionalEntropy;
	}

	public double getJointEntropy() {
		return jointEntropy;
	}


	public PairedTextAnalyser(String firstText, String secondText)
	{
		this.firstAnalyser = new TextAnalyser(firstText);
		this.secondAnalyser = new TextAnalyser(secondText);
		jointMap = createBigramMap(firstAnalyser.getNormalized(), secondAnalyser.getNormalized());
		conditionalEntropy = conditionalEntropy();
		jointEntropy = jointEntropy();
	}


	private static Map<String, Integer> createBigramMap(String firstText, String secondText){
		Map<String, Integer> bigramMap = new HashMap<>();
		int length = Integer.min(firstText.length(), secondText.length());
		for(int i = 0; i < length; i++)
		{
			String bigram = firstText.charAt(i) + "" + secondText.charAt(i);
			bigramMap.merge(bigram, 1, Integer::sum);
		}
		return bigramMap;
	}

	private double conditionalEntropy() {
		double entropy = 0;
		int length = Integer.min(firstAnalyser.getNormalized().length(), secondAnalyser.getNormalized().length());

		for (String key : jointMap.keySet()) {
			double p_AB = (double) jointMap.get(key) / length;
			double p_A_given_B = conditionalProbability(key);

			if (p_A_given_B > 0) {
				double log2 = Math.log(1 / p_A_given_B) / Math.log(2.0);
				entropy += p_AB * log2;
			}
		}
		return entropy;
	}


	private double conditionalProbability(String bigram)
	{
		char b = bigram.charAt(1);
		int length = Integer.min(firstAnalyser.getNormalized().length(), secondAnalyser.getNormalized().length());
		double p_AB = (double) jointMap.get(bigram) / length;
		double p_B = (double) secondAnalyser.getAlphabet().get(b) / length;
		return p_AB/p_B;
	}


	private double jointEntropy()
	{
		double entropy = 0;
		for(String key : jointMap.keySet())
		{
			int length = Integer.min(firstAnalyser.getNormalized().length(), secondAnalyser.getNormalized().length());
			double p_AB = (double) jointMap.get(key) / length;
			double log2 = Math.log(1/p_AB) / Math.log(2.0);
			entropy += p_AB * log2;
		}
		return entropy;
	}

}