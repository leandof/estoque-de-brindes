package api_brindes.dto;
import jakarta.validation.constraints.*;
public record AtualizarValorDTO(@NotNull @PositiveOrZero Double valor) {}
