package teammates.test.cases.logic;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Text;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackResponseCommentAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.logic.core.FeedbackQuestionsLogic;
import teammates.logic.core.FeedbackResponseCommentsLogic;
import teammates.logic.core.FeedbackResponsesLogic;
import teammates.test.cases.BaseComponentTestCase;
import teammates.test.driver.AssertHelper;
import teammates.test.util.TestHelper;

public class FeedbackResponseCommentsLogicTest extends BaseComponentTestCase {

    private FeedbackResponseCommentsLogic frcLogic = FeedbackResponseCommentsLogic.inst();
    private FeedbackQuestionsLogic fqLogic = FeedbackQuestionsLogic.inst();
    private FeedbackResponsesLogic frLogic = FeedbackResponsesLogic.inst();
    
    private static DataBundle dataBundle = getTypicalDataBundle();
    
    @BeforeClass
    public void setupClass() throws Exception {
        printTestClassHeader();
        turnLoggingUp(FeedbackResponseCommentsLogic.class);
        restoreTypicalDataInDatastore();
    }
    
    @Test
    public void testCreateFeedbackResponseComment() throws Exception {
        FeedbackResponseCommentAttributes frComment = new FeedbackResponseCommentAttributes();
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: non-existent course");

        frComment.courseId = "no-such-course";
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "Trying to create feedback response comments for a course that does not exist.");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: giver is not instructor");
        
        frComment.giverEmail = "student2InCourse1@gmail.com";
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "User " + frComment.giverEmail + " is not a registered instructor for course " 
                + frComment.courseId + ".");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: giver is not an instructor for the course");
        
        frComment.giverEmail = "instructor1@course2.com";
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "User " + frComment.giverEmail + " is not a registered instructor for course " 
                + frComment.courseId + ".");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: feedback session is not a session for the course");

        frComment.feedbackSessionName = "Private feedback session";
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "Feedback session " + frComment.feedbackSessionName + " is not a session for course " 
                + frComment.courseId + ".");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: feedback question is not a question for the session");
        
        frComment.feedbackQuestionId = "Non-exist-question-id";
        
        verifyExceptionThrownFromCreateFrComment(frComment, 
                "Feedback question of id " + frComment.feedbackQuestionId + " is not a question for session " 
                + frComment.feedbackSessionName + ".");
        
        frComment.feedbackQuestionId = getQuestionIdInDataBundle("qn2InSession2InCourse2");
        
        verifyExceptionThrownFromCreateFrComment(frComment, 
                "Feedback question of id " + frComment.feedbackQuestionId + " is not a question for session " 
                + frComment.feedbackSessionName + ".");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: feedback response is not a response for the question");

        frComment.feedbackQuestionId = getQuestionIdInDataBundle("qn1InSession1InCourse1");
        frComment.feedbackResponseId = "Non-exist-feedbackResponse-id";
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "Feedback response of id " + frComment.feedbackResponseId + " is not a response for question of id " 
                + frComment.feedbackQuestionId + ".");
        
        frComment.feedbackResponseId = getResponseIdInDataBundle("response1ForQ2S1C1", "qn2InSession1InCourse1");
        
        verifyExceptionThrownFromCreateFrComment(frComment,
                "Feedback response of id " + frComment.feedbackResponseId + " is not a response for question of id " 
                + frComment.feedbackQuestionId + ".");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("typical successful case");

        frComment.setId(null);
        frComment.feedbackQuestionId = getQuestionIdInDataBundle("qn1InSession1InCourse1");
        frComment.feedbackResponseId = getResponseIdInDataBundle("response2ForQ1S1C1", "qn1InSession1InCourse1");
        
        frcLogic.createFeedbackResponseComment(frComment);
        TestHelper.verifyPresentInDatastore(frComment);
        
        ______TS("typical successful case: frComment already exists");
        
        frComment.commentText = new Text("Already existed FeedbackResponseComment from instructor2 in course 1");
        
        frcLogic.createFeedbackResponseComment(frComment);
        FeedbackResponseCommentAttributes actualFrComment = 
                frcLogic.getFeedbackResponseCommentForSession(
                        frComment.courseId, 
                        frComment.feedbackSessionName).get(1);
        
        assertEquals(frComment.commentText, actualFrComment.commentText);
        
        //delete afterwards
        frcLogic.deleteFeedbackResponseComment(frComment);
    }

    @Test
    public void testGetFeedbackResponseComments() throws Exception {
        FeedbackResponseCommentAttributes frComment = new FeedbackResponseCommentAttributes();
        this.restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: invalid parameters");
        
        frComment.courseId = "invalid course id";
        frComment.giverEmail = "invalid giver email";

        verifyNullFromGetFrCommentForSession(frComment);
        verifyNullFromGetFrComment(frComment);
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("Typical successful case");
        
        List<FeedbackResponseCommentAttributes> actualFrComments = 
                frcLogic.getFeedbackResponseCommentForSession(
                        frComment.courseId, frComment.feedbackSessionName);
        FeedbackResponseCommentAttributes actualFrComment = actualFrComments.get(0);
        
        assertEquals(1, actualFrComments.size());
        assertEquals(frComment.courseId, actualFrComment.courseId);
        assertEquals(frComment.giverEmail, actualFrComment.giverEmail);
        assertEquals(frComment.feedbackSessionName, actualFrComment.feedbackSessionName);
        
        actualFrComment = 
                frcLogic.getFeedbackResponseComment(
                        frComment.feedbackResponseId, frComment.giverEmail, frComment.createdAt);
        
        assertEquals(frComment.courseId, actualFrComment.courseId);
        assertEquals(frComment.giverEmail, actualFrComment.giverEmail);
        assertEquals(frComment.feedbackSessionName, actualFrComment.feedbackSessionName);
    }
    
    @Test
    public void testUpdateFeedbackResponseComment() throws Exception{
        FeedbackResponseCommentAttributes frComment = new FeedbackResponseCommentAttributes();
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("fail: invalid params");
        
        frComment.courseId = "invalid course name";
        verifyExceptionThrownWhenUpdateFrComment(frComment,
                "not acceptable to TEAMMATES as a Course ID");
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        
        ______TS("typical success case");

        frComment.commentText = new Text("Updated feedback response comment");
        frcLogic.updateFeedbackResponseComment(frComment);
        TestHelper.verifyPresentInDatastore(frComment);
        FeedbackResponseCommentAttributes actualFrComment = 
                frcLogic.getFeedbackResponseCommentForSession(
                        frComment.courseId, frComment.feedbackSessionName).get(0);
        assertEquals(frComment.commentText, actualFrComment.commentText);
    }
    
    @Test
    public void testDeleteFeedbackResponseComment() throws Exception{
        //create a frComment to delete
        FeedbackResponseCommentAttributes frComment = new FeedbackResponseCommentAttributes();
        restoreFrCommentFromDataBundle(frComment, "comment1FromT1C1ToR1Q1S1C1");
        frComment.setId(null);
        frComment.feedbackQuestionId = getQuestionIdInDataBundle("qn2InSession1InCourse1");
        frComment.feedbackResponseId = getResponseIdInDataBundle("response2ForQ2S1C1", "qn2InSession1InCourse1");
        
        frcLogic.createFeedbackResponseComment(frComment);
        
        ______TS("silent fail nothing to delete");

        frComment.feedbackResponseId = "invalid responseId";
        //without proper frCommentId and its feedbackResponseId,
        //it cannot be deleted
        frcLogic.deleteFeedbackResponseComment(frComment);

        FeedbackResponseCommentAttributes actualFrComment = 
                frcLogic.getFeedbackResponseCommentForSession(
                        frComment.courseId, frComment.feedbackSessionName).get(0);
        TestHelper.verifyPresentInDatastore(actualFrComment);
        
        ______TS("typical success case");
        
        frcLogic.deleteFeedbackResponseComment(actualFrComment);
        TestHelper.verifyAbsentInDatastore(actualFrComment);
    }
    
    private void verifyExceptionThrownFromCreateFrComment(
            FeedbackResponseCommentAttributes frComment, String expectedMessage) 
            throws InvalidParametersException {
        try{
            frcLogic.createFeedbackResponseComment(frComment);
            signalFailureToDetectException();
        } catch(EntityDoesNotExistException e){
            assertEquals(expectedMessage, e.getMessage());
        }
    }
    
    private void verifyNullFromGetFrCommentForSession(
            FeedbackResponseCommentAttributes frComment) {
        List<FeedbackResponseCommentAttributes> frCommentsGot = 
                frcLogic.getFeedbackResponseCommentForSession(frComment.courseId, frComment.feedbackSessionName);
        assertEquals(0, frCommentsGot.size());
    }
    
    private void verifyNullFromGetFrComment(
            FeedbackResponseCommentAttributes frComment) {
        FeedbackResponseCommentAttributes frCommentGot = 
                frcLogic.getFeedbackResponseComment(
                        frComment.feedbackResponseId, frComment.giverEmail, frComment.createdAt);
        assertEquals(null, frCommentGot);
    }
    
    private void verifyExceptionThrownWhenUpdateFrComment(
            FeedbackResponseCommentAttributes frComment, String expectedString)
            throws EntityDoesNotExistException {
        try{
            frcLogic.updateFeedbackResponseComment(frComment);
            signalFailureToDetectException();
        } catch(InvalidParametersException e){
            AssertHelper.assertContains(expectedString, e.getMessage());
        }
    }
    
    private void restoreFrCommentFromDataBundle(
            FeedbackResponseCommentAttributes frComment, String existingFrCommentInDataBundle) {
        
        FeedbackResponseCommentAttributes existingFrComment = 
                dataBundle.feedbackResponseComments.get(existingFrCommentInDataBundle);
        frComment.courseId = existingFrComment.courseId;
        frComment.giverEmail = existingFrComment.giverEmail;
        frComment.feedbackSessionName = existingFrComment.feedbackSessionName;
        frComment.feedbackQuestionId = existingFrComment.feedbackQuestionId;
        frComment.commentText = existingFrComment.commentText;
        frComment.createdAt = existingFrComment.createdAt;
        restoreFrCommentIdFromExistingOne(frComment, existingFrComment);
    }
    
    private void restoreFrCommentIdFromExistingOne(
            FeedbackResponseCommentAttributes frComment,
            FeedbackResponseCommentAttributes existingFrComment) {
        
        List<FeedbackResponseCommentAttributes> existingFrComments = 
                frcLogic.getFeedbackResponseCommentForSession(
                        existingFrComment.courseId, 
                        existingFrComment.feedbackSessionName);
        
        FeedbackResponseCommentAttributes existingFrCommentWithId = null;
        for(FeedbackResponseCommentAttributes c: existingFrComments){
            existingFrCommentWithId = 
                    c.commentText.equals(existingFrComment.commentText)? c: null;
        }
        frComment.setId(existingFrCommentWithId.getId());
        frComment.feedbackResponseId = existingFrCommentWithId.feedbackResponseId;
    }
    
    private String getQuestionIdInDataBundle(String questionInDataBundle) {
        FeedbackQuestionAttributes question = dataBundle.feedbackQuestions.get(questionInDataBundle);
        question = fqLogic.getFeedbackQuestion(
                question.feedbackSessionName, question.courseId, question.questionNumber);
        return question.getId();
    }
    
    private String getResponseIdInDataBundle(String responseInDataBundle, String questionInDataBundle) {
        FeedbackResponseAttributes response = dataBundle.feedbackResponses.get(responseInDataBundle);
        response = frLogic.getFeedbackResponse(
                getQuestionIdInDataBundle(questionInDataBundle), 
                response.giverEmail, 
                response.recipientEmail);
        return response.getId();
    }
}
