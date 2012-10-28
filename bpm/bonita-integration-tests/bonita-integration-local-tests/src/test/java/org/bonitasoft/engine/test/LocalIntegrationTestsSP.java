package org.bonitasoft.engine.test;

import org.bonitasoft.engine.BPMRemoteTests;
import org.bonitasoft.engine.BPMTestsSP;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Elias Ricken de Medeiros
 */
@RunWith(Suite.class)
@SuiteClasses({
    BPMTestsSP.class,
    BPMLocalSuiteTests.class,
    BPMRemoteTests.class
})
public class LocalIntegrationTestsSP {

}
