package org.bonitasoft.engine.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bonitasoft.engine.CommonAPITest;
import org.bonitasoft.engine.api.ProcessManagementAPI;
import org.bonitasoft.engine.bpm.comment.ArchivedComment;
import org.bonitasoft.engine.bpm.comment.Comment;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.bpm.flownode.ActivityInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceCriterion;
import org.bonitasoft.engine.bpm.process.impl.DocumentDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.SubProcessDefinitionBuilder;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.ExpressionBuilder;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.test.annotation.Cover;
import org.bonitasoft.engine.test.annotation.Cover.BPMNConcept;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProcessDeletionTest extends CommonAPITest {

    protected User pedro;

    @Before
    public void before() throws Exception {
        login();
        pedro = createUser("pedro", "secreto");
    }

    @After
    public void after() throws Exception {
        deleteUser(pedro);
        logout();
    }

    private ProcessDefinition deployProcessWithSeveralOutGoingTransitions() throws BonitaException {
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("process To Delete", "2.5");
        final String actorName = "delivery";
        processDefinitionBuilder.addActor(actorName);
        processDefinitionBuilder.addUserTask("step1", actorName);
        for (int i = 0; i < 30; i++) {
            final String activityName = "step2" + i;
            processDefinitionBuilder.addUserTask(activityName, actorName);
            processDefinitionBuilder.addTransition("step1", activityName);

        }
        return deployAndEnableWithActor(processDefinitionBuilder.done(), actorName, pedro);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "delete process" }, jira = "")
    public void deleteProcessInstanceStopsCreatingNewActivities() throws Exception {
        final ProcessDefinition processDefinition = deployProcessWithSeveralOutGoingTransitions();
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTaskAndExecuteIt("step1", processInstance, pedro.getId());
        getProcessAPI().deleteProcessInstance(processInstance.getId());

        Thread.sleep(1500);

        final List<HumanTaskInstance> taskInstances = getProcessAPI().getPendingHumanTaskInstances(pedro.getId(), 0, 100, ActivityInstanceCriterion.DEFAULT);
        assertEquals(0, taskInstances.size());
        disableAndDeleteProcess(processDefinition.getId());
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "delete process" }, jira = "")
    public void deleteProcessInstanceAlsoDeleteArchivedElements() throws Exception {
        final ProcessDefinition processDefinition = deployProcessWithSeveralOutGoingTransitions();
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTaskAndExecuteIt("step1", processInstance, pedro.getId());
        for (int i = 0; i < 30; i++) {
            waitForUserTask("step2" + i, processInstance);
        }

        getProcessAPI().deleteProcessInstance(processInstance.getId());

        final List<ArchivedActivityInstance> taskInstances = getProcessAPI().getArchivedActivityInstances(processInstance.getId(), 0, 100,
                ActivityInstanceCriterion.DEFAULT);
        assertEquals(0, taskInstances.size());
        disableAndDeleteProcess(processDefinition.getId());
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "delete process" })
    public void deleteProcessDefinitionStopsCreatingNewActivities() throws Exception {
        final ProcessDefinitionBuilder processDefinitionBuilder = new ProcessDefinitionBuilder().createNewInstance("process To Delete", "2.5");
        final String actorName = "delivery";
        processDefinitionBuilder.addActor(actorName);
        processDefinitionBuilder.addUserTask("step1", actorName);
        for (int i = 0; i < 30; i++) {
            final String activityName = "step2" + i;
            processDefinitionBuilder.addUserTask(activityName, actorName);
            processDefinitionBuilder.addTransition("step1", activityName);
        
        }
        final ProcessDefinition processDefinition = deployAndEnableWithActor(processDefinitionBuilder.done(), actorName, pedro);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTaskAndExecuteIt("step1", processInstance, pedro.getId());
        disableAndDeleteProcess(processDefinition.getId()); // will fail in CommonAPITest.succeeded if activities are created after delete
        Thread.sleep(1500);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "call activities" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteChildrenProcesses() throws Exception {
        // deploy a simple process P1
        final String simpleStepName = "simpleStep";
        final ProcessDefinition simpleProcess = deployAndEnableSimpleProcess("simpleProcess", simpleStepName);

        // deploy a process P2 containing a call activity calling P1
        final String intermediateStepName = "intermediateStep1";
        final String intermediateCallActivityName = "intermediateCall";
        final ProcessDefinition intermediateProcess = deployAndEnableProcessWithCallActivity("intermediateProcess", simpleProcess.getName(),
                intermediateStepName, intermediateCallActivityName);

        // deploy a process P3 containing a call activity calling P2
        final String rootStepName = "rootStep1";
        final String rootCallActivityName = "rootCall";
        final ProcessDefinition rootProcess = deployAndEnableProcessWithCallActivity("rootProcess", intermediateProcess.getName(), rootStepName,
                rootCallActivityName);

        // start P3, the call activities will start instances of P2 a and P1
        final ProcessInstance rootProcessInstance = getProcessAPI().startProcess(rootProcess.getId());
        waitForUserTask(simpleStepName, rootProcessInstance.getId());

        // check that the instances of p1, p2 and p3 were created
        List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_ASC);
        assertEquals(3, processInstances.size());

        // check that archived flow nodes
        List<ArchivedActivityInstance> taskInstances = getProcessAPI().getArchivedActivityInstances(rootProcessInstance.getId(), 0, 100,
                ActivityInstanceCriterion.DEFAULT);
        assertTrue(taskInstances.size() > 0);

        // delete the root process instance
        getProcessAPI().deleteProcessInstance(rootProcessInstance.getId());

        // check that the instances of p1 and p2 were deleted
        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_ASC);
        assertEquals(0, processInstances.size());

        // check that archived flow nodes were deleted.
        taskInstances = getProcessAPI().getArchivedActivityInstances(rootProcessInstance.getId(), 0, 100, ActivityInstanceCriterion.DEFAULT);
        assertEquals(0, taskInstances.size());

        // clean up
        disableAndDeleteProcess(rootProcess, intermediateProcess, simpleProcess);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "call activities" }, jira = "ENGINE-257")
    public void deleteProcessDefinitionAlsoDeleteArchivedChildrenProcesses() throws Exception {
        // deploy a simple process P1
        final String simpleStepName = "simpleStep";
        final ProcessDefinition simpleProcess = deployAndEnableSimpleProcess("simpleProcess", simpleStepName);

        // deploy a process P2 containing a call activity calling P1
        final String intermediateStepName = "intermediateStep1";
        final String intermediateCallActivityName = "intermediateCall";
        final ProcessDefinition intermediateProcess = deployAndEnableProcessWithCallActivity("intermediateProcess", simpleProcess.getName(),
                intermediateStepName, intermediateCallActivityName);

        // deploy a process P3 containing a call activity calling P2
        final String rootStepName = "rootStep1";
        final String rootCallActivityName = "rootCall";
        final ProcessDefinition rootProcess = deployAndEnableProcessWithCallActivity("rootProcess", intermediateProcess.getName(), rootStepName,
                rootCallActivityName);

        // start P3, the call activities will start instances of P2 a and P1
        final ProcessInstance rootProcessInstance = getProcessAPI().startProcess(rootProcess.getId());
        final ActivityInstance simpleTask = waitForUserTask(simpleStepName, rootProcessInstance.getId());
        final long simpleProcessInstanceId = simpleTask.getParentProcessInstanceId();

        // execute simple task: p1 will finish
        assignAndExecuteStep(simpleTask, pedro.getId());

        // execute intermediate task: p2 will finish
        final ActivityInstance intermediateTask = waitForUserTask(intermediateStepName, rootProcessInstance.getId());
        final long intermediateProcessInstanceId = intermediateTask.getParentProcessInstanceId();
        assignAndExecuteStep(intermediateTask, pedro.getId());

        // execute root task: p3 will finish
        waitForUserTaskAndExecuteIt(rootStepName, rootProcessInstance, pedro.getId());
        waitForProcessToFinish(rootProcessInstance);

        // delete the processDefinition: all archived processes must be deleted
        disableAndDeleteProcess(rootProcess);

        // check that archived flow nodes were deleted.
        checkAllArhivedElementsWereDeleted(rootProcessInstance.getId());
        checkAllArhivedElementsWereDeleted(intermediateProcessInstanceId);
        checkAllArhivedElementsWereDeleted(simpleProcessInstanceId);

        disableAndDeleteProcess(intermediateProcess, simpleProcess);
    }

    private void checkAllArhivedElementsWereDeleted(final long processInstanceId) throws BonitaException {
        // check that archived flow nodes were deleted.
        final List<ArchivedActivityInstance> taskInstances = getProcessAPI().getArchivedActivityInstances(processInstanceId, 0, 100,
                ActivityInstanceCriterion.DEFAULT);
        assertEquals(0, taskInstances.size());

        // check archived processIntances were deleted
        final List<ArchivedProcessInstance> archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(processInstanceId, 0, 10);
        assertEquals(0, archivedProcessInstanceList.size());
    }

    private ProcessDefinition deployAndEnableSimpleProcess(final String processName, final String userTaskName) throws BonitaException {
        final String actorName = "delivery";

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance(processName, "1.0");
        processDefBuilder.addActor(actorName);
        processDefBuilder.addStartEvent("tStart");
        processDefBuilder.addUserTask(userTaskName, actorName);
        processDefBuilder.addEndEvent("tEnd");
        processDefBuilder.addTransition("tStart", userTaskName);
        processDefBuilder.addTransition(userTaskName, "tEnd");

        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(processDefBuilder.done(), actorName, pedro);

        return targetProcessDefinition;
    }

    private ProcessDefinition deployAndEnableProcessWithCallActivity(final String processName, final String targetProcessName, final String userTaskName,
            final String callActivityName) throws BonitaException {

        final String actorName = "delivery";
        final Expression targetProcessNameExpr = new ExpressionBuilder().createConstantStringExpression(targetProcessName);

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance(processName, "1.0");
        processDefBuilder.addActor(actorName);
        processDefBuilder.addStartEvent("start");
        processDefBuilder.addCallActivity(callActivityName, targetProcessNameExpr, null);
        processDefBuilder.addUserTask(userTaskName, actorName);
        processDefBuilder.addEndEvent("end");
        processDefBuilder.addTransition("start", callActivityName);
        processDefBuilder.addTransition(callActivityName, userTaskName);
        processDefBuilder.addTransition(userTaskName, "end");

        final ProcessDefinition processDefinition = deployAndEnableWithActor(processDefBuilder.done(), actorName, pedro);

        return processDefinition;
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "call activities" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteArchivedChildrenProcesses() throws Exception {
        // deploy a simple process P1
        final String simpleStepName = "simpleStep";
        final ProcessDefinition simpleProcess = deployAndEnableSimpleProcess("simpleProcess", simpleStepName);

        // deploy a process P2 containing a call activity calling P1
        final String rootStepName = "rootStep1";
        final String rootCallActivityName = "rootCall";
        final ProcessDefinition rootProcess = deployAndEnableProcessWithCallActivity("rootProcess", simpleProcess.getName(), rootStepName, rootCallActivityName);

        // start P2, the call activities will start an instance of P1
        final ProcessInstance rootProcessInstance = getProcessAPI().startProcess(rootProcess.getId());
        final ActivityInstance simpleTask = waitForUserTask(simpleStepName, rootProcessInstance.getId());
        assignAndExecuteStep(simpleTask, pedro.getId());
        final ProcessInstance simpleProcessInstance = getProcessAPI().getProcessInstance(simpleTask.getParentProcessInstanceId());
        waitForUserTask(rootStepName, rootProcessInstance.getId());
        waitForProcessToFinish(simpleProcessInstance);

        // check that only one instance (p2) is in the journal: p1 is supposed to be archived
        List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_ASC);
        assertEquals(1, processInstances.size());

        // check that there are archived instances of p1
        List<ArchivedProcessInstance> archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(simpleProcessInstance.getId(), 0, 10);
        assertTrue(archivedProcessInstanceList.size() > 0);

        // delete the root process instance
        getProcessAPI().deleteProcessInstance(rootProcessInstance.getId());

        // check that the instance of p2 was deleted
        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.NAME_ASC);
        assertEquals(0, processInstances.size());

        // check that the archived instances of p1 were deleted
        archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(simpleProcessInstance.getId(), 0, 10);
        assertEquals(0, archivedProcessInstanceList.size());

        // check that archived flow node were deleted.
        final List<ArchivedActivityInstance> taskInstances = getProcessAPI().getArchivedActivityInstances(rootProcessInstance.getId(), 0, 10,
                ActivityInstanceCriterion.DEFAULT);
        assertEquals(0, taskInstances.size());

        // clean up
        disableAndDeleteProcess(rootProcess, simpleProcess);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process instance", "event sub-process" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteEventSubProcesses() throws Exception {
        final String parentTaskName = "step1";
        final String childTaskName = "subStep";
        final String signalName = "go";
        // deploy and create a instance of a process containing an event sub-process
        final ProcessDefinition processDefinition = deployAndEnableProcessWithSignalEventSubProcess(parentTaskName, childTaskName, signalName);
        final ProcessInstance rootProcessInstance = getProcessAPI().startProcess(processDefinition.getId());

        // wait for the first step in the parent process before sending signal the launch the event sub-process
        waitForUserTask(parentTaskName, rootProcessInstance.getId());
        getProcessAPI().sendSignal(signalName);

        // wait for first step in the event sub-process
        waitForUserTask(childTaskName, rootProcessInstance.getId());

        // check the number of process instances: 2 expected the root process instance and the event subprocess
        List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        assertEquals(2, processInstances.size());

        // delete the root process instance: the event subprocess must be deleted at same time
        getProcessAPI().deleteProcessInstance(rootProcessInstance.getId());

        // check the number of proces instances
        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        assertEquals(0, processInstances.size());

        // cleanup
        disableAndDeleteProcess(processDefinition);

    }

    private ProcessDefinition deployAndEnableProcessWithSignalEventSubProcess(final String parentTaskName, final String childTaskName, final String signalName)
            throws BonitaException {
        final ProcessDefinitionBuilder builder = new ProcessDefinitionBuilder().createNewInstance("ProcessWithEventSubProcess", "1.0");
        builder.addActor("mainActor");
        builder.addStartEvent("start");
        builder.addUserTask(parentTaskName, "mainActor");
        builder.addEndEvent("end");
        builder.addTransition("start", parentTaskName);
        builder.addTransition(parentTaskName, "end");
        final SubProcessDefinitionBuilder subProcessBuilder = builder.addSubProcess("eventSubProcess", true).getSubProcessBuilder();
        subProcessBuilder.addStartEvent("startSub").addSignalEventTrigger(signalName);
        subProcessBuilder.addUserTask(childTaskName, "mainActor");
        subProcessBuilder.addEndEvent("endSubProcess");
        subProcessBuilder.addTransition("startSub", childTaskName);
        subProcessBuilder.addTransition(childTaskName, "endSubProcess");
        final DesignProcessDefinition processDefinition = builder.done();
        return deployAndEnableWithActor(processDefinition, "mainActor", pedro);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process", "archived process instance" }, jira = "ENGINE-257")
    public void deleteProcessDefinitionAlsoDeleteArchivedProcessIntances() throws Exception {
        // deploy a simple process
        final String userTaskName = "step1";
        final ProcessDefinition processDefinition = deployAndEnableSimpleProcess("myProcess", userTaskName);

        // start a process and execute it until end
        final ProcessInstance processInstanceToArchive = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTaskAndExecuteIt(userTaskName, processInstanceToArchive, pedro.getId());
        waitForProcessToFinish(processInstanceToArchive);

        // start a process non completed process
        final ProcessInstance activeProcessInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask(userTaskName, activeProcessInstance);

        // check number of process instances and archived process instances
        List<ProcessInstance> processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        List<ArchivedProcessInstance> archivedProcessInstances = getProcessAPI().getArchivedProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        assertEquals(1, processInstances.size());
        assertEquals(1, archivedProcessInstances.size());

        // delete definition
        disableAndDeleteProcess(processDefinition);

        // check number of process instances and archived process instances
        processInstances = getProcessAPI().getProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        archivedProcessInstances = getProcessAPI().getArchivedProcessInstances(0, 10, ProcessInstanceCriterion.DEFAULT);
        assertEquals(0, processInstances.size());
        assertEquals(0, archivedProcessInstances.size());

        List<ArchivedProcessInstance> archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(processInstanceToArchive.getId(), 0, 10);
        assertEquals(0, archivedProcessInstanceList.size());

        archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(activeProcessInstance.getId(), 0, 10);
        assertEquals(0, archivedProcessInstanceList.size());

    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process", "archived process instance" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteArchivedProcessIntances() throws Exception {
        // deploy a simple process
        final String userTaskName = "step1";
        final ProcessDefinition processDefinition = deployAndEnableSimpleProcess("myProcess", userTaskName);

        // start a process non completed process
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask(userTaskName, processInstance);

        // delete the process instance
        getProcessAPI().deleteProcessInstance(processInstance.getId());

        // check that all archived process instance related to this process were deleted
        final List<ArchivedProcessInstance> archivedProcessInstanceList = getProcessAPI().getArchivedProcessInstances(processInstance.getId(), 0, 10);
        assertEquals(0, archivedProcessInstanceList.size());

        // delete definition
        disableAndDeleteProcess(processDefinition);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process", "documents" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteDocuments() throws Exception {
        // deploy and instantiate a process containing data and documents
        final String userTaskName = "step1";
        final String url = "http://intranet.bonitasoft.com/private/docStorage/anyValue";
        final ProcessDefinition processDefinition = deployAndEnableProcessWithDocument("myProcess", userTaskName, "doc", url);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask(userTaskName, processInstance.getId());

        // check the number of data and documents
        SearchResult<Document> documentsSearchResult = getProcessAPI().searchDocuments(new SearchOptionsBuilder(0, 10).done());
        assertEquals(1, documentsSearchResult.getCount());

        // delete process instance
        getProcessAPI().deleteProcessInstance(processInstance.getId());

        // check the number of data and documents
        documentsSearchResult = getProcessAPI().searchDocuments(new SearchOptionsBuilder(0, 10).done());
        assertEquals(0, documentsSearchResult.getCount());

        // clean up
        disableAndDeleteProcess(processDefinition);
    }

    private ProcessDefinition deployAndEnableProcessWithDocument(final String processName, final String userTaskName, final String docName, final String url)
            throws BonitaException {
        final String actorName = "delivery";

        final ProcessDefinitionBuilder processDefBuilder = new ProcessDefinitionBuilder().createNewInstance(processName, "1.0");
        processDefBuilder.addActor(actorName);
        final DocumentDefinitionBuilder documentDefinitionBuilder = processDefBuilder.addDocumentDefinition(docName);
        documentDefinitionBuilder.addUrl(url);
        processDefBuilder.addStartEvent("tStart");
        processDefBuilder.addUserTask(userTaskName, actorName);
        processDefBuilder.addEndEvent("tEnd");
        processDefBuilder.addTransition("tStart", userTaskName);
        processDefBuilder.addTransition(userTaskName, "tEnd");

        final ProcessDefinition targetProcessDefinition = deployAndEnableWithActor(processDefBuilder.done(), actorName, pedro);

        return targetProcessDefinition;
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process", "comments" }, jira = "ENGINE-257")
    public void deleteProcessInstanceAlsoDeleteComments() throws Exception {
        // deploy and start a simple process
        final String userTaskName = "step1";
        final ProcessDefinition processDefinition = deployAndEnableSimpleProcess("myProcess", userTaskName);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        waitForUserTask(userTaskName, processInstance.getId());

        // add a comment
        getProcessAPI().addComment(processInstance.getId(), "just do it.");

        SearchResult<Comment> searchResult = getProcessAPI().searchComments(new SearchOptionsBuilder(0, 10).done());
        assertTrue(searchResult.getCount() > 0);

        // delete process instance
        getProcessAPI().deleteProcessInstance(processInstance.getId());

        // check all comments were deleted
        searchResult = getProcessAPI().searchComments(new SearchOptionsBuilder(0, 10).done());
        assertEquals(0, searchResult.getCount());

        // cleanup
        disableAndDeleteProcess(processDefinition);
    }

    @Test
    @Cover(classes = { ProcessManagementAPI.class }, concept = BPMNConcept.PROCESS, keywords = { "delete process", "archived comments" })
    public void deleteProcessInstanceAlsoDeleteArchivedComments() throws Exception {
        // deploy and start a simple process
        final String userTaskName = "etapa1";
        final ProcessDefinition processDefinition = deployAndEnableSimpleProcess("ArchivedCommentsDeletion", userTaskName);
        final ProcessInstance processInstance = getProcessAPI().startProcess(processDefinition.getId());
        final ActivityInstance userTask = waitForUserTask(userTaskName, processInstance.getId());

        // add a comment
        getProcessAPI().addComment(processInstance.getId(), "just do it.");
        assignAndExecuteStep(userTask, pedro.getId());
        waitForProcessToFinish(processInstance);

        SearchResult<ArchivedComment> searchResult = getProcessAPI().searchArchivedComments(new SearchOptionsBuilder(0, 10).done());
        assertTrue(searchResult.getCount() > 0);

        // cleanup
        disableAndDeleteProcess(processDefinition);

        // check all archived comments were deleted along with process instance:
        searchResult = getProcessAPI().searchArchivedComments(new SearchOptionsBuilder(0, 10).done());
        assertEquals(0, searchResult.getCount());
    }

}
