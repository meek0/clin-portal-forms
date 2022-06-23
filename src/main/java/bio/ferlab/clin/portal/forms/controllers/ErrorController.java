package bio.ferlab.clin.portal.forms.controllers;

import bio.ferlab.clin.portal.forms.exceptions.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ErrorController {

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<String> handleException(SecurityException e) {
    return new ResponseEntity<>(e.getReason(), e.getStatus());
  }

}
