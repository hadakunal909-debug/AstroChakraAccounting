package com.astrochakra.accounting.web.dto;

public record RemovalRequest(String date, String time, String type, String label, String detail, String reason) {
}
