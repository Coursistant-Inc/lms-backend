package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.dto.authoring.*;
import com.coursistant.lms.module.quiz.entity.Quiz;
import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.entity.QuizQuestionOption;
import com.coursistant.lms.module.quiz.repository.QuizMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionMapper;
import com.coursistant.lms.module.quiz.repository.QuizQuestionOptionMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizQuestionService {

    @Resource
    private QuizMapper quizMapper;
    @Resource
    private QuizQuestionMapper quizQuestionMapper;
    @Resource
    private QuizQuestionOptionMapper quizQuestionOptionMapper;
    @Resource
    private QuizAccessService quizAccessService;
    @Resource
    private QuizTimeSupport quizTimeSupport;
    @Resource
    private QuizRegradeService quizRegradeService;

    public List<?> listQuestions(HttpServletRequest request, Integer courseId, Integer quizId, Integer userId) {
        quizAccessService.requireQuizReadable(request, courseId, quizId, userId);
        boolean instructor = quizAccessService.isInstructor(courseId, userId);
        List<QuizQuestion> questions = quizQuestionMapper.selectByQuizId(quizId);
        if (instructor) {
            return questions.stream().map(this::toInstructorResponse).collect(Collectors.toList());
        }
        return questions.stream().map(this::toStudentResponse).collect(Collectors.toList());
    }

    public Object getQuestion(HttpServletRequest request, Integer courseId, Integer quizId,
                              Integer questionId, Integer userId) {
        quizAccessService.requireQuizReadable(request, courseId, quizId, userId);
        QuizQuestion q = requireQuestion(quizId, questionId);
        if (quizAccessService.isInstructor(courseId, userId)) {
            return toInstructorResponse(q);
        }
        return toStudentResponse(q);
    }

    @Transactional
    public QuestionResponse create(Integer courseId, Integer quizId, Integer userId, CreateQuestionRequest body) {
        Quiz quiz = quizAccessService.requireQuizConfigurable(courseId, quizId, userId);
        assertContentEditable(quizId);
        validateQuestionInput(body.getType(), body.getStem(), body.getPoints(), body.getOptions());
        var now = quizTimeSupport.nowUtc();
        int position = quizQuestionMapper.maxPositionByQuizId(quizId) + 1;
        QuizQuestion q = new QuizQuestion();
        q.setQuizId(quizId);
        q.setType(body.getType());
        q.setStem(trim(body.getStem(), 20000));
        q.setPoints(body.getPoints());
        q.setPosition(position);
        q.setVersion(1);
        q.setCreatedAt(now);
        q.setUpdatedAt(now);
        quizQuestionMapper.insert(q);
        insertOptions(q.getId(), body.getOptions());
        return toInstructorResponse(quizQuestionMapper.selectById(q.getId()));
    }

    @Transactional
    public QuestionResponse patch(Integer courseId, Integer quizId, Integer questionId,
                                  Integer userId, PatchQuestionRequest body) {
        quizAccessService.requireQuizConfigurable(courseId, quizId, userId);
        assertContentEditable(quizId);
        QuizQuestion q = requireQuestion(quizId, questionId);
        if (body.getExpectedVersion() != null && !body.getExpectedVersion().equals(q.getVersion())) {
            throw new ApiException(ErrorType.QUIZ_VERSION_CONFLICT);
        }
        if (body.getStem() != null) {
            q.setStem(trim(body.getStem(), 20000));
        }
        if (body.getPoints() != null) {
            q.setPoints(body.getPoints());
        }
        q.setVersion(q.getVersion() + 1);
        q.setUpdatedAt(quizTimeSupport.nowUtc());
        if (quizQuestionMapper.updateById(q) == 0) {
            throw new ApiException(ErrorType.QUIZ_VERSION_CONFLICT);
        }
        if (body.getOptions() != null) {
            quizQuestionOptionMapper.deleteByQuestionId(questionId);
            insertOptions(questionId, body.getOptions());
        }
        return toInstructorResponse(quizQuestionMapper.selectById(questionId));
    }

    @Transactional
    public void delete(Integer courseId, Integer quizId, Integer questionId, Integer userId) {
        quizAccessService.requireQuizConfigurable(courseId, quizId, userId);
        assertContentEditable(quizId);
        requireQuestion(quizId, questionId);
        quizQuestionOptionMapper.deleteByQuestionId(questionId);
        quizQuestionMapper.deleteById(questionId);
    }

    @Transactional
    public void reorder(Integer courseId, Integer quizId, Integer userId, ReorderQuestionsRequest body) {
        quizAccessService.requireQuizConfigurable(courseId, quizId, userId);
        assertContentEditable(quizId);
        if (body.getQuestionIds() == null) {
            throw new ApiException(ErrorType.BAD_REQUEST);
        }
        int pos = 1;
        for (Integer qid : body.getQuestionIds()) {
            QuizQuestion q = requireQuestion(quizId, qid);
            quizQuestionMapper.updatePosition(q.getId(), pos++);
        }
    }

    @Transactional
    public QuestionResponse patchAnswerKey(Integer courseId, Integer quizId, Integer questionId,
                                           Integer userId, PatchAnswerKeyRequest body) {
        quizAccessService.requireInstructor(courseId, userId);
        requireQuestion(quizId, questionId);
        return quizRegradeService.regradeAnswerKey(courseId, quizId, questionId, userId, body);
    }

    public void assertPublishReady(Integer quizId) {
        List<QuizQuestion> questions = quizQuestionMapper.selectByQuizId(quizId);
        for (QuizQuestion q : questions) {
            List<QuizQuestionOption> options = quizQuestionOptionMapper.selectInstructorByQuestionId(q.getId());
            if (QuizConstants.TYPE_SHORT_ANSWER.equals(q.getType())) {
                if (options != null && !options.isEmpty()) {
                    throw new ApiException(ErrorType.BAD_REQUEST, "Short answer question has options");
                }
                continue;
            }
            if (options == null || options.size() < 2) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Question " + q.getId() + " needs at least two options");
            }
            long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).count();
            if (QuizConstants.TYPE_TRUE_FALSE.equals(q.getType()) && (options.size() != 2 || correct != 1)) {
                throw new ApiException(ErrorType.BAD_REQUEST, "True/false question invalid answer key");
            }
            if (QuizConstants.TYPE_SINGLE_CHOICE.equals(q.getType()) && correct != 1) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Single choice needs exactly one correct option");
            }
            if (QuizConstants.TYPE_MULTIPLE_SELECT.equals(q.getType()) && correct < 1) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Multiple select needs at least one correct option");
            }
        }
    }

    private void assertContentEditable(Integer quizId) {
        if (quizMapper.countAttemptsByQuizId(quizId) > 0) {
            throw new ApiException(ErrorType.QUIZ_CONTENT_LOCKED);
        }
    }

    private QuizQuestion requireQuestion(Integer quizId, Integer questionId) {
        QuizQuestion q = quizQuestionMapper.selectByQuizIdAndId(quizId, questionId);
        if (q == null) {
            throw new ApiException(ErrorType.QUIZ_QUESTION_NOT_FOUND);
        }
        return q;
    }

    private void insertOptions(Integer questionId, List<OptionInput> options) {
        if (options == null) {
            return;
        }
        int pos = 1;
        for (OptionInput in : options) {
            QuizQuestionOption o = new QuizQuestionOption();
            o.setQuestionId(questionId);
            o.setLabel(trim(in.getLabel(), 500));
            o.setIsCorrect(Boolean.TRUE.equals(in.getIsCorrect()));
            o.setPosition(in.getPosition() != null ? in.getPosition() : pos++);
            quizQuestionOptionMapper.insert(o);
        }
    }

    private void validateQuestionInput(String type, String stem, BigDecimal points, List<OptionInput> options) {
        if (type == null || stem == null || stem.isBlank() || points == null || points.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Invalid question");
        }
        if (QuizConstants.TYPE_SHORT_ANSWER.equals(type)) {
            if (options != null && !options.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "Short answer cannot have options");
            }
            return;
        }
        if (options == null || options.size() < 2) {
            throw new ApiException(ErrorType.BAD_REQUEST, "At least two options required");
        }
        long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.getIsCorrect())).count();
        if (QuizConstants.TYPE_TRUE_FALSE.equals(type) && (options.size() != 2 || correct != 1)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "True/false requires exactly two options with one correct");
        }
        if (QuizConstants.TYPE_SINGLE_CHOICE.equals(type) && correct != 1) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Single choice requires exactly one correct option");
        }
        if (QuizConstants.TYPE_MULTIPLE_SELECT.equals(type) && correct < 1) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Multiple select requires at least one correct option");
        }
    }

    private QuestionResponse toInstructorResponse(QuizQuestion q) {
        QuestionResponse r = new QuestionResponse();
        r.setId(q.getId());
        r.setQuizId(q.getQuizId());
        r.setType(q.getType());
        r.setStem(q.getStem());
        r.setPoints(q.getPoints());
        r.setPosition(q.getPosition());
        r.setVersion(q.getVersion());
        List<OptionResponse> opts = new ArrayList<>();
        for (QuizQuestionOption o : quizQuestionOptionMapper.selectInstructorByQuestionId(q.getId())) {
            OptionResponse or = new OptionResponse();
            or.setId(o.getId());
            or.setLabel(o.getLabel());
            or.setIsCorrect(o.getIsCorrect());
            or.setPosition(o.getPosition());
            opts.add(or);
        }
        r.setOptions(opts);
        return r;
    }

    private StudentQuestionResponse toStudentResponse(QuizQuestion q) {
        StudentQuestionResponse r = new StudentQuestionResponse();
        r.setId(q.getId());
        r.setQuizId(q.getQuizId());
        r.setType(q.getType());
        r.setStem(q.getStem());
        r.setPoints(q.getPoints());
        r.setPosition(q.getPosition());
        List<StudentOptionResponse> opts = new ArrayList<>();
        for (QuizQuestionOption o : quizQuestionOptionMapper.selectStudentSafeByQuestionId(q.getId())) {
            StudentOptionResponse sr = new StudentOptionResponse();
            sr.setId(o.getId());
            sr.setLabel(o.getLabel());
            sr.setPosition(o.getPosition());
            opts.add(sr);
        }
        r.setOptions(opts);
        return r;
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
