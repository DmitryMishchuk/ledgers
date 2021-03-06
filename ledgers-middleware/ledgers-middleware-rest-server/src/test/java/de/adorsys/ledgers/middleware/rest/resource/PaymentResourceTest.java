package de.adorsys.ledgers.middleware.rest.resource;

import de.adorsys.ledgers.middleware.api.domain.payment.PaymentProductTO;
import de.adorsys.ledgers.middleware.api.domain.payment.PaymentTypeTO;
import de.adorsys.ledgers.middleware.api.domain.payment.SinglePaymentTO;
import de.adorsys.ledgers.middleware.api.domain.payment.TransactionStatusTO;
import de.adorsys.ledgers.middleware.api.exception.PaymentNotFoundMiddlewareException;
import de.adorsys.ledgers.middleware.api.service.MiddlewareService;
import de.adorsys.ledgers.middleware.rest.exception.ExceptionAdvisor;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pro.javatar.commons.reader.JsonReader;
import pro.javatar.commons.reader.YamlReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentResourceTest {

    public static final String PAYMENT_ID = "myPaymentId";
    private MockMvc mockMvc;

    @InjectMocks
    private PaymentResource resource;

    @Mock
    private MiddlewareService middlewareService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mockMvc = MockMvcBuilders
                          .standaloneSetup(resource)
                          .setControllerAdvice(new ExceptionAdvisor())
                          .setMessageConverters(new MappingJackson2HttpMessageConverter())
                          .build();
    }

    @Test
    public void getPaymentStatusById() throws Exception {

        when(middlewareService.getPaymentStatusById(PAYMENT_ID)).thenReturn(TransactionStatusTO.ACSP);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/payments/{id}/status", PAYMENT_ID))
                                      .andDo(print())
                                      .andExpect(status().is(HttpStatus.OK.value()))
                                      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                                      .andReturn();

        verify(middlewareService, times(1)).getPaymentStatusById(PAYMENT_ID);
    }

    @Test
    public void getPaymentStatusByIdNotFound() throws Exception {
        when(middlewareService.getPaymentStatusById(PAYMENT_ID))
                .thenThrow(new PaymentNotFoundMiddlewareException("Payment with id=" + PAYMENT_ID + " not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/payments/{id}/status", PAYMENT_ID))
                .andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andReturn();

        verify(middlewareService, times(1)).getPaymentStatusById(PAYMENT_ID);
    }

    @Test
    public void getPaymentById() throws Exception {
        when(middlewareService.getPaymentById(PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, PAYMENT_ID)).thenReturn(readYml(SinglePaymentTO.class, "PaymentSingleTO.yml"));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                "/payments/{payment-type}/{payment-product}/{paymentId}", PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, PAYMENT_ID))
                                      .andDo(print())
                                      .andExpect(status().is(HttpStatus.OK.value()))
                                      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                                      .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        SinglePaymentTO actual = strToObj(content, SinglePaymentTO.class);

        assertThat(actual.getPaymentId(), is(PAYMENT_ID));
        assertThat(mvcResult.getResponse().getStatus(), is(200));

        verify(middlewareService, times(1)).getPaymentById(PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, PAYMENT_ID);
    }

    @Test
    public void getPaymentById_Error() throws Exception {
        when(middlewareService.getPaymentById(PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, "WRONG_ID"))
                .thenThrow(PaymentNotFoundMiddlewareException.class);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get(
                "/payments/{payment-type}/{payment-product}/{paymentId}", PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, "WRONG_ID"))
                                      .andDo(print())
                                      .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                                      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                                      .andReturn();

        assertThat(mvcResult.getResponse().getStatus(), is(404));
        verify(middlewareService, times(1)).getPaymentById(PaymentTypeTO.SINGLE, PaymentProductTO.SEPA, "WRONG_ID");
    }

    @Test
    public void initiatePayment_Single() throws Exception {
        when(middlewareService.initiatePayment(any(), any())).thenReturn(readYml(SinglePaymentTO.class, "PaymentSingleTO.yml"));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(
                "/payments/{paymentType}", PaymentTypeTO.SINGLE
                ).contentType(APPLICATION_JSON_UTF8).content(fileToString("SinglePayment.json"))
        )
                                      .andDo(print())
                                      .andExpect(status().is(HttpStatus.CREATED.value()))
                                      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                                      .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        SinglePaymentTO actual = strToObj(content, SinglePaymentTO.class);

        assertThat(actual.getPaymentId(), is(PAYMENT_ID));
        assertThat(mvcResult.getResponse().getStatus(), is(201));

        verify(middlewareService, times(1)).initiatePayment(any(), eq(PaymentTypeTO.SINGLE));
    }

    @Test
    public void executePaymentNoSca() throws Exception {
        when(middlewareService.executePayment(any())).thenReturn(TransactionStatusTO.ACSP);

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post(
                "/payments/execute-no-sca/{payment-id}/{payment-product}/{payment-type}", PAYMENT_ID, PaymentProductTO.SEPA, PaymentTypeTO.SINGLE))
                                      .andDo(print())
                                      .andExpect(status().is(HttpStatus.OK.value()))
                                      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                                      .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        TransactionStatusTO actual = JsonReader.getInstance().getObjectFromString(content, TransactionStatusTO.class);
        assertThat(actual).isEqualTo(TransactionStatusTO.ACSP);
        verify(middlewareService, times(1)).executePayment(any());
    }

    private <T> T strToObj(String source, Class<T> tClass) {
        try {
            return JsonReader.getInstance().getObjectFromString(source, tClass);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Can't build object from the string", e);
        }
    }

    private String fileToString(String source) {
        try {
        	return IOUtils.toString(PaymentResourceTest.class.getResourceAsStream(source), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Can't build object from the string", e);
        }
    }

    private static <T> T readYml(Class<T> aClass, String fileName) {
        try {
            return YamlReader.getInstance().getObjectFromResource(PaymentResource.class, fileName, aClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}