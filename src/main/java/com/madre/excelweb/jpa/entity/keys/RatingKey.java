package com.madre.excelweb.jpa.entity.keys;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class RatingKey implements Serializable {
    private Long gameId;
    private Long userId;
}
