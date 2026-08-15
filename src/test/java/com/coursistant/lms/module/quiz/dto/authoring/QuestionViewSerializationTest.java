package com.coursistant.lms.module.quiz.dto.authoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves instructor vs student question JSON shapes diverge on answer-key fields.
 * Students must never see {@code isCorrect} / {@code version}.
 */
class QuestionViewSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void instructorQuestionResponse_includesAnswerKeyFields() throws Exception {
        OptionResponse option = new OptionResponse();
        option.setId(101);
        option.setLabel("Paris");
        option.setIsCorrect(true);
        option.setPosition(1);

        QuestionResponse question = new QuestionResponse();
        question.setId(10);
        question.setQuizId(3);
        question.setType("SingleChoice");
        question.setStem("Capital of France?");
        question.setPoints(new BigDecimal("2.0"));
        question.setPosition(1);
        question.setVersion(2);
        question.setOptions(List.of(option));

        assertInstanceOf(QuestionView.class, question);

        JsonNode node = mapper.readTree(mapper.writeValueAsString(question));
        assertEquals(2, node.get("version").asInt());
        assertTrue(node.get("options").get(0).get("isCorrect").asBoolean());
        assertEquals("Paris", node.get("options").get(0).get("label").asText());
    }

    @Test
    void studentQuestionResponse_omitsAnswerKeyFields() throws Exception {
        StudentOptionResponse option = new StudentOptionResponse();
        option.setId(101);
        option.setLabel("Paris");
        option.setPosition(1);

        StudentQuestionResponse question = new StudentQuestionResponse();
        question.setId(10);
        question.setQuizId(3);
        question.setType("SingleChoice");
        question.setStem("Capital of France?");
        question.setPoints(new BigDecimal("2.0"));
        question.setPosition(1);
        question.setOptions(List.of(option));

        assertInstanceOf(QuestionView.class, question);

        String json = mapper.writeValueAsString(question);
        JsonNode node = mapper.readTree(json);

        assertFalse(node.has("version"), "student JSON must not expose question version");
        assertFalse(json.contains("isCorrect"), "student JSON must not contain isCorrect answer key");
        assertFalse(node.get("options").get(0).has("isCorrect"));
        assertEquals("Paris", node.get("options").get(0).get("label").asText());
        assertEquals(10, node.get("id").asInt());
    }

    @Test
    void polymorphicQuestionView_serializesConcreteShapeOnly() throws Exception {
        QuestionView instructor = instructorSample();
        QuestionView student = studentSample();

        JsonNode instructorNode = mapper.readTree(mapper.writeValueAsString(instructor));
        JsonNode studentNode = mapper.readTree(mapper.writeValueAsString(student));

        assertTrue(instructorNode.has("version"));
        assertTrue(instructorNode.get("options").get(0).has("isCorrect"));

        assertFalse(studentNode.has("version"));
        assertFalse(studentNode.get("options").get(0).has("isCorrect"));
        assertFalse(mapper.writeValueAsString(student).contains("isCorrect"));
    }

    private static QuestionResponse instructorSample() {
        OptionResponse option = new OptionResponse();
        option.setId(1);
        option.setLabel("A");
        option.setIsCorrect(true);
        option.setPosition(1);
        QuestionResponse q = new QuestionResponse();
        q.setId(1);
        q.setQuizId(1);
        q.setType("TrueFalse");
        q.setStem("q");
        q.setPoints(BigDecimal.ONE);
        q.setPosition(1);
        q.setVersion(1);
        q.setOptions(List.of(option));
        return q;
    }

    private static StudentQuestionResponse studentSample() {
        StudentOptionResponse option = new StudentOptionResponse();
        option.setId(1);
        option.setLabel("A");
        option.setPosition(1);
        StudentQuestionResponse q = new StudentQuestionResponse();
        q.setId(1);
        q.setQuizId(1);
        q.setType("TrueFalse");
        q.setStem("q");
        q.setPoints(BigDecimal.ONE);
        q.setPosition(1);
        q.setOptions(List.of(option));
        return q;
    }
}
