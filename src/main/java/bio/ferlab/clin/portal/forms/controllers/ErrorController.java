package bio.ferlab.clin.portal.forms.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class ErrorController {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception e) {
    log.error("", e); // hide from the user + log the reason
    return new ResponseEntity<>("internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<String> handleException(ResponseStatusException e) {
    return new ResponseEntity<>(e.getReason(), e.getStatus());
  }

}
