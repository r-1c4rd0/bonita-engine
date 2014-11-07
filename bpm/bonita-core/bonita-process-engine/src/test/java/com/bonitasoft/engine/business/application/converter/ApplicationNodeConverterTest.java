/*******************************************************************************
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 ******************************************************************************/

package com.bonitasoft.engine.business.application.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bonitasoft.engine.api.ImportError;
import org.bonitasoft.engine.api.ImportStatus;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.exception.ExecutionException;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.exception.profile.SProfileNotFoundException;
import org.bonitasoft.engine.profile.model.SProfile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bonitasoft.engine.business.application.ApplicationService;
import com.bonitasoft.engine.business.application.importer.ImportResult;
import com.bonitasoft.engine.business.application.model.SApplication;
import com.bonitasoft.engine.business.application.model.SApplicationPage;
import com.bonitasoft.engine.business.application.model.impl.SApplicationImpl;
import com.bonitasoft.engine.business.application.xml.ApplicationMenuNode;
import com.bonitasoft.engine.business.application.xml.ApplicationNode;
import com.bonitasoft.engine.business.application.xml.ApplicationPageNode;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationNodeConverterTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationPageNodeConverter pageConverter;

    @Mock
    private ApplicationMenuNodeConverter menuConverter;

    @InjectMocks
    private ApplicationNodeConverter converter;

    @Test
    public void toNode_should_return_convert_all_string_fields() throws Exception {
        //given
        final SApplicationImpl application = new SApplicationImpl("app", "my app", "1.0", new Date().getTime(), 11L, "enabled");
        application.setDescription("this is my app");
        application.setIconPath("/icon.jpg");

        //when
        final ApplicationNode applicationNode = converter.toNode(application);

        //then
        assertThat(applicationNode).isNotNull();
        assertThat(applicationNode.getToken()).isEqualTo("app");
        assertThat(applicationNode.getDisplayName()).isEqualTo("my app");
        assertThat(applicationNode.getVersion()).isEqualTo("1.0");
        assertThat(applicationNode.getDescription()).isEqualTo("this is my app");
        assertThat(applicationNode.getIconPath()).isEqualTo("/icon.jpg");
        assertThat(applicationNode.getState()).isEqualTo("enabled");
        assertThat(applicationNode.getProfile()).isNull();
        assertThat(applicationNode.getHomePage()).isNull();

    }

    @Test
    public void toNode_should_replace_profile_id_by_profile_name() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(7L);
        given(application.getHomePageId()).willReturn(null);
        final SProfile profile = mock(SProfile.class);
        given(profile.getName()).willReturn("admin");

        given(profileService.getProfile(7L)).willReturn(profile);

        //when
        final ApplicationNode applicationNode = converter.toNode(application);

        //then
        assertThat(applicationNode).isNotNull();
        assertThat(applicationNode.getProfile()).isEqualTo("admin");
    }

    @Test(expected = ExecutionException.class)
    public void toNode_should_throw_ExecutionException_when_profileService_throws_exception() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(7L);

        given(profileService.getProfile(7L)).willThrow(new SProfileNotFoundException(""));

        //when
        converter.toNode(application);

        //then exception
    }

    @Test
    public void toNode_should_replaceHomePageId_by_application_page_token() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getHomePageId()).willReturn(8L);
        given(application.getProfileId()).willReturn(null);
        final SApplicationPage homePage = mock(SApplicationPage.class);
        given(homePage.getToken()).willReturn("home");

        given(applicationService.getApplicationPage(8L)).willReturn(homePage);

        //when
        final ApplicationNode applicationNode = converter.toNode(application);

        //then
        assertThat(applicationNode).isNotNull();
        assertThat(applicationNode.getHomePage()).isEqualTo("home");
    }

    @Test(expected = ExecutionException.class)
    public void toNode_should_throw_ExecutionException_when_applicationService_throws_exception() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getHomePageId()).willReturn(8L);
        given(application.getProfileId()).willReturn(null);
        final SApplicationPage homePage = mock(SApplicationPage.class);
        given(homePage.getToken()).willReturn("home");

        given(applicationService.getApplicationPage(8L)).willThrow(new SObjectNotFoundException());

        //when
        converter.toNode(application);

        //then exception
    }

    @Test(expected = ExecutionException.class)
    public void toNodeShouldAddThrowExceptionAtMenuConversion() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(null);
        given(application.getHomePageId()).willReturn(null);

        doThrow(new SBonitaReadException("")).when(menuConverter).addMenusToApplicationNode(anyLong(), anyLong(), any(ApplicationNode.class),
                any(ApplicationMenuNode.class));

        converter.toNode(application);
    }

    @Test
    public void toNodeShouldAddConvertedMenus() throws Exception {
        //given
        final long applicationId = 1191L;
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(null);
        given(application.getHomePageId()).willReturn(null);
        given(application.getId()).willReturn(applicationId);

        doNothing().when(menuConverter).addMenusToApplicationNode(eq(applicationId), isNull(Long.class), any(ApplicationNode.class),
                isNull(ApplicationMenuNode.class));

        //when
        converter.toNode(application);

        //then
        verify(menuConverter).addMenusToApplicationNode(eq(applicationId), isNull(Long.class), any(ApplicationNode.class), isNull(ApplicationMenuNode.class));
    }

    @Test(expected = ExecutionException.class)
    public void toNodeShouldAddThrowExceptionAtPageConversion() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(null);
        given(application.getHomePageId()).willReturn(null);

        given(applicationService.searchApplicationPages(any(QueryOptions.class))).willThrow(new SBonitaReadException(""));

        converter.toNode(application);
    }

    @Test
    public void toNodeShouldAddConvertedPages() throws Exception {
        //given
        final SApplication application = mock(SApplication.class);
        given(application.getProfileId()).willReturn(null);
        given(application.getHomePageId()).willReturn(null);
        final List<SApplicationPage> pages = new ArrayList<SApplicationPage>(1);
        final SApplicationPage page = mock(SApplicationPage.class);
        pages.add(page);

        final ApplicationPageNode pageNode = mock(ApplicationPageNode.class);
        given(pageConverter.toPage(page)).willReturn(pageNode);

        given(applicationService.searchApplicationPages(any(QueryOptions.class))).willReturn(pages).willReturn(Collections.<SApplicationPage> emptyList());

        //when
        final ApplicationNode applicationNode = converter.toNode(application);

        //then
        assertThat(applicationNode.getApplicationPages().size()).isEqualTo(1);
        assertThat(applicationNode.getApplicationPages().get(0)).isEqualTo(pageNode);
    }

    @Test
    public void toSApplication_should_return_ImportResult_with_no_errors_and_application_with_all_fields_except_home_page() throws Exception {
        //given
        final ApplicationNode node = new ApplicationNode();
        node.setDisplayName("My app");
        node.setDescription("This is my app");
        node.setHomePage("home");
        node.setVersion("1.0");
        node.setToken("app");
        node.setIconPath("/icon.jpg");
        node.setProfile("admin");
        node.setState("ENABLED");

        final SProfile profile = mock(SProfile.class);
        given(profile.getId()).willReturn(8L);

        given(profileService.getProfileByName("admin")).willReturn(profile);

        //when
        final ImportResult importResult = converter.toSApplication(node, 1L);

        //then
        assertThat(importResult).isNotNull();

        final SApplication application = importResult.getApplication();
        assertThat(application.getDisplayName()).isEqualTo("My app");
        assertThat(application.getDescription()).isEqualTo("This is my app");
        assertThat(application.getHomePageId()).isNull();
        assertThat(application.getVersion()).isEqualTo("1.0");
        assertThat(application.getToken()).isEqualTo("app");
        assertThat(application.getIconPath()).isEqualTo("/icon.jpg");
        assertThat(application.getProfileId()).isEqualTo(8L);
        assertThat(application.getState()).isEqualTo("ENABLED");
        assertThat(application.getCreatedBy()).isEqualTo(1L);

        final ImportStatus importStatus = importResult.getImportStatus();
        assertThat(importStatus.getName()).isEqualTo("app");
        assertThat(importStatus.getStatus()).isEqualTo(ImportStatus.Status.ADDED);
        assertThat(importStatus.getErrors()).isEmpty();

    }

    @Test
    public void toSApplication_should_return_application_with_null_profile_id_when_node_has_no_profile() throws Exception {
        //given
        final ApplicationNode node = new ApplicationNode();
        node.setProfile(null);

        //when
        final ImportResult importResult = converter.toSApplication(node, 1L);

        //then
        assertThat(importResult).isNotNull();
        assertThat(importResult.getApplication().getProfileId()).isNull();
    }

    @Test
    public void toSApplication_should_return_Import_result_with_errors_and_profile_not_set_when_profile_is_not_found() throws Exception {
        //given
        final ApplicationNode node = new ApplicationNode();
        node.setProfile("admin");
        node.setVersion("1.0");
        node.setToken("app");
        node.setState("ENABLED");

        given(profileService.getProfileByName("admin")).willThrow(new SProfileNotFoundException(""));

        //when
        final ImportResult importResult = converter.toSApplication(node, 1L);

        //then
        assertThat(importResult.getApplication().getProfileId()).isNull();

        final ImportStatus importStatus = importResult.getImportStatus();
        assertThat(importStatus.getName()).isEqualTo("app");
        assertThat(importStatus.getStatus()).isEqualTo(ImportStatus.Status.ADDED);
        assertThat(importStatus.getErrors()).containsExactly(new ImportError("admin", ImportError.Type.PROFILE));
    }

}
