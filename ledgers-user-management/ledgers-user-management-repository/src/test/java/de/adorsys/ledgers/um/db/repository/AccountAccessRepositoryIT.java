package de.adorsys.ledgers.um.db.repository;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;
import de.adorsys.ledgers.um.db.domain.*;
import de.adorsys.ledgers.util.Ids;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = UmRepositoryApplication.class)
@DatabaseSetup("SCAMethodsRepositoryTest-db-entries.xml")
@DatabaseTearDown(value={"SCAMethodsRepositoryTest-db-entries.xml"}, type= DatabaseOperation.DELETE_ALL)
public class AccountAccessRepositoryIT {

    @Autowired
    private AccountAccessRepository accountAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void test_create_ok() {

        AccountAccess accountAccess = new AccountAccess();
        accountAccess.setId(Ids.id());
        accountAccess.setIban("FakeIban");
        accountAccess.setAccessType(AccessType.OWNER);
        UserEntity user = new UserEntity();
        user.setId(Ids.id());
        user.setPin("1234");
        user.setLogin("vne");
        user.setEmail("vne@adorsys.de");
        accountAccess.setUser(user);
        user.getAccountAccesses().add(accountAccess);
        accountAccess.setUser(user);
        userRepository.save(user);
        AccountAccess result = accountAccessRepository.findById(accountAccess.getId()).orElse(null);
        Assert.notNull(result);
    }

}

