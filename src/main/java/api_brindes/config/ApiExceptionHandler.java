package api_brindes.config;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<?> autenticacao(Exception e) {
        return ResponseEntity.status(401).body(Map.of("mensagem", "Usuário ou senha inválidos."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> regra(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", e.getMessage()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensagem",
                e.getReason() == null ? "Operação não permitida." : e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
        var campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Verifique os campos informados.", "erros", campos));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> formato(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("mensagem",
                "Dados inválidos ou campos não permitidos. Use ENTRADA/SAIDA e datas AAAA-MM-DD."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflito(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of("mensagem",
                "Conflito de dados: código duplicado ou registro vinculado a histórico."));
    }
}
