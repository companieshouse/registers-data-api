package uk.gov.companieshouse.registers.config;

import static uk.gov.companieshouse.registers.RegistersApplication.NAMESPACE;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.registers.exception.BadRequestException;
import uk.gov.companieshouse.registers.exception.ServiceUnavailableException;
import uk.gov.companieshouse.registers.logging.DataMapHolder;

@ControllerAdvice
public class ExceptionHandlerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    /**
     * BadRequestException exception handler. Thrown when data is given in the wrong format.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {BadRequestException.class, DateTimeParseException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleBadRequestException(Exception ex, WebRequest request) {
        LOGGER.error("Bad request, response code: %s".formatted(HttpStatus.BAD_REQUEST), ex, DataMapHolder.getLogMap());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "Bad request.");
        request.setAttribute("jakarta.servlet.error.exception", ex, 0);
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    /**
     * ServiceUnavailableException exception handler. To be thrown when there are connection issues.
     *
     * @param ex      exception to handle.
     * @param request request.
     * @return error response to return.
     */
    @ExceptionHandler(value = {ServiceUnavailableException.class, DataAccessException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(Exception ex,
            WebRequest request) {
        LOGGER.info("Service unavailable, response code: %s".formatted(HttpStatus.SERVICE_UNAVAILABLE),
                DataMapHolder.getLogMap());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "Service unavailable.");
        request.setAttribute("jakarta.servlet.error.exception", ex, 0);
        return new ResponseEntity<>(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
