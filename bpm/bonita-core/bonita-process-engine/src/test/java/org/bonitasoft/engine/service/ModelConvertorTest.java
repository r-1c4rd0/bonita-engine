package org.bonitasoft.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

import org.bonitasoft.engine.api.impl.DummySCustomUserInfoDefinition;
import org.bonitasoft.engine.api.impl.DummySCustomUserInfoValue;
import org.bonitasoft.engine.bpm.data.DataInstance;
import org.bonitasoft.engine.bpm.flownode.ArchivedUserTaskInstance;
import org.bonitasoft.engine.core.process.instance.model.STaskPriority;
import org.bonitasoft.engine.core.process.instance.model.archive.impl.SAUserTaskInstanceImpl;
import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.core.document.api.DocumentService;
import org.bonitasoft.engine.core.document.model.SDocument;
import org.bonitasoft.engine.core.document.model.SMappedDocument;
import org.bonitasoft.engine.data.instance.model.SDataInstance;
import org.bonitasoft.engine.execution.state.CompletedActivityStateImpl;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.identity.CustomUserInfoValue;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.impl.CustomUserInfoDefinitionImpl;
import org.bonitasoft.engine.identity.model.SCustomUserInfoValue;
import org.bonitasoft.engine.identity.model.SUser;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModelConvertorTest {

    @Mock
    private FlowNodeStateManager manager;

    @Test
    public void convertDataInstanceIsTransient() {
        final SDataInstance sDataInstance = mock(SDataInstance.class);
        when(sDataInstance.getClassName()).thenReturn(Integer.class.getName());
        when(sDataInstance.isTransientData()).thenReturn(true);

        final DataInstance dataInstance = ModelConvertor.toDataInstance(sDataInstance);
        assertTrue(dataInstance.isTransientData());
    }

    @Test
    public void convertDataInstanceIsNotTransient() {
        final SDataInstance sDataInstance = mock(SDataInstance.class);
        when(sDataInstance.getClassName()).thenReturn(Integer.class.getName());
        when(sDataInstance.isTransientData()).thenReturn(false);

        final DataInstance dataInstance = ModelConvertor.toDataInstance(sDataInstance);
        assertFalse(dataInstance.isTransientData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getProcessInstanceState_conversionOnUnknownStateShouldThrowException() {
        ModelConvertor.getProcessInstanceState("un_known_state");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getProcessInstanceState_conversionOnNullStateShouldThrowException() {
        ModelConvertor.getProcessInstanceState(null);
    }

    @Test
    public void convertSUserToUserDoesntShowPassword() {
        final SUser sUser = mock(SUser.class);

        final User testUser = ModelConvertor.toUser(sUser);

        assertThat(testUser.getPassword()).isEmpty();
        verify(sUser, never()).getPassword();
    }
    private DocumentService createdMockedDocumentService() {
        DocumentService documentService = mock(DocumentService.class);
        doReturn("url?fileName=document&contentStorageId=123").when(documentService).generateDocumentURL("document","123");
        return documentService;
    }

    private SMappedDocument createMockedDocument() {
        SMappedDocument documentMapping = mock(SMappedDocument.class);
        doReturn("document").when(documentMapping).getFileName();
        doReturn(123l).when(documentMapping).getDocumentId();
        doReturn("whateverUrl").when(documentMapping).getUrl();
        return documentMapping;
    }


    @Test
    public void toArchivedUserTaskInstance_sould_return_the_right_idenfiers() throws Exception {
        final SAUserTaskInstanceImpl sInstance = new SAUserTaskInstanceImpl();
        sInstance.setRootContainerId(1L);
        sInstance.setParentContainerId(2L);
        sInstance.setLogicalGroup(0, 456789456798L);
        sInstance.setLogicalGroup(1, 1L);
        sInstance.setLogicalGroup(2, 456L);
        sInstance.setLogicalGroup(3, 2L);
        sInstance.setStateId(5);
        sInstance.setPriority(STaskPriority.NORMAL);

        when(manager.getState(5)).thenReturn(new CompletedActivityStateImpl());

        final ArchivedUserTaskInstance archivedUserTaskInstance = ModelConvertor.toArchivedUserTaskInstance(sInstance, manager);
        assertThat(archivedUserTaskInstance.getProcessDefinitionId()).isEqualTo(456789456798L);
        assertThat(archivedUserTaskInstance.getRootContainerId()).isEqualTo(1L);
        assertThat(archivedUserTaskInstance.getParentContainerId()).isEqualTo(2L);
        assertThat(archivedUserTaskInstance.getProcessInstanceId()).isEqualTo(2L);
        assertThat(archivedUserTaskInstance.getParentActivityInstanceId()).isEqualTo(456L);
    }

    @Test
    public void getDocument_from_process_instance_and_name_should_return_a_document_with_generated_url_when_it_has_content() throws Exception {

        SMappedDocument documentMapping = createMockedDocument();
        DocumentService documentService = createdMockedDocumentService();
        doReturn(true).when(documentMapping).hasContent();

        Document document = ModelConvertor.toDocument(documentMapping, documentService);

        assertEquals("url?fileName=document&contentStorageId=123", document.getUrl());
    }


    @Test
    public void getDocument_from_process_instance_and_name_should_return_a_document_url_when_is_external_url() throws Exception {

        SMappedDocument documentMapping = createMockedDocument();
        DocumentService documentService = createdMockedDocumentService();
        doReturn(false).when(documentMapping).hasContent();

        Document document = ModelConvertor.toDocument(documentMapping, documentService);

        assertEquals("whateverUrl", document.getUrl());
    }

    @Test
    public void should_convert_server_definition_into_client_definition() {
        CustomUserInfoDefinitionImpl definition = ModelConvertor.convert(
                new DummySCustomUserInfoDefinition(1L, "name", "description"));

        assertThat(definition.getId()).isEqualTo(1L);
        assertThat(definition.getName()).isEqualTo("name");
        assertThat(definition.getDescription()).isEqualTo("description");
    }

    @Test
    public void should_convert_server_value_into_client_value() {
        CustomUserInfoValue value = ModelConvertor.convert(
                new DummySCustomUserInfoValue(2L, 2L, 1L, "value"));

        assertThat(value.getDefinitionId()).isEqualTo(2L);
        assertThat(value.getValue()).isEqualTo("value");
        assertThat(value.getUserId()).isEqualTo(1L);
    }

    @Test
    public void should_return_null_when_trying_to_convert_a_null_value() {
        CustomUserInfoValue value = ModelConvertor.convert((SCustomUserInfoValue) null);

        assertThat(value).isNull();
    }
}