package com.technova.campusdesk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import com.technova.campusdesk.entity.Bodega;
import com.technova.campusdesk.entity.Producto;
import com.technova.campusdesk.repository.BodegaRepository;
import com.technova.campusdesk.repository.ProductoRepository;

/** [P1] Repuestos: POST/GET /api/tickets/{id}/repuestos, transacción "todo o nada" y bloqueo optimista. */
class P1RepuestosApiTest extends EstudioApiTestSupport {

	@Autowired
	private BodegaRepository bodegaRepository;

	@Autowired
	private ProductoRepository productoRepository;

	private Producto cable;  // stock 5
	private Producto mouse;  // stock 1

	@BeforeEach
	void crearInventario() {
		/*
		 * BUG PENDIENTE (no de este ejercicio): src/main/resources/data.sql inserta bodegas y productos con ids
		 * fijos (1, 2, 3) y no avanza el contador IDENTITY. El primer INSERT con id automático recibe id=1 y choca.
		 * Aquí se sincroniza el contador con el id más alto; el arreglo real va en el script de datos
		 * (en PostgreSQL: SELECT setval(pg_get_serial_sequence('bodegas', 'id'), MAX(id)) ...).
		 */
		reiniciarIdentidad("bodegas");
		reiniciarIdentidad("productos");

		Bodega bodega = bodegaRepository.save(Bodega.builder().nombre("Bodega prueba").ubicacion("Piso 1").build());
		cable = productoRepository.save(Producto.builder().nombre("Cable HDMI").stock(5).bodega(bodega).build());
		mouse = productoRepository.save(Producto.builder().nombre("Mouse").stock(1).bodega(bodega).build());
	}

	@Test
	void elTecnicoAsignadoConsumeYSeDescuentaElStock() throws Exception {
		postAs(carla, url(pedroInProgressWithCarla, "/repuestos"), items(cable.getId(), 2))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$[0].cantidad").value(2))
				.andExpect(jsonPath("$[0].stockActual").value(3));

		assertThat(stock(cable)).isEqualTo(3);
		getAs(pedro, url(pedroInProgressWithCarla, "/repuestos")).andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void siUnProductoNoAlcanzaNoSeDescuentaNinguno() throws Exception {
		// El cable alcanza, el mouse no: se deshace también el descuento del cable
		postAs(carla, url(pedroInProgressWithCarla, "/repuestos"), """
				{"items": [{"productoId": %d, "cantidad": 2}, {"productoId": %d, "cantidad": 3}]}"""
				.formatted(cable.getId(), mouse.getId()))
				.andExpect(status().isConflict());

		assertThat(stock(cable)).isEqualTo(5);
		assertThat(stock(mouse)).isEqualTo(1);
		getAs(carla, url(pedroInProgressWithCarla, "/repuestos")).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void elMismoProductoRepetidoSeSumaAntesDeValidar() throws Exception {
		// 3 + 4 = 7 > 5. Validando línea por línea pasaría y el stock quedaría negativo
		postAs(carla, url(pedroInProgressWithCarla, "/repuestos"), """
				{"items": [{"productoId": %d, "cantidad": 3}, {"productoId": %d, "cantidad": 4}]}"""
				.formatted(cable.getId(), cable.getId()))
				.andExpect(status().isConflict());
		assertThat(stock(cable)).isEqualTo(5);
	}

	@Test
	void soloElTecnicoAsignadoYSoloEnInProgress() throws Exception {
		postAs(luis, url(pedroInProgressWithCarla, "/repuestos"), items(cable.getId(), 1)).andExpect(status().isForbidden());
		postAs(luis, url(anaAssignedToLuis, "/repuestos"), items(cable.getId(), 1)).andExpect(status().isConflict());
	}

	@Test
	void cantidadInvalidaOListaVaciaSon400() throws Exception {
		postAs(carla, url(pedroInProgressWithCarla, "/repuestos"), items(cable.getId(), 0)).andExpect(status().isBadRequest());
		postAs(carla, url(pedroInProgressWithCarla, "/repuestos"), "{\"items\": []}").andExpect(status().isBadRequest());
	}

	@Test
	void bloqueoOptimistaDetectaUnaModificacionSimultanea() {
		// Dos "técnicos" leen el mismo producto (versión 0)
		Producto lecturaA = productoRepository.findById(cable.getId()).orElseThrow();
		Producto lecturaB = productoRepository.findById(cable.getId()).orElseThrow();

		// A guarda primero: la versión pasa a 1
		lecturaA.setStock(lecturaA.getStock() - 2);
		productoRepository.save(lecturaA);

		// B guarda con su copia vieja (versión 0): Hibernate lo detecta en vez de pisar el cambio de A
		lecturaB.setStock(lecturaB.getStock() - 4);
		assertThatThrownBy(() -> productoRepository.save(lecturaB))
				.isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(stock(cable)).isEqualTo(3);
	}

	// Sintaxis de H2 (la base de las pruebas)
	private void reiniciarIdentidad(String tabla) {
		Long max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + tabla, Long.class);
		jdbcTemplate.execute("ALTER TABLE " + tabla + " ALTER COLUMN id RESTART WITH " + (max + 1));
	}

	private int stock(Producto producto) {
		return productoRepository.findById(producto.getId()).orElseThrow().getStock();
	}

	private static String items(Long productoId, int cantidad) {
		return "{\"items\": [{\"productoId\": %d, \"cantidad\": %d}]}".formatted(productoId, cantidad);
	}

}
