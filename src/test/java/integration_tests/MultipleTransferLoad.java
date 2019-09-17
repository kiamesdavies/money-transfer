package integration_tests;

import org.jsmart.zerocode.core.domain.JsonTestCase;
import org.jsmart.zerocode.core.domain.LoadWith;
import org.jsmart.zerocode.core.domain.TestMapping;
import org.jsmart.zerocode.core.runner.parallel.ZeroCodeLoadRunner;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@LoadWith("load_generation.properties")
@TestMapping(testClass = AccountService.class, testMethod = "testTransfer")
@RunWith(ZeroCodeLoadRunner.class)
public class MultipleTransferLoad {

    @AfterClass
    @Test
    @JsonTestCase("load_tests/create_get_and_transfer_get_account.json")
    public static void confirmBalance(){

    }
}
