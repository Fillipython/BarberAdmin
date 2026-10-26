package br.com.barberadmin.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonutSliceDTO {

    private String label;
    private Long valueCents;
    private String formattedValue;
    private BigDecimal percentage;
    private String color;
    private Long count;
}
