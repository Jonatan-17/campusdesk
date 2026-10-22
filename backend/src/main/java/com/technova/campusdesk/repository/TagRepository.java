package com.technova.campusdesk.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.technova.campusdesk.entity.Tag;

/** [T1] Acceso a la tabla tags. */
public interface TagRepository extends JpaRepository<Tag, Long> {

	/*
	 * "Consulta JPQL con filtro por lista": trae en UNA consulta todas las etiquetas que ya existen de la lista,
	 * en vez de hacer un findByNombre por cada nombre (problema N+1).
	 * Equivale a la consulta derivada findByNombreIn(Collection<String>); aquí se escribe a mano para practicar JPQL.
	 */
	@Query("SELECT t FROM Tag t WHERE t.nombre IN :nombres")
	List<Tag> buscarPorNombres(@Param("nombres") Collection<String> nombres);

}
