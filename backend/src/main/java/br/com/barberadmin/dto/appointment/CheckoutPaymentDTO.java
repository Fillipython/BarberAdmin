package br.com.barberadmin.dto.appointment;

import br.com.barberadmin.domain.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPaymentDTO {

    @NotNull(message = "A forma de pagamento é obrigatória")
    private PaymentMethod paymentMethod;

    @NotNull(message = "O valor final é obrigatório")
    @Min(value = 0, message = "O valor não pode ser negativo")
    private Integer finalAmountCents;

    private String notes;
}
