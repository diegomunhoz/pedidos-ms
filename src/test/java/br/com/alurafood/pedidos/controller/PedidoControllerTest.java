package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.repository.PedidoRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PedidoControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // Limpa o banco de dados antes de cada teste
        pedidoRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /pedidos - Deve listar todos os pedidos com sucesso (200 OK)")
    void listarTodos_Success() {
        // Cenário: Criar alguns pedidos de teste
        PedidoDto pedido1 = createPedidoDto("Pizza", 2);
        realizaPedido(pedido1);
        PedidoDto pedido2 = createPedidoDto("Refrigerante", 1);
        realizaPedido(pedido2);

        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("$", hasSize(2))
            .body("[0].itens[0].descricao", Matchers.anyOf(equalTo("Pizza"), equalTo("Refrigerante"))) // Order not guaranteed
            .body("[1].itens[0].descricao", Matchers.anyOf(equalTo("Pizza"), equalTo("Refrigerante")));
    }

    @Test
    @DisplayName("GET /pedidos - Deve retornar lista vazia quando não há pedidos (200 OK)")
    void listarTodos_EmptyList() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("$", hasSize(0));
    }

    @Test
    @DisplayName("GET /pedidos/{id} - Deve listar um pedido por ID com sucesso (200 OK)")
    void listarPorId_Success() {
        // Cenário: Criar um pedido de teste
        PedidoDto pedidoCriado = realizaPedido(createPedidoDto("Hambúrguer", 1));
        Long pedidoId = pedidoCriado.getId();

        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos/{id}", pedidoId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(pedidoId.intValue()))
            .body("status", equalTo(Status.REALIZADO.name()))
            .body("itens[0].descricao", equalTo("Hambúrguer"));
    }

    @Test
    @DisplayName("GET /pedidos/{id} - Deve retornar 404 Not Found para ID inexistente")
    void listarPorId_NotFound() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos/{id}", 9999L) // ID que certamente não existe
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("GET /pedidos/{id} - Deve retornar 400 Bad Request para ID inválido (não numérico)")
    void listarPorId_InvalidId() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos/{id}", "abc") // ID não numérico
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("GET /pedidos/porta - Deve retornar a porta da aplicação com sucesso (200 OK)")
    void retornaPorta_Success() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos/porta")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body(containsString(String.valueOf(port))); // Verifica se a porta está na resposta
    }

    @Test
    @DisplayName("POST /pedidos - Deve realizar um pedido com sucesso (201 Created)")
    void realizaPedido_Success() {
        PedidoDto pedidoRequest = createPedidoDto("Prato Feito", 1);

        given()
            .contentType(ContentType.JSON)
            .body(pedidoRequest)
        .when()
            .post("/pedidos")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .header("Location", containsString("/pedidos/"))
            .body("id", notNullValue())
            .body("status", equalTo(Status.REALIZADO.name()))
            .body("dataHora", notNullValue())
            .body("itens[0].descricao", equalTo("Prato Feito"))
            .body("itens[0].quantidade", equalTo(1));
    }

    @Test
    @DisplayName("POST /pedidos - Deve retornar 400 Bad Request ao realizar pedido com DTO inválido (itens vazios)")
    void realizaPedido_InvalidDto_EmptyItems() {
        PedidoDto pedidoRequest = new PedidoDto();
        pedidoRequest.setItens(Collections.emptyList()); // Itens vazios

        given()
            .contentType(ContentType.JSON)
            .body(pedidoRequest)
        .when()
            .post("/pedidos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
            // .body("message", containsString("Validation failed")); // Depending on error message
    }

    @Test
    @DisplayName("POST /pedidos - Deve retornar 400 Bad Request ao realizar pedido com item com quantidade zero")
    void realizaPedido_InvalidDto_ZeroQuantity() {
        ItemDoPedidoDto item = new ItemDoPedidoDto(null, 0, "Bebida");
        PedidoDto pedidoRequest = new PedidoDto(null, null, null, List.of(item));

        given()
            .contentType(ContentType.JSON)
            .body(pedidoRequest)
        .when()
            .post("/pedidos")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
            // .body("message", containsString("Validation failed"));
    }

    @Test
    @DisplayName("PUT /pedidos/{id}/status - Deve atualizar o status do pedido com sucesso (200 OK)")
    void atualizaStatus_Success() {
        // Cenário: Criar um pedido e posteriormente atualizar seu status
        PedidoDto pedidoCriado = realizaPedido(createPedidoDto("Lanche", 1));
        Long pedidoId = pedidoCriado.getId();

        StatusDto statusUpdate = new StatusDto(Status.CONFIRMADO);

        given()
            .contentType(ContentType.JSON)
            .body(statusUpdate)
        .when()
            .put("/pedidos/{id}/status", pedidoId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(pedidoId.intValue()))
            .body("status", equalTo(Status.CONFIRMADO.name()));
    }

    @Test
    @DisplayName("PUT /pedidos/{id}/status - Deve retornar 404 Not Found para ID inexistente")
    void atualizaStatus_NotFound() {
        StatusDto statusUpdate = new StatusDto(Status.CONFIRMADO);

        given()
            .contentType(ContentType.JSON)
            .body(statusUpdate)
        .when()
            .put("/pedidos/{id}/status", 9999L)
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("PUT /pedidos/{id}/status - Deve retornar 400 Bad Request para status inválido (nulo)")
    void atualizaStatus_InvalidStatus() {
        PedidoDto pedidoCriado = realizaPedido(createPedidoDto("Lanche", 1));
        Long pedidoId = pedidoCriado.getId();
        StatusDto statusUpdate = new StatusDto(null); // Status nulo

        given()
            .contentType(ContentType.JSON)
            .body(statusUpdate)
        .when()
            .put("/pedidos/{id}/status", pedidoId)
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("PUT /pedidos/{id}/pago - Deve aprovar o pagamento do pedido com sucesso (200 OK)")
    void aprovaPagamento_Success() {
        // Cenário: Criar um pedido e depois aprovar o pagamento
        PedidoDto pedidoCriado = realizaPedido(createPedidoDto("Porção", 1));
        Long pedidoId = pedidoCriado.getId();

        given()
            .contentType(ContentType.JSON)
        .when()
            .put("/pedidos/{id}/pago", pedidoId)
        .then()
            .statusCode(HttpStatus.OK.value());

        // Verificar se o status foi atualizado para PAGO
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/pedidos/{id}", pedidoId)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("status", equalTo(Status.PAGO.name()));
    }

    @Test
    @DisplayName("PUT /pedidos/{id}/pago - Deve retornar 404 Not Found para ID inexistente")
    void aprovaPagamento_NotFound() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .put("/pedidos/{id}/pago", 9999L)
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // Helper method para criar um PedidoDto com valores padrão para testes
    private PedidoDto createPedidoDto(String itemDescricao, Integer quantidade) {
        ItemDoPedidoDto item = new ItemDoPedidoDto(null, quantidade, itemDescricao);
        return new PedidoDto(null, null, null, List.of(item));
    }

    // Helper method para realizar um pedido e retornar o DTO com o ID gerado
    private PedidoDto realizaPedido(PedidoDto dto) {
        return given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/pedidos")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract().as(PedidoDto.class);
    }
}