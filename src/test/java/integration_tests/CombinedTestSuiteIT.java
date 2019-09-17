package integration_tests;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MultipleTransferLoad.class,
        ConfirmTransferBalance.class,
})
public class CombinedTestSuiteIT {
}
