import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);
	private final String[] loadedTexts;
	private final int width, height;
	private JFrame frame;
	private int currentTableIndex = 0;

	public App(int width, int height) {
		this.width = width;
		this.height = height;
		this.frame = mainFrame();
		this.loadedTexts = new String[12];
	}

	private JFrame mainFrame() {
		frame = new JFrame("Frequency analyser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		frame.setBounds(screenSize.width/2 - width/2, screenSize.height/2 - height/2, width, height);

		JPanel start = createTextsPanel();
		JMenuBar menu = menuBar();
		frame.setResizable(true);
		frame.add(start);
		frame.setJMenuBar(menu);
		frame.setVisible(true);
		return frame;
	}

	public JMenuBar menuBar() {
		JMenuBar menuBar = new JMenuBar();
		String[] buttons = {"Тексты", "Диаграммы", "Анализ одного текста", "Анализ двух текстов"};
		String[] actions = {"text", "diagram", "analysis1", "analysis2"};

		for (int i = 0; i < buttons.length; i++) {
			JButton button = new JButton(buttons[i]);
			String action = actions[i];
			button.addActionListener(e -> showPanel(action));
			menuBar.add(button);
		}
		return menuBar;
	}

	public void showPanel(String panelType) {
		frame.getContentPane().removeAll();

		switch(panelType) {
			case "text": frame.add(createTextsPanel()); break;
			case "diagram": frame.add(createDiagramPanel()); break;
			case "analysis1": frame.add(createAnalysisPanel1()); break;
			case "analysis2": frame.add(createAnalysisPanel2()); break;
		}
		frame.revalidate();
		frame.repaint();
	}

	private JPanel createAnalysisPanel1() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel cardsPanel = new JPanel(new CardLayout());

		for (int i = 0; i < 12; i++) {
			JPanel cardPanel;
			if(loadedTexts[i] != null) {
				TextAnalyser textAnalyser = new TextAnalyser(loadedTexts[i]);
				cardPanel = createCardPanelWithTabs(textAnalyser);
			} else {
				cardPanel = createEmptyCardPanel();
			}
			cardsPanel.add(cardPanel, "panel_" + i);
		}

		mainPanel.add(createNavigationPanel(cardsPanel), BorderLayout.NORTH);
		mainPanel.add(cardsPanel, BorderLayout.CENTER);
		return mainPanel;
	}

	private JPanel createCardPanelWithTabs(TextAnalyser analyser) {
		JPanel panel = new JPanel(new BorderLayout());

		// Создаем вкладки
		JTabbedPane tabbedPane = new JTabbedPane();

		// Вкладка 1: Частоты биграмм
		JTable freqTable = createCombinedTable(analyser.getBigram(), analyser.getAlphabet(),
				analyser.getNormalized().length(), "freq");
		tabbedPane.addTab("Частоты биграмм", new JScrollPane(freqTable));

		// Вкладка 2: Условные вероятности
		JTable condTable = createCombinedTable(analyser.getBigram(), analyser.getAlphabet(),
				analyser.getNormalized().length(), "conditional");
		tabbedPane.addTab("Условные вероятности", new JScrollPane(condTable));

		// Вкладка 3: Частоты символов
		JTable charTable = createCharFrequencyTable(analyser.getAlphabet(), analyser.getNormalized().length());
		tabbedPane.addTab("Частоты символов", new JScrollPane(charTable));

		// Текст с результатами
		JTextArea text = createResultText(analyser.getNormalized().length(),
				analyser.getDefaultEntropy(), analyser.getMarkovEntropy());

		panel.add(tabbedPane, BorderLayout.CENTER);
		panel.add(new JScrollPane(text), BorderLayout.SOUTH);
		return panel;
	}

	private JTable createCharFrequencyTable(Map<Character, Integer> alphabet, int length) {
		if (alphabet == null || alphabet.isEmpty()) {
			return new JTable(new Object[][]{{"Нет данных"}}, new String[]{"Нет данных"});
		}

		List<Character> chars = new ArrayList<>(new TreeSet<>(alphabet.keySet()));
		Object[][] data = new Object[chars.size()][2];
		String[] columns = {"Символ", "Частота %"};

		for (int i = 0; i < chars.size(); i++) {
			Character ch = chars.get(i);
			double freq = (double) alphabet.get(ch) / length * 100;
			data[i][0] = ch;
			data[i][1] = String.format("%.3f", freq);
		}

		return createStyledTable(data, columns, "char");
	}

	private JPanel createAnalysisPanel2() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		JComboBox<String> firstSelector = new JComboBox<>();
		JComboBox<String> secondSelector = new JComboBox<>();

		setupComboBoxes(firstSelector, secondSelector);

		JPanel comparisonPanel = new JPanel(new GridLayout(1, 1));
		JPanel entropyArea = new JPanel(new BorderLayout());

		ActionListener comparisonListener = e -> updateComparison(
				firstSelector, secondSelector, comparisonPanel, entropyArea);

		firstSelector.addActionListener(comparisonListener);
		secondSelector.addActionListener(comparisonListener);
		comparisonListener.actionPerformed(null);

		mainPanel.add(createControlPanel(firstSelector, secondSelector), BorderLayout.NORTH);
		mainPanel.add(comparisonPanel, BorderLayout.CENTER);
		mainPanel.add(new JScrollPane(entropyArea), BorderLayout.SOUTH);

		return mainPanel;
	}


	private JPanel createEmptyCardPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("Нет данных для анализа", JLabel.CENTER), BorderLayout.CENTER);
		return panel;
	}

	private JPanel createNavigationPanel(JPanel cardsPanel) {
		JPanel controls = new JPanel();
		JButton prev = new JButton("← Пред");
		JButton next = new JButton("След →");
		JLabel titleLabel = new JLabel("Анализ 1");

		prev.addActionListener(e -> navigate(-1, cardsPanel, titleLabel));
		next.addActionListener(e -> navigate(1, cardsPanel, titleLabel));

		controls.add(prev);
		controls.add(titleLabel);
		controls.add(next);
		return controls;
	}

	private void navigate(int direction, JPanel cardsPanel, JLabel titleLabel) {
		currentTableIndex = (currentTableIndex + direction + 12) % 12;
		CardLayout cl = (CardLayout) cardsPanel.getLayout();
		cl.show(cardsPanel, "panel_" + currentTableIndex);
		titleLabel.setText("Анализ " + (currentTableIndex + 1));
	}

	private void setupComboBoxes(JComboBox<String> first, JComboBox<String> second) {
		for (int i = 0; i < 12; i++) {
			String status = loadedTexts[i] != null ? "✓" : "✗";
			String item = "Текст " + (i + 1) + " " + status;
			first.addItem(item);
			second.addItem(item);
		}
	}

	private JPanel createControlPanel(JComboBox<String> first, JComboBox<String> second) {
		JPanel controls = new JPanel();
		controls.add(new JLabel("Сравнить:"));
		controls.add(first);
		controls.add(new JLabel(" с "));
		controls.add(second);
		return controls;
	}

	private void updateComparison(JComboBox<String> firstSelector, JComboBox<String> secondSelector,
								  JPanel comparisonPanel, JPanel entropyArea) {
		try {
			int firstIndex = firstSelector.getSelectedIndex();
			int secondIndex = secondSelector.getSelectedIndex();

			comparisonPanel.removeAll();
			entropyArea.removeAll();

			if(loadedTexts[firstIndex] != null && loadedTexts[secondIndex] != null) {
				PairedTextAnalyser pairedAB = new PairedTextAnalyser(loadedTexts[firstIndex], loadedTexts[secondIndex]);
				PairedTextAnalyser pairedBA = new PairedTextAnalyser(loadedTexts[secondIndex], loadedTexts[firstIndex]);
				TextAnalyser soloA = new TextAnalyser(loadedTexts[firstIndex]);
				TextAnalyser soloB = new TextAnalyser(loadedTexts[secondIndex]);

				setupComparisonEntropies(comparisonPanel, pairedAB, pairedBA, soloA, soloB);
				setupIndividualEntropies(entropyArea, soloA, soloB);
			} else {
				showErrorMessage(comparisonPanel, entropyArea);
			}

			comparisonPanel.revalidate();
			entropyArea.revalidate();

		} catch (Exception ex) {
			log.error("e: ", ex);
			showErrorMessage(comparisonPanel, entropyArea);
		}
	}

	private void setupComparisonEntropies(JPanel comparisonPanel, PairedTextAnalyser pairedAB,
										  PairedTextAnalyser pairedBA, TextAnalyser soloA, TextAnalyser soloB) {
		comparisonPanel.setLayout(new BorderLayout());

		// Энтропии сравнения в виде карточек
		JPanel entropyCards = new JPanel(new GridLayout(1, 3, 15, 0));
		entropyCards.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		entropyCards.add(createEntropyCard("H(A|B)",
				pairedAB.getConditionalEntropy() == 0 ? soloA.getMarkovEntropy() : pairedAB.getConditionalEntropy(),
				"Условная энтропия A при B"));

		entropyCards.add(createEntropyCard("H(B|A)",
				pairedBA.getConditionalEntropy() == 0 ? soloB.getMarkovEntropy() : pairedBA.getConditionalEntropy(),
				"Условная энтропия B при A"));

		entropyCards.add(createEntropyCard("H(A,B)", pairedAB.getJointEntropy(), "Совместная энтропия"));

		comparisonPanel.add(entropyCards, BorderLayout.CENTER);
	}

	private void setupIndividualEntropies(JPanel entropyArea, TextAnalyser soloA, TextAnalyser soloB) {
		entropyArea.setLayout(new BorderLayout());

		JPanel individualPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		individualPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

		individualPanel.add(createTextPanel(soloA, "Текст A"));
		individualPanel.add(createTextPanel(soloB, "Текст B"));

		entropyArea.add(individualPanel, BorderLayout.CENTER);
	}

	private JPanel createEntropyCard(String title, double value, String description) {
		JPanel card = new JPanel(new BorderLayout());
		card.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY, 1),
				BorderFactory.createEmptyBorder(15, 15, 15, 15)
		));
		card.setBackground(Color.WHITE);

		JLabel titleLabel = new JLabel(title, JLabel.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

		JLabel valueLabel = new JLabel(String.format("%.3f", value), JLabel.CENTER);
		valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
		valueLabel.setForeground(Color.BLUE);

		JLabel descLabel = new JLabel(description, JLabel.CENTER);
		descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		descLabel.setForeground(Color.GRAY);

		card.add(titleLabel, BorderLayout.NORTH);
		card.add(valueLabel, BorderLayout.CENTER);
		card.add(descLabel, BorderLayout.SOUTH);

		return card;
	}

	private JPanel createTextPanel(TextAnalyser analyser, String title) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(title),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));

		JTextArea textArea = createResultText(analyser.getNormalized().length(),
				analyser.getDefaultEntropy(), analyser.getMarkovEntropy());

		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		return panel;
	}


	private void showErrorMessage(JPanel comparisonPanel, JPanel entropyArea) {
		comparisonPanel.add(new JLabel("Загрузите оба текста для сравнения", JLabel.CENTER));
		entropyArea.add(new JLabel("Загрузите оба текста для сравнения"), BorderLayout.CENTER);
	}


	private JTextArea createResultText(int length, double entropy, double markovEntropy) {
		JTextArea text = new JTextArea();
		text.setEditable(false);
		text.setText(String.format(
				"Результаты анализа:\nДлина текста: %d символов\nЭнтропия текста: %.3f\nМарковская энтропия первого порядка: %.3f",
				length, entropy, markovEntropy));
		return text;
	}

	private JPanel createTextsPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout());

		JLabel titleLabel = new JLabel("Загрузка текстов для анализа", JLabel.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

		JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 20));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

		contentPanel.add(createLanguagePanel("Русские тексты", 0, 6, Color.decode("#E3F2FD")));
		contentPanel.add(createLanguagePanel("Английские тексты", 6, 6, Color.decode("#F3E5F5")));

		mainPanel.add(titleLabel, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private JPanel createLanguagePanel(String title, int start, int count, Color bgColor) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1), title),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));
		panel.setBackground(bgColor);

		JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));
		buttonPanel.setBackground(bgColor);

		for (int i = start; i < start + count; i++) {
			final int index = i;
			JButton button = new JButton("Текст " + (i + 1));
			button.setFont(new Font("Arial", Font.PLAIN, 12));
			button.setBackground(Color.WHITE);
			button.setFocusPainted(false);

			button.addActionListener(e -> loadTextFile(index, button));
			buttonPanel.add(button);
		}

		panel.add(buttonPanel, BorderLayout.CENTER);
		return panel;
	}

	private void loadTextFile(int index, JButton button) {
		String text = chooseAndLoadFile();
		if (text != null) {
			loadedTexts[index] = text;
			button.setText("Текст " + (index + 1) + " ✓");
		}
	}


	private JTable createCombinedTable(Map<String, Integer> bigrams, Map<Character, Integer> charFreq,
									   int length, String tableType) {
		if (bigrams == null || bigrams.isEmpty()) {
			return new JTable(new Object[][]{{"Нет данных"}}, new String[]{"Нет данных"});
		}

		List<Character> chars = getUniqueChars(bigrams);
		int size = chars.size();

		String[] columns = new String[size + 1];
		Object[][] data = new Object[size][size + 1];

		setupTableStructure(columns, data, chars, bigrams, charFreq, length, tableType);
		return createStyledTable(data, columns, tableType);
	}

	private List<Character> getUniqueChars(Map<String, Integer> bigrams) {
		Set<Character> uniqueChars = new TreeSet<>();
		for (String bigram : bigrams.keySet()) {
			uniqueChars.add(bigram.charAt(0));
			uniqueChars.add(bigram.charAt(1));
		}
		return new ArrayList<>(uniqueChars);
	}

	private void setupTableStructure(String[] columns, Object[][] data, List<Character> chars,
									 Map<String, Integer> bigrams, Map<Character, Integer> charFreq,
									 int length, String tableType) {
		columns[0] = "A\\B";
		for (int i = 0; i < chars.size(); i++) {
			columns[i + 1] = String.valueOf(chars.get(i));
		}

		for (int i = 0; i < chars.size(); i++) {
			data[i][0] = chars.get(i);
			for (int j = 0; j < chars.size(); j++) {
				String bigram = chars.get(i) + "" + chars.get(j);
				int countAB = bigrams.getOrDefault(bigram, 0);

				switch (tableType) {
					case "conditional":
						double p_AB = (double) countAB / length;
						double p_A = (double) charFreq.getOrDefault(chars.get(i), 0) / length;
						double conditionalProb = p_A > 0 ? p_AB / p_A : 0;
						data[i][j + 1] = String.format("%.3f", conditionalProb * 100);
						break;
					case "joint":
						double jointProb = (double) countAB / length;
						data[i][j + 1] = String.format("%.3f", jointProb * 100);
						break;
					case "freq":
					default:
						data[i][j + 1] = String.format("%.3f", (double) countAB / length * 100);
						break;
				}
			}
		}
	}

	private JTable createStyledTable(Object[][] data, String[] columns, String tableType) {
		JTable table = new JTable(data, columns);

		table.setFont(new Font("Arial", Font.PLAIN, 11));
		table.setRowHeight(25);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(60);

		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(50);
		}

		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
														   boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				((JComponent) c).setOpaque(true);
				((JLabel) c).setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
				((JLabel) c).setHorizontalAlignment(JLabel.CENTER);

				if (!isSelected) {
					c.setBackground(Color.WHITE);
					c.setForeground(Color.BLACK);
				}

				if (!isSelected && value != null && column > 0) {
					try {
						String stringValue = value.toString().replace("%", "").replace(",", ".").trim();
						double numericValue = Double.parseDouble(stringValue);

						if (numericValue == 0) {
							c.setBackground(Color.WHITE);
						} else {
							double normalizedValue;
							switch (tableType) {
								case "conditional":
									normalizedValue = Math.min(numericValue / 20.0, 1.0);
									break;
								case "char":
									normalizedValue = Math.min(numericValue / 12.0, 1.0); // предполагаем макс 10%
									break;
								case "freq":
								default:
									normalizedValue = Math.min(numericValue / 1.5, 1.0); // предполагаем макс 5%
									break;
							}

							// Градиент от белого к зеленому
							int green = 255;
							int red = (int) (255 - (normalizedValue * 200));
							int blue = (int) (255 - (normalizedValue * 200));

							red = Math.max(red, 55);
							blue = Math.max(blue, 55);

							Color cellColor = new Color(red, green, blue);
							c.setBackground(cellColor);
						}
						c.setForeground(Color.BLACK);

					} catch (NumberFormatException ignored) {}
				}

				if (isSelected) {
					c.setBackground(table.getSelectionBackground());
					c.setForeground(table.getSelectionForeground());
				}

				return c;
			}
		});

		return table;
	}

	private String chooseAndLoadFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDragEnabled(true);
		FileNameExtensionFilter text = new FileNameExtensionFilter("Текстовые файлы (*.txt)", "txt");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(text);

		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			try {
				return Files.readString(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private JPanel createDiagramPanel() {
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel chartPanel = new JPanel(new CardLayout());

		for (int i = 0; i < 12; i++) {
			JFreeChart chart = createChart("Текст " + (i + 1),
					loadedTexts[i] != null ? new TextAnalyser(loadedTexts[i]).getAlphabet() : null,
					loadedTexts[i] != null ? new TextAnalyser(loadedTexts[i]).getNormalized().length() : 0);

			CategoryPlot plot = chart.getCategoryPlot();
			ValueAxis rangeAxis = plot.getRangeAxis();
			rangeAxis.setUpperBound(0.18);
			rangeAxis.setAutoRange(false);

			chartPanel.add(new ChartPanel(chart), "chart_" + i);
		}

		mainPanel.add(createChartControls(chartPanel), BorderLayout.NORTH);
		mainPanel.add(chartPanel, BorderLayout.CENTER);
		return mainPanel;
	}

	private JPanel createChartControls(JPanel chartPanel) {
		JPanel controls = new JPanel();
		JButton prev = new JButton("← Пред");
		JButton next = new JButton("След →");

		prev.addActionListener(e -> ((CardLayout) chartPanel.getLayout()).previous(chartPanel));
		next.addActionListener(e -> ((CardLayout) chartPanel.getLayout()).next(chartPanel));

		controls.add(prev);
		controls.add(next);
		return controls;
	}

	private JFreeChart createChart(String title, Map<Character, Integer> frequencies, int length) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		if(frequencies != null) {
			for(Character key : frequencies.keySet()) {
				double p = (double) frequencies.get(key) / length;
				dataset.addValue(p, "Частота", key);
			}
		} else {
			dataset.addValue(0, "Частота", "Нет данных");
		}

		return ChartFactory.createBarChart(title, "Символы", "Частота", dataset);
	}
}