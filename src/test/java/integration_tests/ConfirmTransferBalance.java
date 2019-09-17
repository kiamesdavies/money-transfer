package integration_tests;

import org.jsmart.zerocode.core.domain.JsonTestCase;
import org.jsmart.zerocode.core.domain.TargetEnv;
import org.jsmart.zerocode.core.runner.ZeroCodeUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@TargetEnv("server_host.properties")
@RunWith(ZeroCodeUnitRunner.class)
public class ConfirmTransferBalance {


    @Test
    @JsonTestCase("load_tests/confirm_account_balance.json")
    public void confirm_balance() throws Exception {

    }
}
