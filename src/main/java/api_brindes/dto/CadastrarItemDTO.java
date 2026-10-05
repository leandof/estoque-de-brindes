package api_brindes.dto;

import jakarta.validation.constraints.*;

public record CadastrarItemDTO(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 100) String nome,
        @NotNull @PositiveOrZero Integer quantidade,
        @NotNull @PositiveOrZero Double valor) {}
