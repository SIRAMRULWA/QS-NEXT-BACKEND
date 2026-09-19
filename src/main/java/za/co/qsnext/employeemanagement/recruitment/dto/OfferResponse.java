package za.co.qsnext.employeemanagement.recruitment.dto;

import za.co.qsnext.employeemanagement.recruitment.Offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID applicationId,
        String jobTitle,
        BigDecimal salaryAmount,
        String currency,
        LocalDate startDate,
        String status,
        OffsetDateTime sentAt,
        OffsetDateTime respondedAt
) {

    public static OfferResponse from(Offer offer) {
        return new OfferResponse(
                offer.getId(), offer.getApplicationId(), offer.getJobTitle(), offer.getSalaryAmount(),
                offer.getCurrency(), offer.getStartDate(), offer.getStatus(), offer.getSentAt(),
                offer.getRespondedAt()
        );
    }
}
