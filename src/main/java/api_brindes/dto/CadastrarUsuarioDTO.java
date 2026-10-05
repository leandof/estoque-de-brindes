package api_brindes.dto;
import jakarta.validation.constraints.*;
public record CadastrarUsuarioDTO(@NotBlank @Size(max = 255) String login,
                                 @NotBlank @Size(max = 72) String senha) {}
