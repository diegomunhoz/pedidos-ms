package br.com.food.pedidos.dto;

import lombok.*;

@Data
public class ItemDoPedidoDto {

    private Long id;
    private Integer quantidade;
    private String descricao;

}
