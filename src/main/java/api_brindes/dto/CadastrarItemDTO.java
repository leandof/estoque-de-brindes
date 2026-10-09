package api_brindes.dto;

import jakarta.validation.constraints.*;

public record CadastrarItemDTO(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 100) String nome,
        @NotNull @PositiveOrZero Integer quantidade,
        @NotNull @PositiveOrZero @DecimalMax("999999999.99") @Digits(integer = 9, fraction = 2) Double valor) {}
