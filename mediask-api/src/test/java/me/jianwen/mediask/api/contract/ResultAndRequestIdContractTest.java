package me.jianwen.mediask.api.contract;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class ResultAndRequestIdContractTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .addFilters(new RequestCorrelationFilter())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void result_WhenNoRequestIdHeader_GenerateAndWriteBack() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/result"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.code").value(ErrorCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.msg").value(ErrorCode.SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.value").value("ok"))
                .andReturn();

        String requestId = assertJsonRequestIdMatchesHeader(mvcResult);
        assertTrue(requestId.startsWith("req_"));
    }

    @Test
    void result_WhenRequestIdHeaderExists_ReuseOriginalValue() throws Exception {
        String requestId = "req_existing_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/result")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId)
                        .header(RequestConstants.LEGACY_TRACE_ID_HEADER, "trace_legacy_should_not_win"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void result_WhenOnlyLegacyTraceIdHeaderExists_ReuseLegacyValue() throws Exception {
        String legacyRequestId = "trace_legacy_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/result")
                        .header(RequestConstants.LEGACY_TRACE_ID_HEADER, legacyRequestId))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, legacyRequestId))
                .andReturn();

        assertEquals(legacyRequestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void plain_WhenControllerReturnsString_DoNotWrapAsResult() throws Exception {
        mockMvc.perform(get("/test/contracts/plain"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, startsWith("req_")))
                .andExpect(content().string("plain-response"));
    }

    @Test
    void biz_WhenBizExceptionThrown_ReturnMappedStatusCodeAndRequestId() throws Exception {
        String requestId = "req_biz_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/biz")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isForbidden())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()))
                .andExpect(jsonPath("$.msg").value("doctor access denied"))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void sys_WhenSysExceptionThrown_PreserveCustomMessage() throws Exception {
        String requestId = "req_sys_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/sys")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value("upstream service unavailable"))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void typeMismatch_WhenRequestParameterInvalid_ReturnBadRequestAndStableCode() throws Exception {
        String requestId = "req_bad_request_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/type-mismatch")
                        .param("count", "not-a-number")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
                .andExpect(jsonPath("$.msg").value(ErrorCode.INVALID_PARAMETER.getMessage()))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void echo_WhenRequestBodyMalformed_ReturnBadRequestAndStableCode() throws Exception {
        String requestId = "req_json_001";

        MvcResult mvcResult = mockMvc.perform(post("/test/contracts/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void illegalArgument_WhenThrown_ReturnBadRequestAndStableMessage() throws Exception {
        String requestId = "req_illegal_argument_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/illegal-argument")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
                .andExpect(jsonPath("$.msg").value(ErrorCode.INVALID_PARAMETER.getMessage()))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    @Test
    void missingParameter_WhenRequiredParameterMissing_ReturnBadRequestAndSpecificMessage() throws Exception {
        String requestId = "req_missing_param_001";

        MvcResult mvcResult = mockMvc.perform(get("/test/contracts/missing-parameter")
                        .header(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestConstants.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.getCode()))
                .andExpect(jsonPath("$.msg").value("keyword is required"))
                .andReturn();

        assertEquals(requestId, assertJsonRequestIdMatchesHeader(mvcResult));
    }

    private String assertJsonRequestIdMatchesHeader(MvcResult mvcResult) throws Exception {
        String headerRequestId = mvcResult.getResponse().getHeader(RequestConstants.REQUEST_ID_HEADER);
        assertNotNull(headerRequestId);

        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsByteArray());
        JsonNode requestIdNode = jsonNode.get("requestId");
        JsonNode timestampNode = jsonNode.get("timestamp");

        assertNotNull(requestIdNode);
        assertEquals(headerRequestId, requestIdNode.asText());
        assertNotNull(timestampNode);
        assertTrue(timestampNode.asLong() > 0L);
        return headerRequestId;
    }

    @RestController
    @RequestMapping("/test/contracts")
    static class TestController {

        @GetMapping("/result")
        Result<EchoResponse> result() {
            return Result.ok(new EchoResponse("ok"));
        }

        @GetMapping(value = "/plain", produces = MediaType.TEXT_PLAIN_VALUE)
        String plain() {
            return "plain-response";
        }

        @GetMapping("/biz")
        Result<Void> biz() {
            throw new BizException(ErrorCode.FORBIDDEN, "doctor access denied");
        }

        @GetMapping("/sys")
        Result<Void> sys() {
            throw new SysException(ErrorCode.SYSTEM_ERROR, "upstream service unavailable");
        }

        @GetMapping("/type-mismatch")
        Result<Integer> typeMismatch(@RequestParam Integer count) {
            return Result.ok(count);
        }

        @PostMapping("/echo")
        Result<EchoRequest> echo(@RequestBody EchoRequest request) {
            return Result.ok(request);
        }

        @GetMapping("/illegal-argument")
        Result<Void> illegalArgument() {
            throw new IllegalArgumentException("No enum constant me.jianwen.mediask.domain.outpatient.model.ClinicType.UNKNOWN");
        }

        @GetMapping("/missing-parameter")
        Result<String> missingParameter(@RequestParam String keyword) {
            return Result.ok(keyword);
        }
    }

    private record EchoRequest(String value) {
    }

    private record EchoResponse(String value) {
    }
}
