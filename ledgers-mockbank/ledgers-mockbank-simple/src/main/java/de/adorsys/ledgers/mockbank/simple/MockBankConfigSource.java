package de.adorsys.ledgers.mockbank.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import de.adorsys.ledgers.deposit.api.service.domain.ASPSPConfigData;
import de.adorsys.ledgers.deposit.api.service.domain.ASPSPConfigSource;
import de.adorsys.ledgers.deposit.api.service.domain.LedgerAccountModel;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Component
public class MockBankConfigSource implements ASPSPConfigSource {
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public ASPSPConfigData aspspConfigData() {
        InputStream inputStream = MockBankConfigSource.class.getResourceAsStream("aspsps-config.yml");
        try {
            return mapper.readValue(inputStream, ASPSPConfigData.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<LedgerAccountModel> chartOfAccount(String coaFile) {
        InputStream inputStream = MockBankConfigSource.class.getResourceAsStream(coaFile);
        LedgerAccountModel[] ledgerAccounts;
        try {
            ledgerAccounts = mapper.readValue(inputStream, LedgerAccountModel[].class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return Arrays.asList(ledgerAccounts);
    }

}
