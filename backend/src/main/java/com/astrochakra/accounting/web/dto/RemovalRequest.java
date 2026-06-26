package com.astrochakra.accounting.web.dto;

import java.time.LocalDate;

public record RemovalRequest(LocalDate date, String time, String type, String label, String detail, String reason) {
}
