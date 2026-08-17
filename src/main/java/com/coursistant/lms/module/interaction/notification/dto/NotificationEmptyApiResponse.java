package com.coursistant.lms.module.interaction.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * OpenAPI success envelope when {@code data} is null. Not used as a runtime type.
 */
@Schema(name = "NotificationEmptyApiResponse",
        description = "Success envelope with no payload (data omitted/null)")
public class NotificationEmptyApiResponse {

    @Schema(description = "HTTP status mirrored in body", example = "200")
    private Integer status;
    @Schema(description = "Stable machine code", example = "SUCCESS")
    private String code;
    @Schema(description = "Always null on this operation", nullable = true)
    private Object data;
    @Schema(description = "Human-readable message", example = "Success")
    private String message;
    @Schema(description = "ISO-8601 timestamp", format = "date-time", example = "2026-08-17T18:00:00Z")
    private String timestamp;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
