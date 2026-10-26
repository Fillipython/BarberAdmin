package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.FinancialRecord;
import br.com.barberadmin.domain.enums.PaymentMethod;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.dashboard.*;
import br.com.barberadmin.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialRecordRepository financialRecordRepository;

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd 'de' MMMM", new Locale("pt", "BR"));
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", new Locale("pt", "BR"));

    private static final String[] BARBER_COLORS = {
            "#d4af37", "#38bdf8", "#34d399", "#f472b6", "#a78bfa", "#fbbf24", "#fb923c"
    };

    @Transactional(readOnly = true)
    public DashboardDailyDTO getDailyDashboard(LocalDate date, UUID barberId, UserRole role) {
        OffsetDateTime start = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

        List<FinancialRecord> records = (role == UserRole.BARBER && barberId != null)
                ? financialRecordRepository.findByBarberInTimeRange(barberId, start, end)
                : (barberId != null ? financialRecordRepository.findByBarberInTimeRange(barberId, start, end)
                                    : financialRecordRepository.findAllInTimeRange(start, end));

        long grossCents = records.stream().mapToLong(FinancialRecord::getAmountCents).sum();
        long barberCents = records.stream().mapToLong(FinancialRecord::getBarberCommissionCents).sum();
        long shopCents = records.stream().mapToLong(FinancialRecord::getShopNetCents).sum();

        // 1. Donut Formas de Pagamento
        List<DonutSliceDTO> paymentDonut = buildPaymentMethodsDonut(records, grossCents);

        // 2. Donut Participação por Barbeiro
        List<DonutSliceDTO> barbersDonut = buildBarbersDonut(records, grossCents);

        // 3. Extrato detalhado
        List<FinancialReceiptItemDTO> receipts = records.stream().map(r -> {
            String time = r.getPaidAt().atZoneSameInstant(ZONE_BR).format(TIME_FORMATTER);
            return FinancialReceiptItemDTO.builder()
                    .id(r.getId())
                    .clientName(r.getClient() != null ? r.getClient().getName() : "Cliente Avulso")
                    .barberName(r.getBarber().getName())
                    .paymentMethod(r.getPaymentMethod())
                    .amountCents((long) r.getAmountCents())
                    .amountFormatted(formatMoney(r.getAmountCents()))
                    .barberCommissionCents((long) r.getBarberCommissionCents())
                    .shopNetCents((long) r.getShopNetCents())
                    .paidAt(r.getPaidAt())
                    .timeFormatted(time)
                    .build();
        }).collect(Collectors.toList());

        return DashboardDailyDTO.builder()
                .date(date)
                .dateFormatted(date.format(DATE_FORMATTER))
                .grossCents(grossCents)
                .grossFormatted(formatMoney(grossCents))
                .barberCommissionCents(barberCents)
                .barberCommissionFormatted(formatMoney(barberCents))
                .shopNetCents(shopCents)
                .shopNetFormatted(formatMoney(shopCents))
                .totalCompletedAppointments(records.size())
                .paymentMethodsDonut(paymentDonut)
                .barbersDonut(barbersDonut)
                .receipts(receipts)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardMonthlyDTO getMonthlyDashboard(YearMonth yearMonth, UUID barberId, UserRole role) {
        LocalDate startMonth = yearMonth.atDay(1);
        LocalDate endMonth = yearMonth.plusMonths(1).atDay(1);

        OffsetDateTime start = startMonth.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime end = endMonth.atStartOfDay(ZONE_BR).toOffsetDateTime();

        List<FinancialRecord> records = (role == UserRole.BARBER && barberId != null)
                ? financialRecordRepository.findByBarberInTimeRange(barberId, start, end)
                : (barberId != null ? financialRecordRepository.findByBarberInTimeRange(barberId, start, end)
                                    : financialRecordRepository.findAllInTimeRange(start, end));

        long grossCents = records.stream().mapToLong(FinancialRecord::getAmountCents).sum();
        long barberCents = records.stream().mapToLong(FinancialRecord::getBarberCommissionCents).sum();
        long shopCents = records.stream().mapToLong(FinancialRecord::getShopNetCents).sum();

        List<DonutSliceDTO> paymentDonut = buildPaymentMethodsDonut(records, grossCents);
        List<DonutSliceDTO> barbersDonut = buildBarbersDonut(records, grossCents);

        return DashboardMonthlyDTO.builder()
                .yearMonth(yearMonth.toString())
                .monthNameFormatted(startMonth.format(MONTH_FORMATTER))
                .grossCents(grossCents)
                .grossFormatted(formatMoney(grossCents))
                .barberCommissionCents(barberCents)
                .barberCommissionFormatted(formatMoney(barberCents))
                .shopNetCents(shopCents)
                .shopNetFormatted(formatMoney(shopCents))
                .totalCompletedAppointments(records.size())
                .paymentMethodsDonut(paymentDonut)
                .barbersDonut(barbersDonut)
                .build();
    }

    private List<DonutSliceDTO> buildPaymentMethodsDonut(List<FinancialRecord> records, long totalGross) {
        Map<PaymentMethod, Long> map = records.stream()
                .collect(Collectors.groupingBy(FinancialRecord::getPaymentMethod, Collectors.summingLong(FinancialRecord::getAmountCents)));

        Map<PaymentMethod, Long> countMap = records.stream()
                .collect(Collectors.groupingBy(FinancialRecord::getPaymentMethod, Collectors.counting()));

        List<DonutSliceDTO> list = new ArrayList<>();
        for (Map.Entry<PaymentMethod, Long> entry : map.entrySet()) {
            PaymentMethod method = entry.getKey();
            long value = entry.getValue();
            long count = countMap.getOrDefault(method, 0L);
            BigDecimal percentage = totalGross > 0
                    ? BigDecimal.valueOf(value * 100.0 / totalGross).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(DonutSliceDTO.builder()
                    .label(getPaymentMethodLabel(method))
                    .valueCents(value)
                    .formattedValue(formatMoney(value))
                    .percentage(percentage)
                    .color(getPaymentMethodColor(method))
                    .count(count)
                    .build());
        }

        // Ordena por maior valor
        list.sort((a, b) -> Long.compare(b.getValueCents(), a.getValueCents()));
        return list;
    }

    private List<DonutSliceDTO> buildBarbersDonut(List<FinancialRecord> records, long totalGross) {
        Map<String, Long> sumMap = records.stream()
                .collect(Collectors.groupingBy(r -> r.getBarber().getName(), Collectors.summingLong(FinancialRecord::getAmountCents)));

        Map<String, Long> countMap = records.stream()
                .collect(Collectors.groupingBy(r -> r.getBarber().getName(), Collectors.counting()));

        List<DonutSliceDTO> list = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Long> entry : sumMap.entrySet()) {
            long value = entry.getValue();
            long count = countMap.getOrDefault(entry.getKey(), 0L);
            BigDecimal percentage = totalGross > 0
                    ? BigDecimal.valueOf(value * 100.0 / totalGross).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String color = BARBER_COLORS[colorIdx % BARBER_COLORS.length];
            colorIdx++;

            list.add(DonutSliceDTO.builder()
                    .label(entry.getKey())
                    .valueCents(value)
                    .formattedValue(formatMoney(value))
                    .percentage(percentage)
                    .color(color)
                    .count(count)
                    .build());
        }

        list.sort((a, b) -> Long.compare(b.getValueCents(), a.getValueCents()));
        return list;
    }

    private String getPaymentMethodLabel(PaymentMethod method) {
        return switch (method) {
            case PIX -> "PIX";
            case DINHEIRO -> "Dinheiro";
            case CARTAO_CREDITO -> "Cartão de Crédito";
            case CARTAO_DEBITO -> "Cartão de Débito";
            case OUTRO -> "Outro";
        };
    }

    private String getPaymentMethodColor(PaymentMethod method) {
        return switch (method) {
            case PIX -> "#10b981";          // Verde esmeralda
            case DINHEIRO -> "#f59e0b";     // Âmbar
            case CARTAO_CREDITO -> "#3b82f6"; // Azul
            case CARTAO_DEBITO -> "#8b5cf6";  // Roxo
            case OUTRO -> "#6b7280";        // Cinza
        };
    }

    private String formatMoney(long cents) {
        double val = cents / 100.0;
        return String.format("R$ %.2f", val).replace(".", ",");
    }
}
