package com.technova.campusdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.technova.campusdesk.entity.QuickResponse;

/**
 * [T5] Acceso a quick_responses. TODAS las búsquedas llevan el técnico (filtro de pertenencia): así es imposible
 * leer o modificar una plantilla ajena aunque se adivine su id.
 */
public interface QuickResponseRepository extends JpaRepository<QuickResponse, Long> {

	List<QuickResponse> findByTecnicoIdOrderByTituloAsc(Long tecnicoId);

	// "WHERE id = ? AND tecnico_id = ?": si la plantilla es de otro técnico, simplemente no se encuentra (404)
	Optional<QuickResponse> findByIdAndTecnicoId(Long id, Long tecnicoId);

	// Unicidad del título por técnico, sin distinguir mayúsculas (al crear)
	boolean existsByTecnicoIdAndTituloIgnoreCase(Long tecnicoId, String titulo);

	// Igual, pero excluyendo la propia plantilla (al editar: mantener el mismo título no es un duplicado)
	boolean existsByTecnicoIdAndTituloIgnoreCaseAndIdNot(Long tecnicoId, String titulo, Long id);

}
