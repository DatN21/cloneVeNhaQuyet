package com.dulich.toudulich.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TourResponse {
    @JsonProperty("tour_name")
    String tourName;

    int days;

    @JsonProperty("start_date")
    Date startDate;

    String destination;

    @JsonProperty("tour_type")
    String tourType;

    @JsonProperty("departure_location")
    String departureLocation;

    String status;

    float price;
}