package api_brindes.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Mantém o VARCHAR existente e aceita os valores legados em minúsculas. */
@Converter
public class TipoMovimentacaoConverter implements AttributeConverter<TipoMovimentacao, String> {
    public String convertToDatabaseColumn(TipoMovimentacao tipo) {
        return tipo == null ? null : tipo.name();
    }
    public TipoMovimentacao convertToEntityAttribute(String valor) {
        return valor == null ? null : TipoMovimentacao.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
