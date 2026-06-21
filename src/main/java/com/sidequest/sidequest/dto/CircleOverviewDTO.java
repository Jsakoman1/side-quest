package com.sidequest.sidequest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircleOverviewDTO {
    private List<CircleContactDTO> circles;
    private List<CircleRequestResponseDTO> incomingRequests;
    private List<CircleRequestResponseDTO> outgoingRequests;
    private List<CircleSearchResultDTO> inviteCandidates;
}
