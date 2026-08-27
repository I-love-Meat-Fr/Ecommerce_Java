package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReq {
    
    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Rating phải từ 1 đến 5 sao")
    private Integer rating;
    
    @NotBlank(message = "Nội dung đánh giá không được để trống")
    private String comment;
}
