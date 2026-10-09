package api_brindes.dto;
import jakarta.validation.constraints.*;
public record AtualizarValorDTO(@NotNull @PositiveOrZero @DecimalMax("999999999.99") @Digits(integer = 9, fraction = 2) Double valor) {}
