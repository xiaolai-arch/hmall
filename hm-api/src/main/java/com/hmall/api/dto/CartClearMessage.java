package com.hmall.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartClearMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Collection<Long> itemIds;
}