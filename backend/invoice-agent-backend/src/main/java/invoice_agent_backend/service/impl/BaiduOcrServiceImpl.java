package invoice_agent_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.OcrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/*
 * Baidu real invoice OCR implementation.
 *
 * Enabled only when:
 *
 * ocr.provider=baidu
 *
 * Workflow only depends on OcrService, so the rest
 * of the audit workflow does not need to know
 * whether OCR comes from Mock or Baidu.
 */
@Service
@ConditionalOnProperty(
        name = "ocr.provider",
        havingValue = "baidu"
)
public class BaiduOcrServiceImpl
        implements OcrService {

    private static final String TOKEN_URL =
            "https://aip.baidubce.com/oauth/2.0/token";

    private static final String VAT_INVOICE_URL =
            "https://aip.baidubce.com/rest/2.0/ocr/v1/vat_invoice";

    private final String apiKey;

    private final String secretKey;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public BaiduOcrServiceImpl(
            @Value("${ocr.baidu.api-key}")
            String apiKey,

            @Value("${ocr.baidu.secret-key}")
            String secretKey,

            ObjectMapper objectMapper) {

        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;

        this.restClient =
                RestClient.create();
    }

    @Override
    public InvoiceInfo recognizeInvoice(
            Long taskId,
            String filePath) {

        if (taskId == null) {
            throw new RuntimeException(
                    "OCR taskId cannot be null"
            );
        }

        if (filePath == null
                || filePath.trim().isEmpty()) {

            throw new RuntimeException(
                    "OCR filePath cannot be empty"
            );
        }

        validateConfiguration();

        try {

            /*
             * Step 1:
             * Read uploaded invoice file.
             */
            byte[] fileBytes =
                    Files.readAllBytes(
                            Path.of(filePath)
                    );

            /*
             * Step 2:
             * Convert image into Base64.
             */
            String base64Image =
                    Base64.getEncoder()
                            .encodeToString(
                                    fileBytes
                            );

            /*
             * Step 3:
             * Obtain Baidu access token.
             */
            String accessToken =
                    getAccessToken();

            /*
             * Step 4:
             * Call Baidu VAT invoice OCR.
             */
            String rawResponse =
                    callVatInvoiceApi(
                            accessToken,
                            base64Image
                    );

            /*
             * Step 5:
             * Convert Baidu response
             * into our own InvoiceInfo.
             */
            return convertToInvoiceInfo(
                    taskId,
                    rawResponse
            );

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Baidu OCR failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /*
     * Check whether API credentials exist.
     */
    private void validateConfiguration() {

        if (isBlank(apiKey)
                || isBlank(secretKey)) {

            throw new RuntimeException(
                    "Baidu OCR API Key or Secret Key is not configured"
            );
        }
    }

    /*
     * Obtain access_token using
     * API Key + Secret Key.
     */
    private String getAccessToken()
            throws Exception {

        String response =
                restClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .scheme("https")
                                        .host("aip.baidubce.com")
                                        .path("/oauth/2.0/token")
                                        .queryParam(
                                                "grant_type",
                                                "client_credentials"
                                        )
                                        .queryParam(
                                                "client_id",
                                                apiKey
                                        )
                                        .queryParam(
                                                "client_secret",
                                                secretKey
                                        )
                                        .build()
                        )
                        .retrieve()
                        .body(String.class);

        if (isBlank(response)) {
            throw new RuntimeException(
                    "Baidu access token response is empty"
            );
        }

        JsonNode root =
                objectMapper.readTree(
                        response
                );

        if (root.has("error")) {

            throw new RuntimeException(
                    "Failed to obtain Baidu access token: "
                            + root.path(
                            "error_description"
                    ).asText()
            );
        }

        String accessToken =
                root.path(
                        "access_token"
                ).asText();

        if (isBlank(accessToken)) {

            throw new RuntimeException(
                    "Baidu access token is missing"
            );
        }

        return accessToken;
    }

    /*
     * Call VAT invoice OCR API.
     */
    private String callVatInvoiceApi(
            String accessToken,
            String base64Image) {

        MultiValueMap<String, String>
                form =
                new LinkedMultiValueMap<>();

        form.add(
                "image",
                base64Image
        );

        /*
         * normal:
         * normal VAT invoice,
         * electronic invoice,
         * special VAT invoice.
         */
        form.add(
                "type",
                "normal"
        );

        String response =
                restClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .scheme("https")
                                        .host("aip.baidubce.com")
                                        .path(
                                                "/rest/2.0/ocr/v1/vat_invoice"
                                        )
                                        .queryParam(
                                                "access_token",
                                                accessToken
                                        )
                                        .build()
                        )
                        .contentType(
                                MediaType
                                        .APPLICATION_FORM_URLENCODED
                        )
                        .body(form)
                        .retrieve()
                        .body(String.class);

        if (isBlank(response)) {

            throw new RuntimeException(
                    "Baidu OCR response is empty"
            );
        }

        return response;
    }

    /*
     * Adapter:
     *
     * Baidu response
     *      ↓
     * InvoiceInfo
     *
     * The rest of the application
     * never depends on Baidu field names.
     */
    private InvoiceInfo convertToInvoiceInfo(
            Long taskId,
            String rawResponse)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(
                        rawResponse
                );

        /*
         * Baidu returns error_code
         * when OCR fails.
         */
        if (root.has("error_code")) {

            throw new RuntimeException(
                    "Baidu OCR API error: "
                            + root.path(
                            "error_code"
                    ).asText()
                            + " - "
                            + root.path(
                            "error_msg"
                    ).asText()
            );
        }

        JsonNode words =
                root.path(
                        "words_result"
                );

        if (words.isMissingNode()
                || words.isNull()
                || !words.isObject()) {

            throw new RuntimeException(
                    "Baidu OCR returned no invoice information"
            );
        }

        InvoiceInfo invoiceInfo =
                new InvoiceInfo();

        invoiceInfo.setTaskId(
                taskId
        );

        /*
         * Baidu:
         * InvoiceNum
         *
         * Our system:
         * invoiceNo
         */
        invoiceInfo.setInvoiceNo(
                text(
                        words,
                        "InvoiceNum"
                )
        );

        /*
         * InvoiceDate
         * e.g. 2025年08月18日
         */
        invoiceInfo.setInvoiceDate(
                parseInvoiceDate(
                        text(
                                words,
                                "InvoiceDate"
                        )
                )
        );

        /*
         * AmountInFiguers =
         * total amount including tax.
         *
         * This is closer to the amount
         * actually reimbursed.
         */
        invoiceInfo.setAmount(
                decimal(
                        text(
                                words,
                                "AmountInFiguers"
                        )
                )
        );

        /*
         * TotalTax =
         * total tax amount.
         */
        invoiceInfo.setTaxAmount(
                decimal(
                        text(
                                words,
                                "TotalTax"
                        )
                )
        );

        invoiceInfo.setBuyerName(
                text(
                        words,
                        "PurchaserName"
                )
        );

        invoiceInfo.setSellerName(
                text(
                        words,
                        "SellerName"
                )
        );

        invoiceInfo.setInvoiceType(
                text(
                        words,
                        "InvoiceType"
                )
        );

        /*
         * Keep complete vendor response.
         *
         * Useful for:
         * - debugging
         * - auditing
         * - future field extraction
         */
        invoiceInfo.setRawJson(
                rawResponse
        );

        return invoiceInfo;
    }

    private String text(
            JsonNode node,
            String fieldName) {

        JsonNode value =
                node.get(fieldName);

        if (value == null
                || value.isNull()) {

            return null;
        }

        String result =
                value.asText();

        return isBlank(result)
                ? null
                : result.trim();
    }

    private BigDecimal decimal(
            String value) {

        if (isBlank(value)) {
            return null;
        }

        try {

            String normalized =
                    value
                            .replace(",", "")
                            .replace("￥", "")
                            .replace("¥", "")
                            .trim();

            return new BigDecimal(
                    normalized
            );

        } catch (NumberFormatException e) {

            throw new RuntimeException(
                    "Invalid OCR amount: "
                            + value
            );
        }
    }

    private LocalDateTime parseInvoiceDate(
            String value) {

        if (isBlank(value)) {
            return null;
        }

        String normalized =
                value.trim();

        DateTimeFormatter[]
                formatters = {

                DateTimeFormatter.ofPattern(
                        "yyyy年MM月dd日"
                ),

                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd"
                ),

                DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd"
                )
        };

        for (DateTimeFormatter formatter
                : formatters) {

            try {

                LocalDate date =
                        LocalDate.parse(
                                normalized,
                                formatter
                        );

                return date.atStartOfDay();

            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException(
                "Unsupported invoice date format: "
                        + value
        );
    }

    private boolean isBlank(
            String value) {

        return value == null
                || value.trim().isEmpty();
    }
}