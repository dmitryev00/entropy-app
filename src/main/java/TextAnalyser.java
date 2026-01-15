import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TextAnalyser {
	protected static String alphabets = "абвгдежзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyz";
	private final Map<Character, Integer> alphabet;
	private final Map<String, Integer> bigram;
	private final String normalized;
	private final double defaultEntropy;
	private final double markovEntropy;
	private final int length;


	public double getMarkovEntropy() {
		return markovEntropy;
	}

	public double getDefaultEntropy() {
		return defaultEntropy;
	}

	public String getNormalized() {
		return normalized;
	}

	public Map<String, Integer> getBigram() {
		return bigram;
	}

	public Map<Character, Integer> getAlphabet() {
		return alphabet;
	}

	public TextAnalyser(String text) {
		normalized = normalizeText(text);
		length = normalized.length();
		alphabet = setLetterMap(normalized);
		bigram = createBigramMap(normalized);
		defaultEntropy = defaultEntropy();
		markovEntropy = markovEntropy();

	}

	private double defaultEntropy() {
		Map<Character, Integer> alb = alphabet;
		double entropy = 0;
		for(char c : alb.keySet()) {
			if(alb.get(c) == 0) continue;
			double p_i = (double) alb.get(c) / length;
			double log2p = Math.log(1 / p_i) / Math.log(2.0);
			entropy += p_i * log2p;
		}
		return entropy;
	}

	private double markovEntropy() {
		double entropy = 0;
		for(String key : bigram.keySet()) {
			double p_i = (double) bigram.get(key) / length;
			double condProb = conditionalProbability(key);
			if(condProb > 0) {
				double log2 = Math.log(1 / condProb)/ Math.log(2);
				entropy += p_i * log2;
			}
		}
		return entropy;
	}

	private static String normalizeText(String text) {
		StringBuilder result = new StringBuilder();
		for(char c : text.toCharArray()) {
			if(alphabets.contains(Character.toString(c).toLowerCase())) {
				result.append(Character.toLowerCase(c));
			}
		}
		return result.toString();
	}

	private static Map<String, Integer> createBigramMap(String text) {
		Map<String, Integer> bigramMap = new HashMap<>();
		for(int i = 0; i <= text.length() - 1; i++) {
			String bigram;
			if(i == text.length() - 1) {
				bigram = text.charAt(i) + "" + text.charAt(0);
			} else {
				bigram = text.substring(i, i + 2);
			}
			bigramMap.merge(bigram, 1, Integer::sum);
		}
		return bigramMap;
	}

	private Map<Character, Integer> setLetterMap(String text) {
		Map<Character, Integer> letterMap = setDefaultMap(text);
		for(char c : text.toCharArray()) {
			letterMap.merge(c, 1, Integer::sum);
		}
		return letterMap;
	}

	public double conditionalProbability(String bigram) {
		Integer countAB = this.bigram.get(bigram);
		if (countAB == null) return 0.0;
		Integer countA = alphabet.get(bigram.charAt(0));
		if (countA == null || countA == 0) return 0.0;
		double p_AB = (double) this.bigram.get(bigram) / length;
		double p_A = (double) alphabet.get(bigram.charAt(0)) / length;
		return p_AB / p_A;
	}

	private static Map<Character, Integer> setDefaultMap(String normalized)
	{
		Map<Character, Integer> defaultMap;
		List<Character> englishLetters = List.of(
				'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
				'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
		);
		List<Character> russianLetters = List.of(
				'а', 'б', 'в', 'г', 'д', 'е', 'ж', 'з', 'и', 'й', 'к', 'л', 'м',
				'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ъ',
				'ы', 'ь', 'э', 'ю', 'я'
		);
		if(String.valueOf(normalized.charAt(0)).matches("[а-яА-Я]"))
		{
			defaultMap = russianLetters.stream().collect(Collectors.toMap(
					letter -> letter,
					letter -> 0,
					(e1, e2) -> e1,
					LinkedHashMap::new
			));
		}
		else
		{
			defaultMap = englishLetters.stream().collect(Collectors.toMap(
					letter -> letter,
					letter -> 0,
					(e1, e2) -> e1,
					LinkedHashMap::new
			));
		}
		return defaultMap;
	}
}