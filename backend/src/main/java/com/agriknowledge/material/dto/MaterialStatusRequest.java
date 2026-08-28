package com.agriknowledge.material.dto;

import com.agriknowledge.material.MaterialStatus;
import jakarta.validation.constraints.NotNull;

public record MaterialStatusRequest(@NotNull MaterialStatus status) {
}
