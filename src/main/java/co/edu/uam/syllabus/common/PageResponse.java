package co.edu.uam.syllabus.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Forma paginada que consume el frontend: { data, pagination }. */
public record PageResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(int page, int limit, long total, int totalPages) {}

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                new Pagination(page.getNumber() + 1, page.getSize(), page.getTotalElements(),
                        Math.max(page.getTotalPages(), 1)));
    }
}
