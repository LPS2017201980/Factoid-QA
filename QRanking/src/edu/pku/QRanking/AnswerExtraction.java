package edu.pku.QRanking;

import java.io.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.pku.QRanking.answerscore.CombinationAnswerScorer;
import edu.pku.QRanking.evidencescore.CombinationEvidenceScorer;
import edu.pku.QRanking.util.NLPTools;

/**
 * Answer Extraction
 *
 * @author 李琦
 * @email stormier@126.com
 * 
 */

public class AnswerExtraction {
	List<Question> questions = new ArrayList<>();
	AnswerSelector selector = new AnswerSelector();
	CombinationEvidenceScorer evidenceScorer = new CombinationEvidenceScorer();
	CombinationAnswerScorer answerScorer = new CombinationAnswerScorer();

	public void extractAnswer(String inputFileName, String outputFileName)
			throws Exception {
		NLPTools tool = new NLPTools();

		File input = new File(inputFileName);
		Document doc = Jsoup.parse(input, "UTF-8", "");
		Elements elements = doc.select("QuestionSet > question");
		List<String> newone;
		for (Element element : elements) {
			Question question = new Question();

			// get question
			Elements subElements = element.select("q");
			String title = subElements.get(0).text();
			newone = tool.segment(title);
			question.title = newone;
			question.tagged_title = tool.postag(newone);

			// get question type
			subElements = element.select("category");
			String type = subElements.get(0).text();
			switch (type) {
			case "People":
				question.type = QuestionType.PERSON_NAME;
				break;
			case "Location":
				question.type = QuestionType.LOCATION_NAME;
				break;
			case "Number":
				question.type = QuestionType.NUMBER;
				break;
			case "Other":
				question.type = QuestionType.OTHER;
				break;
			default:
				question.type = QuestionType.NULL;
			}

			// get evidence
			subElements = element.select("summary");
			for (Element subelement : subElements) {
				Evidence new_evidence = new Evidence();
				String evidence = subelement.text();
				newone = tool.segment(evidence);
				new_evidence.evidence_content = newone;
				new_evidence.tagged_evidence = tool.postag(newone);
				new_evidence.score = 0;
				question.evidences.add(new_evidence);
			}

			questions.add(question);
		}

		// get candidate answers
		for (Question question : questions) {
			selector.select(question);
		}

		// score evidences
		for (Question question : questions) {
			evidenceScorer.score(question);
		}

		// score answers
		for (Question question : questions) {
			answerScorer.score(question);
		}

		// show answers
		int i = 1;
		File output = new File(outputFileName);
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		for (Question question : questions) {

			writer.write(i + "\t" + question.synthesized_answers.get(0).answer
					+"\n");

			for (SynthesizedAnswer answer : question.synthesized_answers) {
				System.out.println(i + "\t" + answer.answer + " " + answer.score
						+ "\n");
			}

			i++;
		}
		writer.flush();
		writer.close();
	}

	public static void main(String[] args) throws Exception {
		AnswerExtraction ae = new AnswerExtraction();
		ae.extractAnswer("stage3.xml", "stage4.xml");

	}

}